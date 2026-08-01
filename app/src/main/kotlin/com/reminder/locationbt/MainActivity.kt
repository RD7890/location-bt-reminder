package com.reminder.locationbt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.reminder.locationbt.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQ_PERMISSIONS = 100
    }

    private val requiredPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnToggleReminder.setOnClickListener {
            if (ReminderService.isRunning) {
                stopReminderService()
            } else {
                if (hasPermissions()) {
                    startReminderService()
                } else {
                    requestPermissions()
                }
            }
        }

        binding.btnAutoStart.setOnClickListener {
            requestAutoStartPermission()
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun hasPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQ_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startReminderService()
            } else {
                Toast.makeText(this, R.string.permissions_needed, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Auto Start ────────────────────────────────────────────────────────────

    @SuppressLint("BatteryLife")
    private fun requestAutoStartPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "Auto Start (Ignore Battery) is already enabled!", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            Toast.makeText(this, "Not required on this Android version.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Service control ───────────────────────────────────────────────────────

    private fun startReminderService() {
        if (DeviceState.bothOff(this)) {
            Toast.makeText(this, R.string.both_already_off, Toast.LENGTH_SHORT).show()
        } else {
            ReminderService.start(this)
            Toast.makeText(this, R.string.reminder_started, Toast.LENGTH_SHORT).show()
        }
        refreshUi()
    }

    private fun stopReminderService() {
        ReminderService.stop(this)
        Toast.makeText(this, R.string.reminder_stopped, Toast.LENGTH_SHORT).show()
        refreshUi()
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun refreshUi() {
        val locOn = DeviceState.isLocationOn(this)
        val btOn = DeviceState.isBluetoothOn(this)

        binding.tvLocationStatus.text = getString(
            if (locOn) R.string.location_on else R.string.location_off
        )
        binding.tvBluetoothStatus.text = getString(
            if (btOn) R.string.bluetooth_on else R.string.bluetooth_off
        )

        binding.tvLocationStatus.setTextColor(
            getColor(if (locOn) R.color.status_on else R.color.status_off)
        )
        binding.tvBluetoothStatus.setTextColor(
            getColor(if (btOn) R.color.status_on else R.color.status_off)
        )

        if (ReminderService.isRunning) {
            binding.btnToggleReminder.text = getString(R.string.btn_stop_reminder)
        } else {
            binding.btnToggleReminder.text = getString(R.string.btn_start_reminder)
        }
    }
}
