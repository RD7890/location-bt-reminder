package com.reminder.locationbt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class ReminderService : Service() {

    companion object {
        private const val TAG = "ReminderService"
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIF_ID = 1
        private const val REMINDER_INTERVAL_MS = 30_000L
        
        var isRunning = false
            private set

        fun start(context: Context) {
            try {
                isRunning = true
                val intent = Intent(context, ReminderService::class.java)
                context.startForegroundService(intent)
            } catch (e: Exception) {
                isRunning = false
                Log.e(TAG, "Failed to start ReminderService: \${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                isRunning = false
                val intent = Intent(context, ReminderService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop ReminderService: \${e.message}")
            }
        }
    }

    private lateinit var soundPool: SoundPool
    private var soundLocation = 0
    private var soundsLoaded = false

    private val handler = Handler(Looper.getMainLooper())
    private var isLooping = false

    private val reminderRunnable = object : Runnable {
        override fun run() {
            if (DeviceState.isLocationOff(this@ReminderService)) {
                Log.d(TAG, "Location off — stopping loop.")
                isLooping = false
                return
            }
            playReminders()
            handler.postDelayed(this, REMINDER_INTERVAL_MS)
        }
    }

    // Dynamic receiver to catch location changes instantly
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Location state changed dynamically")
            checkAndLoop()
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        initSoundPool()

        val filter = IntentFilter().apply {
            addAction("android.location.PROVIDERS_CHANGED")
            addAction("android.location.MODE_CHANGED")
        }
        registerReceiver(locationReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkAndLoop()
        return START_STICKY
    }

    private fun checkAndLoop() {
        if (DeviceState.isLocationOff(this)) {
            Log.d(TAG, "Location is OFF. Stopping loop but keeping service alive.")
            isLooping = false
            handler.removeCallbacks(reminderRunnable)
        } else {
            if (!isLooping) {
                Log.d(TAG, "Location is ON. Starting reminder loop.")
                isLooping = true
                handler.post(reminderRunnable)
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        isLooping = false
        handler.removeCallbacks(reminderRunnable)
        unregisterReceiver(locationReceiver)
        if (::soundPool.isInitialized) soundPool.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            soundsLoaded = true
        }

        soundLocation = soundPool.load(this, R.raw.sound_location, 1)
    }

    private fun playReminders() {
        if (!soundsLoaded) return

        if (DeviceState.isLocationOn(this)) {
            Log.d(TAG, "Location ON — playing sound and vibrating.")
            soundPool.play(soundLocation, 1f, 1f, 1, 0, 1f)
            
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminder Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Monitoring Location state" }

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
