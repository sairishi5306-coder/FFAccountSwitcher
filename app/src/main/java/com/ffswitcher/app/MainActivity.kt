package com.ffswitcher.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var edtAccountsPath: EditText
    private lateinit var edtCachePath: EditText
    private lateinit var txtOverlayStatus: TextView
    private lateinit var txtStorageStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtAccountsPath = findViewById(R.id.edtAccountsPath)
        edtCachePath = findViewById(R.id.edtCachePath)
        txtOverlayStatus = findViewById(R.id.txtOverlayStatus)
        txtStorageStatus = findViewById(R.id.txtStorageStatus)

        edtAccountsPath.setText(Prefs.getAccountsPath(this))
        edtCachePath.setText(Prefs.getCachePath(this))

        findViewById<Button>(R.id.btnSavePaths).setOnClickListener {
            val accPath = edtAccountsPath.text.toString().trim()
            val cachePath = edtCachePath.text.toString().trim()
            if (accPath.isEmpty() || cachePath.isEmpty()) {
                Toast.makeText(this, "Dono path bharo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.savePaths(this, accPath, cachePath)
            Toast.makeText(this, "Paths saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnGrantOverlay).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnGrantStorage).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    100
                )
            }
        }

        findViewById<Button>(R.id.btnStartOverlay).setOnClickListener {
            startFloatingSwitcher()
        }

        findViewById<Button>(R.id.btnStopOverlay).setOnClickListener {
            stopService(Intent(this, FloatingOverlayService::class.java))
            Toast.makeText(this, "Floating switcher band kar diya", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        txtOverlayStatus.text = "Overlay permission: " + if (overlayGranted) "✅ Granted" else "❌ Not granted"

        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true

        txtStorageStatus.text = "Storage permission: " + if (storageGranted) "✅ Granted" else "❌ Not granted"
    }

    private fun startFloatingSwitcher() {
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true

        if (!overlayGranted) {
            Toast.makeText(this, "Pehle Overlay permission do", Toast.LENGTH_LONG).show()
            return
        }
        if (!storageGranted) {
            Toast.makeText(this, "Pehle Storage (All Files Access) permission do", Toast.LENGTH_LONG).show()
            return
        }
        if (Prefs.getAccountsPath(this).isBlank() || Prefs.getCachePath(this).isBlank()) {
            Toast.makeText(this, "Pehle dono file path save karo", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Floating switcher shuru ho gaya", Toast.LENGTH_SHORT).show()
    }
}
