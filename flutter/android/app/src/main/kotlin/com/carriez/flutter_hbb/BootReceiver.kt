package com.carriez.flutter_hbb

import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hjq.permissions.XXPermissions
import io.flutter.embedding.android.FlutterActivity

const val DEBUG_BOOT_COMPLETED = "com.carriez.flutter_hbb.DEBUG_BOOT_COMPLETED"

class BootReceiver : BroadcastReceiver() {
    private val logTag = "tagBootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(logTag, "onReceive $action")
        val isBoot = Intent.ACTION_BOOT_COMPLETED == action || DEBUG_BOOT_COMPLETED == action
        // Kiosk apps trigger this action after boot; it must not open MainActivity.
        val isManual = ACTION_START_SERVICE == action
        if (!isBoot && !isManual) {
            return
        }

        val prefs = context.getSharedPreferences(KEY_SHARED_PREFERENCES, FlutterActivity.MODE_PRIVATE)
        if (isBoot && !prefs.getBoolean(KEY_START_ON_BOOT_OPT, false)) {
            Log.d(logTag, "KEY_START_ON_BOOT_OPT is false")
            return
        }
        // The overlay permission is what allows a background service to launch the
        // transparent MediaProjection activity. The battery exemption is only needed
        // for the boot path (so the OS does not kill the foreground service).
        if (!XXPermissions.isGranted(context, SYSTEM_ALERT_WINDOW)) {
            Log.d(logTag, "SYSTEM_ALERT_WINDOW is not granted")
            return
        }
        if (isBoot && !XXPermissions.isGranted(context, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
            Log.d(logTag, "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is not granted")
            return
        }
        if (MainService.isReady) {
            Log.d(logTag, "MainService is already ready")
            return
        }

        val serviceIntent = Intent(context, MainService::class.java).apply {
            action = ACT_INIT_MEDIA_PROJECTION_AND_SERVICE
            putExtra(EXT_INIT_FROM_BOOT, true)
        }
        Log.d(logTag, "starting MainService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
