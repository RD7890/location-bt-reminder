package com.reminder.locationbt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
            if (hasPermissions()) {
                startReminderService()
            } else {
                requestPermissions()
            }
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

        binding.btnToggleReminder.text = getString(R.string.btn_start_reminder)
    }
}
