package com.vitalyilchenko.tripcomputer.services

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.vitalyilchenko.tripcomputer.Models.*
import java.io.InputStream
import java.util.*

/**
 * Created by vitalyilchenko on 2/26/18.
 */
object ObdService {

    private const val responseDelayInMs = 100L
    private const val requestDelayInMs = 100L

    // private val fuelLevelRate: Float = 23.6f
    // (L/100) = fuelLevelRate * deltaFuelLevel / distance
    // (reserve km) = fuelLevelRate * currentFuelLevel / (L/100)

    private var faultedTriesCount: Int = 0
    private val maxFaultTries: Int = 2

    fun getData(ctx: Context): TripData {
        var result = TripData()

        if (!ConnectionManager.isConnected()) {
            var obdAddress = getElmAddress(ctx)
            if (obdAddress.isEmpty()) {
                result.state = ConnectionState.Disconnected
            } else {
                result.state = ConnectionState.Connecting
                if (ConnectionManager.connectToDevice(obdAddress)) {
                    initObd(ConnectionManager.getSocket()!!)
                }
            }

            faultedTriesCount = 0
        } else {
            try {
                result.state = ConnectionState.Connected

                var socket = ConnectionManager.getSocket()!!

                if (socket.isConnected) {
                    result.engineTemperature = getEngineTemperature(socket)
                    result.voltage = getVoltage(socket)

                    var mafValue = getMaf(socket)
                    var currentSpeed = getSpeed(socket)
                    ConsumptionManager.addValues(mafValue, currentSpeed)

                    var currentFuelLevel = getFuelLevel(socket)

                    result.fuelConsumption = ConsumptionManager.getAverageConsumption()
                    result.reserve = ConsumptionManager.getReserveDistance(currentFuelLevel).toString()

                    faultedTriesCount = 0
                }
            } catch (e: Exception) {
                faultedTriesCount++
                Log.e("Trip_ObdService", e.message)
                result.state = ConnectionState.Unknown
                result.engineTemperature = "--"
                result.voltage = "--"
                result.fuelConsumption = "--"
                result.reserve = "--"
            }
        }

        if (faultedTriesCount >= maxFaultTries) {
            try {
                // Let's disconnect and connect again at the next iteration
                ConnectionManager.disconnect()
            } catch (e: Exception) {
                Log.e("Trip_ObdService", e.message)
            }
        }

        val calendar = Calendar.getInstance();
        var minutes = calendar.get(Calendar.MINUTE)
        var seconds = calendar.get(Calendar.SECOND)
        result.timestamp = "${minutes} : ${seconds}"

        return result
    }

    private fun getElmAddress(ctx: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        var obdDeviceAddress = prefs.getString(PreferencesKeys.ObdDeviceAddress, "");
        return obdDeviceAddress;
    }

    private fun initObd(socket: BluetoothSocket): Boolean {
        try {
            var input = socket.inputStream
            var output = socket.outputStream

            // Reset ELM
            output.write("AT Z\r".toByteArray())
            output.flush()
            delay(responseDelayInMs)
            var resetResult = getString(input)
            if (!resetResult.contains("ELM327", true)) {
                throw Exception("Reset command didn't return expected response")
            }
            delay(requestDelayInMs)


            // Echo off
            output.write("AT E0\r".toByteArray())
            output.flush()
            delay(responseDelayInMs)
            var echoResult = getString(input)
            if (!echoResult.contains("OK", true)) {
                throw Exception("Echo Off command didn't return expected response")
            }
            delay(requestDelayInMs)

            // Line off
            output.write("AT L0\r".toByteArray())
            output.flush()
            delay(responseDelayInMs)
            var lineResult = getString(input)
            if (!lineResult.contains("OK",true)) {
                throw Exception("Line Off command didn't return expected response")
            }
            delay(requestDelayInMs)

            // Select protocol
            output.write("AT SP0\r".toByteArray())
            output.flush()
            delay(responseDelayInMs)
            var protocolResult = getString(input)
            if (!protocolResult.contains("OK", true)) {
                throw Exception("Select Protocol command didn't return expected response")
            }
            delay(requestDelayInMs)

            // Check available PIDs
            output.write("09 02\r".toByteArray())
            output.flush()
            delay(responseDelayInMs)
            var availablePidResult = getString(input)
            if (!availablePidResult.contains("SEARCHING...", true)) {
                throw Exception("Check PIDs command didn't return expected response")
            }
            delay(requestDelayInMs)

            return true
        }
        catch (e: Exception) {
            Log.e("Trip_OBD_Service", e.message)
            return false
        }
    }

