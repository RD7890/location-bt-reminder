package com.reminder.locationbt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground service that:
 *  - Plays a specific sound when Location is ON.
 *  - Plays a different sound when Bluetooth is ON.
 *  - Stops itself when both are OFF.
 *
 * The reminder interval is [REMINDER_INTERVAL_MS]. Battery impact is minimal because
 * the service only schedules a Handler post — no wake locks, no busy-polling.
 * System-level BroadcastReceiver (StateChangeReceiver) can also start/stop this service
 * reactively whenever Bluetooth or Location state changes.
 */
class ReminderService : Service() {

    companion object {
        private const val TAG = "ReminderService"
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIF_ID = 1
        private const val REMINDER_INTERVAL_MS = 60_000L  // every 60 seconds

        fun start(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var soundPool: SoundPool
    private var soundLocation = 0
    private var soundBluetooth = 0
    private var soundsLoaded = false

    private val handler = Handler(Looper.getMainLooper())
    private val reminderRunnable = object : Runnable {
        override fun run() {
            if (DeviceState.bothOff(this@ReminderService)) {
                Log.d(TAG, "Both off — stopping service.")
                stopSelf()
                return
            }
            playReminders()
            handler.postDelayed(this, REMINDER_INTERVAL_MS)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        initSoundPool()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check immediately; if both already off don't bother starting loop.
        if (DeviceState.bothOff(this)) {
            Log.d(TAG, "Both already off on start — stopping.")
            stopSelf()
            return START_NOT_STICKY
        }
        // Remove any pending callbacks before scheduling (handles re-starts cleanly).
        handler.removeCallbacks(reminderRunnable)
        handler.post(reminderRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(reminderRunnable)
        if (::soundPool.isInitialized) soundPool.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Sound ─────────────────────────────────────────────────────────────────

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            // Both sounds will trigger this; track when at least both are ready
            soundsLoaded = true
        }

        soundLocation = soundPool.load(this, R.raw.sound_location, 1)
        soundBluetooth = soundPool.load(this, R.raw.sound_bluetooth, 1)
    }

    private fun playReminders() {
        if (!soundsLoaded) return

        val locationOn = DeviceState.isLocationOn(this)
        val bluetoothOn = DeviceState.isBluetoothOn(this)

        if (locationOn) {
            Log.d(TAG, "Location ON — playing location reminder sound.")
            soundPool.play(soundLocation, 1f, 1f, 1, 0, 1f)
        }

        if (bluetoothOn) {
            Log.d(TAG, "Bluetooth ON — playing bluetooth reminder sound.")
            // Slight delay so the two sounds don't fully overlap when both are on
            handler.postDelayed({
                soundPool.play(soundBluetooth, 1f, 1f, 1, 0, 1f)
            }, 1_500L)
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminder Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Location & Bluetooth reminder service" }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
