package com.pocketpet.service.overlay

import android.content.Context
import com.pocketpet.core.domain.action.OverlayController

class OverlayControllerImpl(private val appContext: Context) : OverlayController {
    override fun start() = OverlayService.start(appContext)
    override fun stop() = OverlayService.stop(appContext)
    override fun isRunning(): Boolean = OverlayService.isRunning()
}
