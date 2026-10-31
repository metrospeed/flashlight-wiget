package com.flashlightwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast

/**
 * Owns the torch. The camera service turns the torch off when the process that
 * enabled it dies, so this runs as a foreground service for as long as the light is on.
 */
class FlashlightService : Service() {

    companion object {
        const val ACTION_TOGGLE = "com.flashlightwidget.action.TOGGLE"
        const val ACTION_OFF = "com.flashlightwidget.action.OFF"

        private const val CHANNEL_ID = "flashlight"
        private const val NOTIFICATION_ID = 1

        /** Whether the torch is on. Resets with the process, which also turns the torch off. */
        var isOn = false
            private set

        /** Notified on the main thread whenever [isOn] changes. */
        var listener: (() -> Unit)? = null

        fun intent(context: Context, action: String): Intent =
            Intent(context, FlashlightService::class.java).setAction(action)

        fun pendingIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getForegroundService(
                context,
                action.hashCode(),
                intent(context, action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    // Null until the camera service reports the current state, which it does right after registration.
    private var torchState: Boolean? = null
    private var pendingAction: String? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            torchState = enabled
            pendingAction?.let {
                pendingAction = null
                apply(it)
                return
            }
            setOn(enabled)
            // Turned off from elsewhere, e.g. the quick settings tile.
            if (!enabled) stopSelf()
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id != cameraId) return
            if (pendingAction != null) toast(R.string.flash_unavailable)
            pendingAction = null
            torchState = false
            setOn(false)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(CameraManager::class.java)
        cameraId = findFlashCamera()
        if (cameraId != null) {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must be called promptly after startForegroundService(), even when turning the light off.
        startInForeground()

        if (cameraId == null) {
            toast(R.string.no_flash)
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent?.action ?: ACTION_TOGGLE
        if (torchState == null) pendingAction = action else apply(action)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cameraManager.unregisterTorchCallback(torchCallback)
        if (isOn) {
            setTorch(false)
            setOn(false)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun apply(action: String) {
        val turnOn = action == ACTION_TOGGLE && torchState != true
        if (!setTorch(turnOn)) {
            toast(R.string.flash_unavailable)
            setOn(false)
            stopSelf()
        } else if (!turnOn) {
            setOn(false)
            stopSelf()
        }
        // When turning on, the torch callback confirms the new state.
    }

    private fun setTorch(enabled: Boolean): Boolean {
        val id = cameraId ?: return false
        return try {
            cameraManager.setTorchMode(id, enabled)
            true
        } catch (e: CameraAccessException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun setOn(on: Boolean) {
        if (isOn == on) return
        isOn = on
        FlashlightWidget.updateAll(this)
        listener?.invoke()
    }

    private fun findFlashCamera(): String? = try {
        val withFlash = cameraManager.cameraIdList.filter {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        withFlash.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    } catch (e: CameraAccessException) {
        null
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flashlight_on)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pendingIntent(this, ACTION_OFF))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
