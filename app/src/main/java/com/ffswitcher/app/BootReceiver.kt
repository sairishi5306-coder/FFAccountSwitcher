package com.ffswitcher.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Optional: if you want the floating switcher to auto-start after a
 * phone reboot (so you don't have to open the app again). Only starts
 * if paths were already saved AND overlay permission is already granted.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val accountsPath = Prefs.getAccountsPath(context)
        val cachePath = Prefs.getCachePath(context)
        if (accountsPath.isBlank() || cachePath.isBlank()) return

        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        if (!overlayGranted) return

        val serviceIntent = Intent(context, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
