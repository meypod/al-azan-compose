package com.github.meypod.al_azan.core.util.device

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.util.android.IntentUtils
import kotlin.concurrent.Volatile

// taken from notifee project with edits and conversion to kotlin

object PowerManagerUtils {
    private const val TAG = "PowerManagerUtils"

    @Volatile
    private var sPowerManagerIntentCache: Intent? = null

    fun setPowerManagerIntentCache(intent: Intent?) {
        synchronized(PowerManagerUtils::class.java) {
            sPowerManagerIntentCache = intent
        }
    }

    val powerManagerIntent: Intent?
        get() {
            synchronized(PowerManagerUtils::class.java) {
                return sPowerManagerIntentCache
            }
        }

    /**
     * Attempts to open the device's battery optimization settings
     * Requires at least Build.VERSION_CODES.M
     *
     * @param activity
     */
    fun openBatteryOptimizationSettings(activity: Activity?) {
        try {
            val intent = Intent()
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (activity != null) {
                val isAvailableOnDevice: Boolean =
                    IntentUtils.isAvailableOnDevice(activity.applicationContext, intent)

                if (!isAvailableOnDevice) {
                    Log.d(TAG, "battery optimization settings is not available on device")
                    return
                }

                IntentUtils.startActivityOnUiThread(activity, intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred whilst trying to open battery optimization settings", e)
        }
    }

    /**
     * Returns true if the app has battery optimization enabled
     *
     * @param context
     */
    fun isBatteryOptimizationEnabled(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Lights up the screen if it is not already lit
     *
     * @param context
     */
    fun lightUpScreenIfNeeded(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isInteractive = pm.isInteractive()

        if (!isInteractive) {
            val wl =
                pm.newWakeLock(
                    (
                        PowerManager.FULL_WAKE_LOCK
                            or PowerManager.ACQUIRE_CAUSES_WAKEUP
                            or PowerManager.ON_AFTER_RELEASE
                        ),
                    "AlAzan:lock",
                )
            wl.acquire(10 * 60 * 1000L) // 10 minutes

            val wlCpu =
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlAzan:cpuLock")
            wlCpu.acquire(10 * 60 * 1000L) // 10 minutes
        }
    }

    /**
     * Retrieves information about the device and its Power Manager Settings
     *
     * @return PowerManagerInfo
     */
    fun getPowerManagerInfo(context: Context): PowerManagerUtils.PowerManagerInfo {
        val activityName: String?

        val intent = findPowerManagerIntent(context)
        activityName = IntentUtils.getActivityName(intent)

        val result =
            PowerManagerInfo(Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, activityName)
        return result
    }

    /**
     * Attempts to open the device's Power Manager settings
     *
     * @param activity
     */
    fun openPowerManagerSettings(activity: Activity?) {
        var intent: Intent? = powerManagerIntent
        if (intent == null) {
            intent = findPowerManagerIntent(activity?.applicationContext)
        }

        if (intent != null) {
            try {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                IntentUtils.startActivityOnUiThread(activity, intent)
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Unable to start activity: " + IntentUtils.getActivityName(intent),
                    exception,
                )
            }
        } else {
            Log.w(TAG, "Unable to find an activity to open the device's power manager")
        }
    }

    private fun findPowerManagerIntent(context: Context?): Intent? {
        val manufacturerName = Build.BRAND.lowercase()
        val possibleIntents = getManufacturerPowerManagerIntents(manufacturerName)

        for (i in possibleIntents.indices) {
            val possibleIntent = possibleIntents[i]
            val isAvailableOnDevice: Boolean = IntentUtils.isAvailableOnDevice(context, possibleIntent)
            if (isAvailableOnDevice) {
                setPowerManagerIntentCache(possibleIntent)
                return possibleIntent
            }
        }
        return null
    }

    @SuppressLint("BatteryLife")
    private fun getManufacturerPowerManagerIntents(manufacturerName: String): List<Intent?> =
        when (manufacturerName) {
            "asus" ->
                listOf<Intent?>(
                    createIntent(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.powersaver.PowerSaverSettings",
                    ),
                    createIntent(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.autostart.AutoStartActivity",
                    ),
                    createIntent(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.entry.FunctionActivity",
                    )
                        .setData("mobilemanager://function/entry/AutoStart".toUri()),
                )

            "samsung" ->
                listOf<Intent?>(
                    createIntent(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity",
                    ),
                    createIntent(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity",
                    ),
                    createIntent(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity",
                    ),
                )

            "huawei" ->
                listOf<Intent?>(
                    createIntent(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity",
                    ),
                    createIntent(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                    ),
                    createIntent(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
                    ),
                )

            "redmi", "xiaomi" ->
                listOf<Intent?>(
                    createIntent(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity",
                    ),
                )

            "letv" ->
                listOf<Intent?>(
                    createIntent(
                        "com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity",
                    )
                        .setData("mobilemanager://function/entry/AutoStart".toUri()),
                )

            "honor" ->
                listOf<Intent?>(
                    createIntent(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity",
                    ),
                )

            "coloros", "oppo" ->
                listOf<Intent?>(
                    createIntent(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                    ),
                    createIntent(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity",
                    ),
                    createIntent(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity",
                    )
                        .setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS),
                    createIntent(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
                    ),
                    createIntent(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerSaverModeActivity",
                    ),
                    createIntent(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity",
                    ),
                )

            "vivo" ->
                listOf<Intent?>(
                    createIntent(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
                    ),
                    createIntent(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                    ),
                    createIntent(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
                    ),
                )

            "nokia" ->
                listOf<Intent?>(
                    createIntent(
                        "com.evenwell.powersaving.g3",
                        "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity",
                    ),
                )

            "oneplus" ->
                listOf<Intent?>(
                    createIntent(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
                    ),
                )

            "meizu" ->
                listOf<Intent?>(
                    createIntent("com.meizu.safe", "com.meizu.safe.security.SHOW_APPSEC")
                        .addCategory(Intent.CATEGORY_DEFAULT),
                )

            "htc" ->
                listOf<Intent?>(
                    createIntent(
                        "com.htc.pitroad",
                        "com.htc.pitroad.landingpage.activity.LandingPageActivity",
                    ),
                )

            else -> emptyList()
        }

    private fun createIntent(
        pkg: String,
        cls: String,
    ): Intent {
        val intent = Intent()
        intent.setComponent(ComponentName(pkg, cls))
        return intent
    }

    @Immutable
    data class PowerManagerInfo(
        val manufacturer: String?,
        val model: String?,
        val version: String?,
        val activity: String?,
    )
}
