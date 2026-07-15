package com.pocketpet.core.system.action

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.pocketpet.core.domain.action.AccessibilityActionBridge
import com.pocketpet.core.domain.action.AccessibilityGlobalAction
import com.pocketpet.core.domain.action.PetActionExecutor
import com.pocketpet.core.domain.provider.DispatcherProvider
import com.pocketpet.core.domain.repository.WeatherRepository
import com.pocketpet.core.model.ActionResult
import com.pocketpet.core.model.LaunchAppRequest
import com.pocketpet.core.model.SystemAction
import com.pocketpet.core.system.flashlight.FlashlightController
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class PetActionExecutorImpl(
    private val appContext: Context,
    private val flashlightController: FlashlightController,
    private val accessibilityBridge: AccessibilityActionBridge,
    private val weatherRepository: WeatherRepository,
    private val dispatchers: DispatcherProvider,
) : PetActionExecutor {

    override suspend fun execute(action: SystemAction, launchAppRequest: LaunchAppRequest?): ActionResult =
        withContext(dispatchers.io) {
            when (action) {
                SystemAction.TorchOn -> setTorch(true)
                SystemAction.TorchOff -> setTorch(false)
                SystemAction.OpenCamera -> openViaIntent(
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                    "camera app",
                )
                SystemAction.OpenClock -> openViaIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS), "clock app")
                SystemAction.OpenCalculator -> openViaIntent(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR),
                    "calculator app",
                )
                SystemAction.OpenCalendar -> openViaIntent(
                    Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI),
                    "calendar app",
                )
                SystemAction.OpenContacts -> openViaIntent(
                    Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI),
                    "contacts app",
                )
                SystemAction.OpenPhone -> openViaIntent(Intent(Intent.ACTION_DIAL), "phone app")
                SystemAction.OpenMessages -> openViaIntent(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
                    "messaging app",
                )
                SystemAction.OpenBrightnessSettings ->
                    openViaIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS), "display settings")
                SystemAction.OpenBatterySaverSettings ->
                    openViaIntent(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), "battery saver settings")
                SystemAction.OpenWifiSettings -> openWifiSettings()
                SystemAction.OpenBluetoothSettings ->
                    openViaIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth settings")
                SystemAction.OpenNotificationPanel ->
                    performAccessibilityAction(AccessibilityGlobalAction.OpenNotificationPanel)
                SystemAction.OpenQuickSettings ->
                    performAccessibilityAction(AccessibilityGlobalAction.OpenQuickSettings)
                SystemAction.VolumeUp -> adjustVolume(raise = true)
                SystemAction.VolumeDown -> adjustVolume(raise = false)
                SystemAction.ShowTime -> showTime()
                SystemAction.ShowBattery -> showBattery()
                SystemAction.ShowStorage -> showStorage()
                SystemAction.ShowMemory -> showMemory()
                SystemAction.ShowWeather -> showCachedWeather()
                SystemAction.OpenBatteryOptimizationSettings -> openViaIntent(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                    "battery optimization settings",
                )
                SystemAction.LaunchSelectedApp -> launchApp(launchAppRequest)
            }
        }

    private fun setTorch(enabled: Boolean): ActionResult {
        if (!flashlightController.isTorchAvailable()) {
            return ActionResult.Unsupported("This device doesn't report a flashlight.")
        }
        return if (flashlightController.setTorchEnabled(enabled)) {
            ActionResult.Success(if (enabled) "Torch on." else "Torch off.")
        } else {
            ActionResult.Failed("Couldn't change the torch state.")
        }
    }

    private fun openViaIntent(intent: Intent, humanReadableTarget: String): ActionResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = intent.resolveActivity(appContext.packageManager) != null
        if (!resolved) {
            return ActionResult.NoCompatibleApplication("No $humanReadableTarget is installed.")
        }
        return try {
            appContext.startActivity(intent)
            ActionResult.Success("Opened the $humanReadableTarget.")
        } catch (e: Exception) {
            ActionResult.Failed("Couldn't open the $humanReadableTarget.")
        }
    }

    private fun openWifiSettings(): ActionResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        return openViaIntent(intent, "Wi-Fi settings")
    }

    private fun performAccessibilityAction(action: AccessibilityGlobalAction): ActionResult {
        if (!accessibilityBridge.isServiceConnected()) {
            return ActionResult.RequiresPermission(
                permission = "AccessibilityService",
                rationale = "Turn on Pocket Pet's accessibility feature in Settings to use this.",
            )
        }
        return if (accessibilityBridge.performGlobalAction(action)) {
            ActionResult.Success("Done.")
        } else {
            ActionResult.Failed("Couldn't complete that action.")
        }
    }

    private fun adjustVolume(raise: Boolean): ActionResult {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult.Failed("Audio service is unavailable.")
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
        return ActionResult.Success(if (raise) "Volume up." else "Volume down.")
    }

    private fun showTime(): ActionResult {
        val formatted = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date())
        return ActionResult.Success(formatted)
    }

    private fun showBattery(): ActionResult {
        val sticky = appContext.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val level = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return ActionResult.Failed("Battery status is unavailable right now.")
        val percent = (level * 100) / scale
        return ActionResult.Success("$percent% battery.")
    }

    private fun showStorage(): ActionResult = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = stat.availableBytes / 1_000_000_000.0
        val totalGb = stat.totalBytes / 1_000_000_000.0
        ActionResult.Success(String.format(Locale.getDefault(), "%.1f GB free of %.1f GB.", freeGb, totalGb))
    } catch (e: Exception) {
        ActionResult.Failed("Couldn't read storage information.")
    }

    private fun showMemory(): ActionResult {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return ActionResult.Failed("Memory information is unavailable.")
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val availableGb = info.availMem / 1_000_000_000.0
        val totalGb = info.totalMem / 1_000_000_000.0
        return ActionResult.Success(
            String.format(Locale.getDefault(), "~%.1f GB available of %.1f GB.", availableGb, totalGb),
        )
    }

    private suspend fun showCachedWeather(): ActionResult {
        val cached = weatherRepository.cachedWeather()
            ?: return ActionResult.Unsupported("Check the weather from the Home screen first.")
        return ActionResult.Success("${cached.locationLabel}: ${cached.temperatureCelsius.toInt()}°C")
    }

    private fun launchApp(request: LaunchAppRequest?): ActionResult {
        if (request == null) return ActionResult.Failed("No app was selected.")
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(request.packageName)
            ?: return ActionResult.NoCompatibleApplication("${request.displayName} is no longer installed.")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(launchIntent)
            ActionResult.Success("Opened ${request.displayName}.")
        } catch (e: Exception) {
            ActionResult.Failed("Couldn't open ${request.displayName}.")
        }
    }
}
