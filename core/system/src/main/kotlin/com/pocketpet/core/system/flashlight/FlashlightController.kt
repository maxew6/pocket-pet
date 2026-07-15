package com.pocketpet.core.system.flashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager

/**
 * Toggles the torch via [CameraManager.setTorchMode]. Deliberately does *not* request the
 * `CAMERA` permission: Android's torch API was specifically designed so flashlight-only apps
 * don't need camera access, and requesting it anyway would just be an unnecessary permission ask.
 */
class FlashlightController(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val torchCameraId: String? = findTorchCapableCameraId()

    fun setTorchEnabled(enabled: Boolean): Boolean {
        val id = torchCameraId ?: return false
        return try {
            cameraManager.setTorchMode(id, enabled)
            true
        } catch (e: CameraAccessException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun isTorchAvailable(): Boolean = torchCameraId != null

    private fun findTorchCapableCameraId(): String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: CameraAccessException) {
        null
    }
}