    private fun getFuelLevel(socket: BluetoothSocket): Int {
        var input = socket.inputStream
        var output = socket.outputStream

        delay(requestDelayInMs)
        output.write("01 2F\r".toByteArray())
        output.flush()
        delay(responseDelayInMs)
        var cmdResult = getString(input)
        if (!cmdResult.contains("41 2F", true)) {
            throw Exception("Fuel level command didn't return expected response")
        }

        // var responseSample = ""
        var splt = cmdResult.split(' ')
        var item = splt[splt.size-2]
        var parsed = Integer.parseInt(item, 16)

        // From 0 to 255
        return parsed
    }

    private fun getVoltage(socket: BluetoothSocket): String {
        var input = socket.inputStream
        var output = socket.outputStream

        delay(requestDelayInMs)
        output.write("AT RV\r".toByteArray())
        output.flush()
        delay(responseDelayInMs)
        var cmdResult = getString(input)
        if (!cmdResult.contains("V", true)) {
            throw Exception("Voltage command didn't return expected response")
        }

        // var responseSample = "12.2V >"
        var splt = cmdResult.split(' ')
        return splt[0].replace("V", "")
    }

    private fun getEngineTemperature(socket: BluetoothSocket): String {
        var input = socket.inputStream
        var output = socket.outputStream

        delay(requestDelayInMs)
        output.write("01 05\r".toByteArray())
        output.flush()
        delay(responseDelayInMs)
        var cmdResult = getString(input)
        if (!cmdResult.contains("41 05")) {
            throw Exception("Eng. temp. command didn't return expected response")
        }

        // var responseSample = "7E8 03 41 05 72 >"
        var splt = cmdResult.split(' ')
        var item = splt[splt.size-2]
        var parsed = Integer.parseInt(item, 16) - 40

        return parsed.toString()
    }

    private fun getMaf(socket: BluetoothSocket): Double {
        var input = socket.inputStream
        var output = socket.outputStream

        delay(requestDelayInMs)
        output.write("01 10\r".toByteArray())
        output.flush()
        delay(responseDelayInMs)
        var cmdResult = getString(input)
        if (!cmdResult.contains("41 10")) {
            throw Exception("MAF command didn't return expected response")
        }

        // var responseSample = ""
        var splt = cmdResult.split(' ')
        var itemA = splt[splt.size-3]
        var itemB = splt[splt.size-2]
        var parsedA = Integer.parseInt(itemA, 16)
        var parsedB = Integer.parseInt(itemB, 16)

        return (256*parsedA + parsedB) / 100.0
    }

    private fun getSpeed(socket: BluetoothSocket): Int {
        var input = socket.inputStream
        var output = socket.outputStream

        delay(requestDelayInMs)
        output.write("01 0D\r".toByteArray())
        output.flush()
        delay(responseDelayInMs)
        var cmdResult = getString(input)
        if (!cmdResult.contains("41 0D", true)) {
            throw Exception("Speed command didn't return expected response")
        }

        // var responseSample = ""
        var splt = cmdResult.split(' ')
        var item = splt[splt.size-2]
        var parsed = Integer.parseInt(item, 16)

        return parsed
    }

    private fun getString(input: InputStream): String {
        var result = StringBuilder()
        var c: Char
        var b = input.read()
        while(b > -1) {
            c = b.toChar()
            if (c == '>')
            {
                break
            }

            result.append(c)
            b = input.read()
        }

        return result.toString()
    }

    private fun delay(duration: Long) {
        try {
            Thread.sleep(duration)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
