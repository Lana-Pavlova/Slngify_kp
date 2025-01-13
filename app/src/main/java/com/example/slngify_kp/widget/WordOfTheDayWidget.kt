package com.example.slngify_kp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import com.example.slngify_kp.R
import com.example.slngify_kp.screens.HomePageActivity
import com.example.slngify_kp.screens.fetchRandomWordOfTheDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WordOfTheDayWidget : AppWidgetProvider() {
    private val workRequest = OneTimeWorkRequestBuilder<WordOfTheDayWorker>()
        .setInitialDelay(1, TimeUnit.HOURS)
        .build()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }


    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.word_of_the_day_widget)

        val sharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            val wordOfTheDay = fetchRandomWordOfTheDay()
            if (wordOfTheDay != null) {
                views.setTextViewText(R.id.word_text, "✨ ${wordOfTheDay.word} ✨")
                views.setTextViewText(R.id.definition_text, "${wordOfTheDay.definition}\n\n${wordOfTheDay.translation}")

                val intent = Intent(context, HomePageActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context,0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

    }

    companion object {
        fun updateWidget(context: Context) {
            val widgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WordOfTheDayWidget::class.java)
            val widgetIds = widgetManager.getAppWidgetIds(componentName)

            val intent = Intent(context, WordOfTheDayWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)
        }

    }

}