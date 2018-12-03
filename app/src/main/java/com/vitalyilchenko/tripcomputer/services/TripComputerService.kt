package com.vitalyilchenko.tripcomputer.services

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.support.v4.content.WakefulBroadcastReceiver
import java.util.*
import android.widget.RemoteViews
import com.vitalyilchenko.tripcomputer.*
import android.util.Log
import android.util.TypedValue
import com.vitalyilchenko.tripcomputer.Models.IntentParams
import com.vitalyilchenko.tripcomputer.Models.TripData

/**
 * Created by vitalyilchenko on 2/25/18.
 */

// Must create a default constructor
class TripComputerService : IntentService("TripComputerService") {

    private var pIntent: PendingIntent? = null
    private var alarmManager: AlarmManager? = null

    companion object {
        @JvmStatic val ACTION = "com.vitaliiilchenko.tripcomputer.obdupdate"
        const val START_SERVICE_ACTION = "com.vitaliiilchenko.tripcomputer.startservice"

        const val TriggerIntervalSec = 6L
    }

    fun startService() {
        stopService()

        val intent = Intent(applicationContext, TripAlarmReceiver::class.java)
        pIntent = PendingIntent.getBroadcast(applicationContext, TripAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager?.setRepeating(AlarmManager.RTC_WAKEUP,
                SystemClock.elapsedRealtime() + 5000,
                TriggerIntervalSec * 1000, pIntent)
    }

    fun stopService() {
        if (pIntent != null) {
            try {
                alarmManager?.cancel(pIntent)
                pIntent = null
            } catch (ignored: Exception) { Log.e("TripComputerService", ignored.message)}
        }
    }

    override fun onCreate() {
        super.onCreate() // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
    }

    override fun onHandleIntent(intent: Intent?) {

        if (intent != null) {
            if (intent.action == BootBroadcastReceiver.Action &&
                intent.hasExtra(BootBroadcastReceiver.IntentExtraName) &&
                intent.getBooleanExtra(BootBroadcastReceiver.IntentExtraName, false)) {
                // Release the wake lock provided by the WakefulBroadcastReceiver.
                WakefulBroadcastReceiver.completeWakefulIntent(intent)

                startService()
            } else if (intent.action == START_SERVICE_ACTION) {
                startService()
            }
        }

        var tripData = ObdService.getData(this.applicationContext)

        val obdUpdateIntent = Intent(ACTION)
        obdUpdateIntent.putExtra(IntentParams.ResultCode, Activity.RESULT_OK)
        obdUpdateIntent.putExtra(IntentParams.EngineTemp, tripData.engineTemperature)
        obdUpdateIntent.putExtra(IntentParams.Consumption, tripData.fuelConsumption)
        obdUpdateIntent.putExtra(IntentParams.Reserve, tripData.reserve)
        obdUpdateIntent.putExtra(IntentParams.State, tripData.state.toString())
        obdUpdateIntent.putExtra(IntentParams.DistanceCovered, "N/A")
        obdUpdateIntent.putExtra(IntentParams.Voltage, tripData.voltage)

        LocalBroadcastManager.getInstance(this).sendBroadcast(obdUpdateIntent)

        // Update widget
        val remoteViews = RemoteViews(this.applicationContext.packageName, R.layout.wide_widget_layout)
        // remoteViews.setTextViewText(R.id.txtValue, "--")
        remoteViews.setImageViewBitmap(R.id.ivVoltage, convertVoltToImg(tripData.voltage, this.applicationContext))
        remoteViews.setImageViewBitmap(R.id.ivTemperature, convertToImg(tripData.engineTemperature, this.applicationContext))
        remoteViews.setImageViewBitmap(R.id.ivConsumption, convertToImg(tripData.fuelConsumption, this.applicationContext))
        remoteViews.setImageViewBitmap(R.id.ivDistance, convertToImg(tripData.reserve, this.applicationContext))

        val thiswidget = ComponentName(this.applicationContext, WideWidgetProvider::class.java)
        val manager = AppWidgetManager.getInstance(this.applicationContext)
        manager.updateAppWidget(thiswidget, remoteViews)
    }

    private val imageWidth = 680
    private val imageHeight = 640
    private val textX = 340f
    private val textY = 462f
    private val textSize = 380f
    private val fontFilename = "fonts/digital7_mono.ttf"
    private val textColor = Color.parseColor("#FCFCFC")

    private fun convertToImg(text: String, context: Context): Bitmap {
        val btmText = createBitmap()
        val cnvText = Canvas(btmText)
        val paint = createTextPaint(context)

        cnvText.drawText(text, textX, textY, paint)
        return btmText
    }

    private fun convertVoltToImg(text: String, context: Context): Bitmap {
        val btmText = createBitmap()
        val cnvText = Canvas(btmText)
        val paint = createTextPaint(context)

        cnvText.drawText(text, textX - 28f, textY, paint)
        return btmText
    }

    private fun createBitmap(): Bitmap {
        return Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
    }

    private fun createTextPaint(context: Context): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isSubpixelText = true
        paint.typeface = Typeface.createFromAsset(context.assets, fontFilename)
        paint.color = textColor
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        return paint
    }
}