package com.vitalyilchenko.tripcomputer

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.content.WakefulBroadcastReceiver
import com.vitalyilchenko.tripcomputer.services.TripComputerService


/**
 * Created by vitalyilchenko on 2/25/18.
 */
class TripAlarmReceiver : BroadcastReceiver() {

    // Triggered by the Alarm periodically (starts the service to run task)
    override fun onReceive(context: Context, intent: Intent) {
        val i = Intent(context, TripComputerService::class.java)
        context.startService(i)
    }

    companion object {
        val REQUEST_CODE = 901188
        val ACTION = "com.vitaliiilchenko.tripcomputer.alarm"
    }
}

// WakefulBroadcastReceiver ensures the device does not go back to sleep
// during the startup of the service
class BootBroadcastReceiver : WakefulBroadcastReceiver() {

    companion object {
        val IntentExtraName: String = "bootload"
        var Action: String = "BOOT_ACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Launch the specified service when this message is received
        val startServiceIntent = Intent(context, TripComputerService::class.java)
        startServiceIntent.action = Action
        startServiceIntent.putExtra(IntentExtraName, true);
        startWakefulService(context, startServiceIntent)
    }
}