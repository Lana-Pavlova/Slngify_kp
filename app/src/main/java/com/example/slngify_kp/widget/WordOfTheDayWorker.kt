package com.example.slngify_kp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.slngify_kp.R
import com.example.slngify_kp.screens.HomePageActivity
import com.example.slngify_kp.screens.WordOfTheDay
import com.example.slngify_kp.screens.fetchRandomWordOfTheDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordOfTheDayWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {


    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val wordOfTheDay = fetchRandomWordOfTheDay()
                if (wordOfTheDay != null) {
                    updateWidgetUI(applicationContext, wordOfTheDay)
                    Result.success()
                } else {
                    Log.e("WordOfTheDayWorker", "No word of the day found")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log.e("WordOfTheDayWorker", "Error updating widget", e)
                Result.failure()
            }
        }
    }

    private fun updateWidgetUI(context: Context, wordOfTheDay: WordOfTheDay) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WordOfTheDayWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)


        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.word_of_the_day_widget)
            views.setTextViewText(R.id.word_text, "✨ ${wordOfTheDay.word} ✨")
            views.setTextViewText(R.id.definition_text, "${wordOfTheDay.definition}\n\n${wordOfTheDay.translation}")

            val intent = Intent(context, HomePageActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context,0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)

        }
    }

}