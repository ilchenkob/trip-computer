package com.vitalyilchenko.tripcomputer.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * Created by vitalyilchenko on 2/25/18.
 */
object ConnectionManager {
    private val serialUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null

    fun isConnected(): Boolean {
        return socket != null && socket!!.isConnected
    }

    fun getSocket(): BluetoothSocket? {
        return socket
    }

    fun disconnect() {
        try {
            if (isConnected()) {
                socket?.close()
                socket = null
            }
        } catch (e: Exception) {
            Log.e("Trip_BluetoothManager", e.message)
        }
    }

    fun connectToDevice(address: String): Boolean {
        var btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null && btAdapter.isEnabled) {
            var pairedDevices = btAdapter.bondedDevices;
            var device = pairedDevices.find{ device -> device.address.equals(address, true) }
            if (device == null)
                return false

            try {
                var connectedSocket = device.createRfcommSocketToServiceRecord(serialUuid);
                connectedSocket.connect()

                if (connectedSocket.isConnected)
                    socket = connectedSocket
                else
                    socket = null

                return connectedSocket.isConnected;
            }
            catch (e: IOException) {
                Log.e("Trip+BluetoothManager", e.message)
            }
        }

        return false
    }
}