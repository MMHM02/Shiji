package com.shiji.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.shiji.app.MainActivity

class CaloriesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, com.shiji.app.R.layout.widget_calories_ring)

            // Mock data — real data from Room in production
            val intake = 1250
            val target = 2000
            val progress = (intake * 100 / target).coerceAtMost(100)

            views.setTextViewText(com.shiji.app.R.id.widget_calories, "$intake / $target")
            views.setTextViewText(com.shiji.app.R.id.widget_subtitle, "kcal 已摄入 ($progress%)")
            views.setProgressBar(com.shiji.app.R.id.widget_progress, 100, progress, false)

            // Launch app on tap
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(com.shiji.app.R.id.widget_calories, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
