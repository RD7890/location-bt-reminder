package com.reminder.locationbt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Listens for system broadcasts when Bluetooth or Location state changes.
 * - If at least one is still ON → ensure the service is running.
 * - If both are OFF       → stop the service immediately.
 *
 * This is the battery-friendly reactive path; the service's own Handler loop
 * is a fallback for cases where the broadcast is delayed or missed.
 */
class StateChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "StateChangeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received: ${intent.action}")

        // We just ensure the service is running. The service itself handles looping vs pausing based on Location state.
        Log.d(TAG, "Location state changed broadcast received → ensuring ReminderService is running.")
        ReminderService.start(context)
    }
}
