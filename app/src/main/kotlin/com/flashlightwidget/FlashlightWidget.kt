package com.flashlightwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class FlashlightWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, views(context))
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FlashlightWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context))
        }

        private fun views(context: Context): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
                setImageViewResource(
                    R.id.widget_icon,
                    if (FlashlightService.isOn) R.drawable.widget_on else R.drawable.widget_off,
                )
                setOnClickPendingIntent(
                    R.id.widget_root,
                    FlashlightService.pendingIntent(context, FlashlightService.ACTION_TOGGLE),
                )
            }
    }
}
