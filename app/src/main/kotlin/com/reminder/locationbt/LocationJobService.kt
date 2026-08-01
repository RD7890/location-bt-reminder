package com.reminder.locationbt

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log

class LocationJobService : JobService() {
    companion object {
        private const val JOB_ID = 1001
        private const val TAG = "LocationJobService"

        fun schedule(context: Context) {
            val componentName = ComponentName(context, LocationJobService::class.java)
            
            // Listen to both LOCATION_MODE and LOCATION_PROVIDERS_ALLOWED for maximum compatibility across API levels
            val modeUri = Settings.Secure.getUriFor(Settings.Secure.LOCATION_MODE)
            val provUri = Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED)

            val builder = JobInfo.Builder(JOB_ID, componentName)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.addTriggerContentUri(JobInfo.TriggerContentUri(modeUri, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                builder.addTriggerContentUri(JobInfo.TriggerContentUri(provUri, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                // Minimal delay to let the setting fully apply before we check
                builder.setTriggerContentUpdateDelay(500)
                builder.setTriggerContentMaxDelay(2000)
            } else {
                // For pre-N, we don't have TriggerContentUri, but implicit broadcasts work fine anyway in manifest
                return
            }

            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            scheduler.schedule(builder.build())
            Log.d(TAG, "Location settings observation job scheduled")
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Location settings changed (Job triggered)")
        
        // We just ensure the service is running. The service itself handles looping vs pausing based on Location state.
        Log.d(TAG, "Location state changed (Job triggered) -> ensuring ReminderService is running.")
        ReminderService.start(this)

        // Reschedule to keep listening for the next change
        schedule(this)
        
        return false // Work is done synchronously
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Reschedule if system kills it
    }
}
