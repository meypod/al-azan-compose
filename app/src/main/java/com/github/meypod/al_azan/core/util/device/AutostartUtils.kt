package com.github.meypod.al_azan.core.util.device

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.util.android.IntentUtils
import kotlin.concurrent.Volatile

/**
 * Some OEM skins (MIUI, ColorOS, Funtouch, EMUI, HiOS, …) keep their own "auto-start" /
 * "auto-launch" allow-list that is separate from the system battery-optimization list. An app that
 * is not on it is prevented from being started by the system (boot, alarms, broadcasts), so adhan
 * alarms never fire after the app is swiped away or the phone reboots.
 *
 * Component names collected from the notifee project, AutoStarter and dontkillmyapp. The activities
 * are vendor-unique, so we probe the whole list and open the first one that resolves on the device
 * instead of keying on [android.os.Build.BRAND] (which POCO/Redmi/iQOO/sub-brands report
 * inconsistently).
 */
object AutostartUtils {
    private const val TAG = "AutostartUtils"

    @Volatile
    private var cache: Intent? = null

    private val AUTOSTART_INTENTS: List<Intent> =
        listOf(
            // Xiaomi / Redmi / POCO (MIUI)
            createIntent(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
            // LeEco / LeTV
            createIntent(
                "com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity",
            ),
            // Huawei / Honor (EMUI / Magic UI)
            createIntent(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
            createIntent(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
            ),
            createIntent(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity",
            ),
            // Oppo / Realme (ColorOS)
            createIntent(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            ),
            createIntent(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity",
            ),
            createIntent(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity",
            ),
            // Vivo / iQOO (Funtouch / OriginOS)
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
            // OnePlus (OxygenOS)
            createIntent(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
            ),
            // Asus (ZenUI)
            createIntent(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.autostart.AutoStartActivity",
            ),
            createIntent(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.entry.FunctionActivity",
            )
                .setData("mobilemanager://function/entry/AutoStart".toUri()),
            // Transsion: Infinix (XOS) / Tecno (HiOS) / itel
            createIntent(
                "com.transsion.phonemanager",
                "com.itel.autobootmanager.activity.AutoBootMgrActivity",
            ),
        )

    /**
     * Finds the first auto-start allow-list activity that resolves on this device, or null when the
     * OEM has no such screen (e.g. stock Android, Samsung).
     */
    fun findAutostartIntent(context: Context?): Intent? {
        cache?.let { return it }
        for (intent in AUTOSTART_INTENTS) {
            if (IntentUtils.isAvailableOnDevice(context, intent)) {
                cache = intent
                return intent
            }
        }
        return null
    }

    fun hasAutostartSettings(context: Context): Boolean = findAutostartIntent(context) != null

    fun openAutostartSettings(activity: Activity?) {
        val intent = findAutostartIntent(activity?.applicationContext)
        if (intent == null) {
            Log.w(TAG, "No auto-start settings activity available on this device")
            return
        }
        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            IntentUtils.startActivityOnUiThread(activity, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open auto-start settings: " + IntentUtils.getActivityName(intent), e)
        }
    }

    private fun createIntent(
        pkg: String,
        cls: String,
    ): Intent = Intent().setComponent(ComponentName(pkg, cls))
}
