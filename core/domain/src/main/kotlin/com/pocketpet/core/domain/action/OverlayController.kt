package com.pocketpet.core.domain.action

/**
 * Starts, stops, and reports on the overlay foreground service. Implemented in `service:overlay`
 * (the only module allowed to reference the real `OverlayService` class); `feature:*` modules
 * depend on this interface so they never need a module dependency on `service:overlay` itself.
 */
interface OverlayController {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}
