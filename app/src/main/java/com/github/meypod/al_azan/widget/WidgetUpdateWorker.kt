package com.github.meypod.al_azan.widget

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        try {
            widgetUpdater.update()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Widget update failed", e)
            Result.retry()
        }

    private companion object {
        const val TAG = "WidgetUpdateWorker"
    }
}
