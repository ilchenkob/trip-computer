package com.vitalyilchenko.tripcomputer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.vitalyilchenko.tripcomputer.services.ConnectionManager
import android.support.v4.content.LocalBroadcastManager
import android.content.IntentFilter
import android.graphics.Typeface
import android.util.Log
import com.vitalyilchenko.tripcomputer.Models.*
import com.vitalyilchenko.tripcomputer.services.ConsumptionManager
import com.vitalyilchenko.tripcomputer.services.ObdService
import com.vitalyilchenko.tripcomputer.services.TripComputerService
import java.lang.Integer.parseInt

class Dashboard : AppCompatActivity() {

    private var obdDeviceAddress: String = ""

    private var txtDeviceName: TextView? = null
    private var txtState: TextView? = null
    private var txtConsumption: TextView? = null
    private var txtVoltage: TextView? = null
    private var txtTemperature: TextView? = null
    private var txtDistance: TextView? = null
    private var connectionButton: Button? = null

    private var tripUpdateReceiver: TripDataUpdateReceiver = TripDataUpdateReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        connectionButton = this.findViewById<Button>(R.id.btnConnect)
        connectionButton?.setOnClickListener { _ -> showConnectionDialog() }

        var resetButton = this.findViewById<Button>(R.id.btnReset)
        resetButton?.setOnClickListener { _ -> ConsumptionManager.reset() }

        txtDeviceName = this.findViewById<TextView>(R.id.txtDeviceName)
        txtState = this.findViewById<TextView>(R.id.txtState)
        txtConsumption = this.findViewById<TextView>(R.id.txtConsumption)
        txtVoltage = this.findViewById<TextView>(R.id.txtVoltage)
        txtTemperature = this.findViewById<TextView>(R.id.txtEngineTemp)
        txtDistance = this.findViewById<TextView>(R.id.txtDistance)

        var customTypeface = Typeface.createFromAsset(applicationContext.assets, "fonts/digital7_mono.ttf")
        txtVoltage?.typeface = customTypeface
        txtTemperature?.typeface = customTypeface
        txtConsumption?.typeface = customTypeface
        txtDistance?.typeface = customTypeface

        tripUpdateReceiver.setOnUpdate { value ->
            txtState?.text = value.state.toString()
            txtVoltage?.text = value.voltage
            txtTemperature?.text = value.engineTemperature
            txtConsumption?.text = value.fuelConsumption
            txtDistance?.text = value.reserve
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        obdDeviceAddress = prefs.getString(PreferencesKeys.ObdDeviceAddress, "");
        if (obdDeviceAddress.isEmpty()) {
            showConnectionDialog()
        } else {
            runTripComputerService()
        }
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter(TripComputerService.ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(tripUpdateReceiver, filter)

        updateConnectionButtonState()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tripUpdateReceiver);
    }

    private fun runTripComputerService() {
        var intent = Intent(applicationContext, TripComputerService::class.java)
        intent.action = TripComputerService.START_SERVICE_ACTION
        startService(intent)
    }

    private fun updateConnectionButtonState() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        obdDeviceAddress = prefs.getString(PreferencesKeys.ObdDeviceAddress, "");
        if (obdDeviceAddress.isEmpty()) {
            connectionButton?.text = "Connect"
            connectionButton?.setOnClickListener { _ -> showConnectionDialog() }
        } else {
            connectionButton?.text = "Disconnect"
            connectionButton?.setOnClickListener { _ -> showDisconnectionDialog() }
        }
    }

    private fun showDisconnectionDialog() {
        var alertBuilder = AlertDialog.Builder(this);
        alertBuilder.setTitle("Are you sure?")
        alertBuilder.setMessage("ELM327 will be paired with your Android, but this application will lose connection with it")
        var dialog = alertBuilder.create()
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "DISCONNECT", { d, _ ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            var editor = prefs.edit()
            editor.putString(PreferencesKeys.ObdDeviceName, "")
            editor.putString(PreferencesKeys.ObdDeviceAddress, "")
            editor.commit()

            ConnectionManager.disconnect()
            updateConnectionButtonState()

            d.dismiss()
        })
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL", { d, _ ->
            updateConnectionButtonState()
            d.dismiss()
        })
        dialog.show()
    }

    private fun showConnectionDialog() {
        var btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null && btAdapter.isEnabled) {
            var selectedDevice: BluetoothDevice? = null;
            var pairedDevices = btAdapter.bondedDevices.toMutableList();
            var pairedDeviceStrings = pairedDevices.map { device -> device.name }.toTypedArray()
            var alertBuilder = AlertDialog.Builder(this);
            alertBuilder.setSingleChoiceItems(pairedDeviceStrings, -1, { _, index: Int ->
                selectedDevice = pairedDevices[index]
            })
            alertBuilder.setTitle("Select ELM327 device")
            var dialog = alertBuilder.create()
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "CONNECT", { d, _ ->
                if (selectedDevice != null) {
                    connectToDevice(selectedDevice!!.name, selectedDevice!!.address)
                }

                updateConnectionButtonState()
                d.dismiss()
            })
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL", { d, _ ->
                updateConnectionButtonState()
                d.dismiss()
            })
            dialog.show()
        }
    }

    private fun connectToDevice(name: String, address: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        var editor = prefs.edit()
        editor.putString(PreferencesKeys.ObdDeviceName, name)
        editor.putString(PreferencesKeys.ObdDeviceAddress, address)
        editor.commit()

        txtDeviceName?.text = name

        runTripComputerService()
    }
}

class TripDataUpdateReceiver : BroadcastReceiver() {

    var onUpdateCallback: (TripData) -> Unit = { }

    fun setOnUpdate(callback: ((TripData) -> Unit)) {
        onUpdateCallback = callback
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        var resultCode = intent?.getIntExtra(IntentParams.ResultCode, Activity.RESULT_CANCELED)
        if (intent != null && resultCode == Activity.RESULT_OK) {
            var result = TripData()
            result.state = ConnectionState.valueOf(intent.getStringExtra(IntentParams.State))
            result.engineTemperature = intent.getStringExtra(IntentParams.EngineTemp)
            result.voltage = intent.getStringExtra(IntentParams.Voltage)
            // result.distanceCovered = intent.getStringExtra(IntentParams.DistanceCovered)
            result.fuelConsumption = intent.getStringExtra(IntentParams.Consumption)
            result.reserve = intent.getStringExtra(IntentParams.Reserve)
            onUpdateCallback.invoke(result)
        }
    }
}
