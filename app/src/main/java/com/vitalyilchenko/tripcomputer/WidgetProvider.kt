package com.vitalyilchenko.tripcomputer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews


/**
 * Created by vitalyilchenko on 2/25/18.
 */
class WideWidgetProvider : AppWidgetProvider() {

    companion object {
        val ACTION: String = "com.vitaliiilchenko.tripwidget.UPDATE"
        val TAP_ACTION: String = "com.vitaliiilchenko.tripwidget.TAPPED"
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
    }
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        var index = 0
        var count = 0
        if (appWidgetIds != null)
            count = appWidgetIds.size
        while (index < count) {
            val appWidgetId = appWidgetIds?.elementAt(index++)!!

            val intent = Intent(context, Dashboard::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val views = RemoteViews(context?.packageName, R.layout.wide_widget_layout)
            views.setOnClickPendingIntent(R.id.widgetBody, pendingIntent)
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
//        if (intent?.action.equals(ACTION, true)) {
//            var a = intent?.getStringExtra("foo")
//        }
    }
}