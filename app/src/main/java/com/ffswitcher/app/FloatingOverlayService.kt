package com.ffswitcher.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * Foreground Service that draws a small floating card (like a game
 * booster menu) over other apps. Shows current account's UID/Password,
 * a RED "Previous" and GREEN "Next" button, and auto-writes the
 * account into the Free Fire guest cache file every time it changes -
 * exactly like the Telegram bot version, just as a native overlay.
 */
class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private var accounts: List<Account> = emptyList()
    private var currentIndex: Int = 0
    private lateinit var cachePath: String

    companion object {
        const val CHANNEL_ID = "ff_switcher_channel"
        const val NOTIF_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        loadDataAndShowOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    // --------------------------------------------------------------
    // Notification (required for a foreground service)
    // --------------------------------------------------------------
    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FF Account Switcher",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FF Account Switcher chal raha hai")
            .setContentText("Floating menu active")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    // --------------------------------------------------------------
    // Load accounts + build the floating view
    // --------------------------------------------------------------
    private fun loadDataAndShowOverlay() {
        val accountsPath = Prefs.getAccountsPath(this)
        cachePath = Prefs.getCachePath(this)

        try {
            accounts = AccountStore.loadAccounts(accountsPath)
        } catch (e: Exception) {
            accounts = emptyList()
        }

        currentIndex = Prefs.getIndex(this).coerceIn(0, (accounts.size - 1).coerceAtLeast(0))

        showOverlay()
        updateOverlayUi()
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_view, null)
        overlayView = view

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 40
        params.y = 200

        windowManager.addView(view, params)

        // Drag to move
        val dragHandle = view.findViewById<View>(R.id.dragHandle)
        dragHandle.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        view.findViewById<View>(R.id.btnPrevious).setOnClickListener {
            if (currentIndex > 0) {
                currentIndex -= 1
                Prefs.saveIndex(this, currentIndex)
                updateOverlayUi()
            }
        }

        view.findViewById<View>(R.id.btnNext).setOnClickListener {
            if (currentIndex < accounts.size - 1) {
                currentIndex += 1
                Prefs.saveIndex(this, currentIndex)
                updateOverlayUi()
            }
        }

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            stopSelf()
        }
    }

    private fun updateOverlayUi() {
        val view = overlayView ?: return

        val txtCounter = view.findViewById<TextView>(R.id.txtCounter)
        val txtUid = view.findViewById<TextView>(R.id.txtUid)
        val txtPassword = view.findViewById<TextView>(R.id.txtPassword)
        val txtStatus = view.findViewById<TextView>(R.id.txtStatus)
        val btnPrevious = view.findViewById<TextView>(R.id.btnPrevious)
        val btnNext = view.findViewById<TextView>(R.id.btnNext)

        if (accounts.isEmpty()) {
            txtCounter.text = "Koi account nahi mila"
            txtUid.text = "UID: -"
            txtPassword.text = "Password: -"
            txtStatus.text = "Accounts path check karo (app kholke)"
            btnPrevious.visibility = View.INVISIBLE
            btnNext.visibility = View.INVISIBLE
            return
        }

        val acc = accounts[currentIndex]
        txtCounter.text = "Account ${currentIndex + 1}/${accounts.size}"
        txtUid.text = "UID: ${acc.uid}"
        txtPassword.text = "Password: ${acc.password}"

        btnPrevious.visibility = if (currentIndex > 0) View.VISIBLE else View.INVISIBLE
        btnNext.visibility = if (currentIndex < accounts.size - 1) View.VISIBLE else View.INVISIBLE

        val result = AccountStore.writeCache(cachePath, acc.uid, acc.password)
        txtStatus.text = if (result.isSuccess) {
            "Cache file updated - ab game me Recover dabao"
        } else {
            "Cache write fail: ${result.exceptionOrNull()?.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayView = null
    }
}
