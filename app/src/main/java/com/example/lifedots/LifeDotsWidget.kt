package com.example.lifedots

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import com.example.lifedots.graphics.GridDrawer

class LifeDotsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // 1. Create a bitmap to draw on
            val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 2. Use your existing GridDrawer to draw the state
            val drawer = GridDrawer(context)
            drawer.onSurfaceChanged(800, 800) // Initialize metrics
            drawer.draw(canvas)

            // 3. Set the image to the widget
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            // 4. Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Helper to force update all widgets when settings change
        fun forceUpdateAll(context: Context) {
            val intent = android.content.Intent(context, LifeDotsWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, LifeDotsWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}