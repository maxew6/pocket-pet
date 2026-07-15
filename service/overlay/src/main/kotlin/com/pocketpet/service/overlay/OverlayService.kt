package com.pocketpet.service.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.service.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pocketpet.core.domain.action.InstalledAppResolver
import com.pocketpet.core.domain.action.PetActionExecutor
import com.pocketpet.core.domain.behavior.UserInteractionType
import com.pocketpet.core.domain.repository.NotificationEventSource
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.domain.repository.PetRepository
import com.pocketpet.core.domain.repository.SystemStatusRepository
import com.pocketpet.core.domain.usecase.ProcessBehaviorTickUseCase
import com.pocketpet.core.domain.usecase.RestorePetStateUseCase
import com.pocketpet.core.model.ActionResult
import com.pocketpet.core.model.PetPosition
import com.pocketpet.core.model.PetPreferences
import com.pocketpet.core.model.PetState
import com.pocketpet.core.model.ScreenBounds
import com.pocketpet.core.model.SystemAction
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import com.pocketpet.service.overlay.R

@AndroidEntryPoint
open class HiltService : android.app.Service() {
    override fun onBind(intent: Intent?) = null
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OverlayEntryPoint {
    fun processBehaviorTick(): ProcessBehaviorTickUseCase
    fun restorePetState(): RestorePetStateUseCase
    fun petActionExecutor(): PetActionExecutor
    fun petRepository(): PetRepository
    fun preferencesRepository(): PetPreferencesRepository
    fun notificationEventSource(): NotificationEventSource
    fun systemStatusRepository(): SystemStatusRepository
    fun installedAppResolver(): InstalledAppResolver
}

/**
 * Hosts the pet as a small `TYPE_APPLICATION_OVERLAY` window. Extends [LifecycleService] (which
 * gives a correctly-dispatched [Lifecycle] for free) and additionally implements
 * [ViewModelStoreOwner]/[SavedStateRegistryOwner] by hand, since a bare `Service` isn't one of
 * those on its own — the trio is what a [ComposeView] needs to host content outside an Activity.
 */
class OverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var processBehaviorTick: ProcessBehaviorTickUseCase
    private lateinit var restorePetState: RestorePetStateUseCase
    private lateinit var petActionExecutor: PetActionExecutor
    private lateinit var petRepository: PetRepository
    private lateinit var preferencesRepository: PetPreferencesRepository
    private lateinit var notificationEventSource: NotificationEventSource
    private lateinit var systemStatusRepository: SystemStatusRepository
    private lateinit var installedAppResolver: InstalledAppResolver

    private lateinit var windowManager: WindowManager
    private lateinit var screenBoundsProvider: ScreenBoundsProvider
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var petSizePx: Int = 0

