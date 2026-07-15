package com.pocketpet.core.system.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.pocketpet.core.domain.repository.SystemStatusRepository
import com.pocketpet.core.model.BatteryStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reads battery/charging state from Android's `ACTION_BATTERY_CHANGED` sticky broadcast. This is
 * push-based, not polled: `registerReceiver` immediately delivers the current sticky value, and
 * every subsequent broadcast pushes a new one — there is no timer anywhere in this class.
 */
class AndroidBatteryMonitor(private val appContext: Context) : SystemStatusRepository {

    override val batteryStatus: Flow<BatteryStatus> = callbackFlow {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.toBatteryStatus())
            }
        }
        val sticky = ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { trySend(it.toBatteryStatus()) }

        awaitClose { appContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun Intent.toBatteryStatus(): BatteryStatus {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 50
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL ||
            plugged != 0
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || percent >= 100
        return BatteryStatus(
            percent = percent.coerceIn(0, 100),
            isCharging = isCharging,
            isFull = isFull,
        )
    }
}
