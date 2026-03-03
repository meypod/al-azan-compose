package com.github.meypod.al_azan.core.util.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

// taken from notifee with edits

object IntentUtils {
    private const val TAG = "IntentUtils"

    @SuppressLint("QueryPermissionsNeeded")
    fun isAvailableOnDevice(
        ctx: Context?,
        intent: Intent?,
    ): Boolean {
        try {
            if (ctx == null || intent == null) {
                return false
            }

            val mgr = ctx.packageManager
            val list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return list.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred whilst trying to check if intent is available on device", e)

            return false
        }
    }

    fun getActivityName(intent: Intent?): String? {
        if (intent == null) {
            return null
        }
        val className = intent.component?.className ?: return null
        val index = className.lastIndexOf(".")
        if (index != -1) {
            return className.substring(index + 1)
        }
        return null
    }

    fun startActivityOnUiThread(
        activity: Activity?,
        intent: Intent?,
    ) {
        if (activity == null || intent == null) {
            Log.w(TAG, "Activity or intent is null when calling startActivityOnUiThread()")
            return
        }

        val ctx: Context? = activity.applicationContext
        if (ctx == null) {
            Log.w(TAG, "Unable to get application context when calling startActivityOnUiThread()")
        }

        activity.runOnUiThread(
            Runnable {
                try {
                    ctx!!.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "An error occurred whilst trying to start activity on Ui Thread", e)
                }
            },
        )
    }

    fun getLaunchActivity(
        applicationContext: Context,
        launchActivity: String?,
    ): Class<*>? {
        val activity: String?

        if (launchActivity != null && launchActivity != "default") {
            activity = launchActivity
        } else {
            activity = getMainActivityClassName(applicationContext)
        }

        if (activity == null) {
            Log.e("ReceiverService", "Launch Activity for notification could not be found.")
            return null
        }

        val launchActivityClass = getClassForName(activity)

        if (launchActivityClass == null) {
            Log.e(
                "ReceiverService",
                String.format("Launch Activity for notification does not exist ('%s').", launchActivity),
            )
            return null
        }

        return launchActivityClass
    }

    private fun getClassForName(className: String): Class<*>? =
        try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            null
        }

    private fun getMainActivityClassName(applicationContext: Context): String? {
        val launchIntent: Intent? =
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)

        return launchIntent?.component?.className
    }
}