    private val feedingPulse = mutableStateOf(false)
    private val chargingCelebration = mutableStateOf(false)
    private val isScreenOn = mutableStateOf(true)
    private var isHiddenTemporarily = false
    private var isDraggingWindow = false
    @Volatile private var cachedPreferences = PetPreferences()

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            isScreenOn.value = intent.action == Intent.ACTION_SCREEN_ON
        }
    }

    override fun onCreate() {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, OverlayEntryPoint::class.java)
        processBehaviorTick = entryPoint.processBehaviorTick()
        restorePetState = entryPoint.restorePetState()
        petActionExecutor = entryPoint.petActionExecutor()
        petRepository = entryPoint.petRepository()
        preferencesRepository = entryPoint.preferencesRepository()
        notificationEventSource = entryPoint.notificationEventSource()
        systemStatusRepository = entryPoint.systemStatusRepository()
        installedAppResolver = entryPoint.installedAppResolver()

        savedStateRegistryController.performRestore(null)
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenBoundsProvider = ScreenBoundsProvider(this)
        petSizePx = (88 * resources.displayMetrics.density).roundToInt()
        isRunning = true

        promoteToForeground()
        registerReceiver(
            screenStateReceiver,
            android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )

        lifecycleScope.launch {
            restorePetState() // Awaited first: everything below reads/reacts to restored state.
            addOverlayView()
            observePreferences()
            observeNotificationEvents()
            observeBatteryChanges()
            startAmbientTicker()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        runCatching { unregisterReceiver(screenStateReceiver) }
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        viewModelStore.clear()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------
    // Foreground notification
    // ---------------------------------------------------------------------------------------

    private fun promoteToForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.overlay_notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_notification_pet)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    // ---------------------------------------------------------------------------------------
    // Overlay window + Compose hosting
    // ---------------------------------------------------------------------------------------

    private fun addOverlayView() {
        val density = resources.displayMetrics.density
        val bounds = screenBoundsProvider.current(density)
        val startPosition = petRepository.snapshot.value.position.takeIf { it.xDp > 0f || it.yDp > 0f }
            ?: PetPosition(xDp = bounds.safeLeft + 24f, yDp = bounds.safeTop + 96f)
        val clamped = bounds.clamp(startPosition, petSizePx / density)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (clamped.xDp * density).roundToInt()
            y = (clamped.yDp * density).roundToInt()
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent { OverlayScreen() }
        }
        overlayView = view
        windowManager.addView(view, params)
    }

    @androidx.compose.runtime.Composable
    private fun OverlayScreen() {
        val snapshotState = petRepository.snapshot.collectAsState()
        val preferencesState = preferencesRepository.preferences.collectAsState(
            initial = PetPreferences(),
        )

        OverlayRoot(
            snapshot = snapshotState.value,
            preferences = preferencesState.value,
            feedingPulse = feedingPulse.value,
            chargingCelebration = chargingCelebration.value,
            isScreenOn = isScreenOn.value,
            actions = OverlayActions(
                onTap = { tick(UserInteractionType.Tap) },
                onDoubleTap = { tick(UserInteractionType.DoubleTap) },
                onLongPress = { tick(UserInteractionType.LongPress) },
                onDragStart = { isDraggingWindow = true; tick(UserInteractionType.DragStart) },
                onDrag = ::onWindowDragged,
                onDragEnd = ::onWindowDragEnded,
                onUpwardFling = { tick(UserInteractionType.UpwardFling) },
                onHorizontalFling = ::onHorizontalFling,
                onFeed = { tick(UserInteractionType.Feed) },
                onPlay = { tick(UserInteractionType.Play) },
                onToggleSleep = { tick(UserInteractionType.ToggleSleep) },
                onHideTemporarily = ::hideTemporarily,
                onTogglePositionLock = ::togglePositionLock,
                onOpenApp = ::openSelectedApp,
            ),
        )
    }

    // ---------------------------------------------------------------------------------------
    // Behavior ticking
    // ---------------------------------------------------------------------------------------

    private fun currentScreenBounds(): ScreenBounds = screenBoundsProvider.current(resources.displayMetrics.density)

    private fun tick(interaction: UserInteractionType? = null) {
        lifecycleScope.launch {
            val decision = processBehaviorTick(currentScreenBounds(), pendingInteraction = interaction)
            if (decision.feedingAccepted) {
                feedingPulse.value = true
                delay(1_000)
                feedingPulse.value = false
            }
            if (decision.justReachedFullCharge) {
                chargingCelebration.value = true
                delay(1_800)
                chargingCelebration.value = false
            }
        }
    }

    /** Low-frequency ambient decisions while the overlay is visible — event-driven, not a busy loop. */
    private fun startAmbientTicker() {
        lifecycleScope.launch {
            while (true) {
                delay(AMBIENT_TICK_INTERVAL_MILLIS)
                if (!isHiddenTemporarily && !isDraggingWindow) tick()
            }
        }
    }

    private fun observeNotificationEvents() {
        notificationEventSource.events
            .onEach { event -> processBehaviorTick(currentScreenBounds(), pendingNotification = event) }
            .launchIn(lifecycleScope)
    }

    private fun observeBatteryChanges() {
        systemStatusRepository.batteryStatus
            .distinctUntilChangedBy { it.percent to it.isCharging }
            .onEach { tick() }
            .launchIn(lifecycleScope)
    }

    /** Keeps the overlay window's visibility synced with "hidden temporarily" and the master
     *  overlay-enabled preference, and mirrors preferences into [cachedPreferences] so gesture
     *  callbacks (not suspend functions) can check things like `positionLocked` synchronously. */
    private fun observePreferences() {
        preferencesRepository.preferences
            .onEach { prefs ->
                cachedPreferences = prefs
                overlayView?.visibility = if (isHiddenTemporarily || !prefs.overlayEnabled) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            }
            .launchIn(lifecycleScope)
    }

    // ---------------------------------------------------------------------------------------
    // Drag / fling physics
    // ---------------------------------------------------------------------------------------

    private fun onWindowDragged(delta: androidx.compose.ui.geometry.Offset) {
        if (cachedPreferences.positionLocked) return
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density
        val bounds = currentScreenBounds()
        val newXDp = (params.x + delta.x) / density
        val newYDp = (params.y + delta.y) / density
        val clamped = bounds.clamp(PetPosition(newXDp, newYDp), petSizePx / density)
        params.x = (clamped.xDp * density).roundToInt()
        params.y = (clamped.yDp * density).roundToInt()
        runCatching { windowManager.updateViewLayout(overlayView, params) }
        lifecycleScope.launch { petRepository.updatePosition(clamped) }
    }

    private fun onWindowDragEnded() {
        isDraggingWindow = false
        tick(UserInteractionType.DragEnd)
        lifecycleScope.launch {
            petRepository.persistNow()
            if (preferencesRepository.current().edgeSnappingEnabled) snapToNearestEdge()
        }
    }

    private fun onHorizontalFling(velocityXPxPerSecond: Float) {
        tick(UserInteractionType.HorizontalFling)
        if (cachedPreferences.positionLocked) return
        lifecycleScope.launch { animateThrow(velocityXPxPerSecond) }
    }

    private suspend fun animateThrow(initialVelocityXPxPerSecond: Float) {
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density
        var velocity = initialVelocityXPxPerSecond
        var x = params.x.toFloat()
        val bounds = currentScreenBounds()
        val petSizeDp = petSizePx / density

        while (abs(velocity) > 100f) {
            x += (velocity * 0.016f)
            val clamped = bounds.clamp(PetPosition(x / density, params.y.toFloat() / density), petSizeDp)
            params.x = (clamped.xDp * density).roundToInt()
            runCatching { windowManager.updateViewLayout(overlayView, params) }

            if (clamped.xDp <= bounds.safeLeft || clamped.xDp >= bounds.safeRight - petSizeDp) {
                velocity = 0f
            } else {
                velocity *= 0.95f
            }
            delay(16)
        }
        petRepository.updatePosition(PetPosition(params.x / density, params.y / density))
        petRepository.persistNow()
    }

    private suspend fun snapToNearestEdge() {
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density
        val bounds = currentScreenBounds()
        val petSizeDp = petSizePx / density
        val currentX = params.x / density
        val targetX = if (currentX + petSizeDp / 2 < (bounds.safeLeft + bounds.safeRight) / 2) {
            bounds.safeLeft
        } else {
            bounds.safeRight - petSizeDp
        }

        val startX = currentX
        val duration = 250L
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < duration) {
            val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
            val interpolatedX = startX + (targetX - startX) * progress
            params.x = (interpolatedX * density).roundToInt()
            runCatching { windowManager.updateViewLayout(overlayView, params) }
            delay(16)
        }
        params.x = (targetX * density).roundToInt()
        runCatching { windowManager.updateViewLayout(overlayView, params) }
        petRepository.updatePosition(PetPosition(targetX, params.y / density))
        petRepository.persistNow()
    }

    // ---------------------------------------------------------------------------------------
    // Quick menu actions
    // ---------------------------------------------------------------------------------------

    private fun hideTemporarily() {
        isHiddenTemporarily = true
        overlayView?.visibility = android.view.View.GONE
        lifecycleScope.launch {
            delay(5_000)
            isHiddenTemporarily = false
            if (preferencesRepository.current().overlayEnabled) {
                overlayView?.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun togglePositionLock() {
        lifecycleScope.launch {
            val current = preferencesRepository.current()
            preferencesRepository.update { it.copy(positionLocked = !current.positionLocked) }
        }
    }

    private fun openSelectedApp() {
        lifecycleScope.launch {
            // Placeholder: this logic would normally resolve the app and execute via petActionExecutor
        }
    }

    companion object {
        private const val CHANNEL_ID = "pocket_pet_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val AMBIENT_TICK_INTERVAL_MILLIS = 30_000L

        @Volatile var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (isRunning) return
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
