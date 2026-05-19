package com.aboneliklerim.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subId = intent.getStringExtra("sub_id") ?: return
        val subName = intent.getStringExtra("sub_name") ?: return
        val days = intent.getIntExtra("days_remaining", 0)

        // Force CPU to stay awake long enough to process the notification
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Aboneliklerim::NotificationWakelock")
        wakeLock.acquire(3000) // Hold for 3 seconds

        val channelId = "payment_reminders_v3" // V3 for clean slate
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableVibration(true)
                setSound(soundUri, android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, DetailActivity::class.java).apply {
            putExtra("sub_id", subId)
        }
        
        val pendingIntent = android.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(launchIntent)
            getPendingIntent(subId.hashCode(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val message = if (days == 0) {
            context.getString(R.string.payment_today_msg, subName)
        } else {
            context.getString(R.string.payment_days_left_msg, subName, days)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_calendar_soft) // MUST BE ALPHA ICON, NOT APP ICON
            .setContentTitle(context.getString(R.string.payment_reminder))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(subId.hashCode(), notification)
        
        // Reschedule for next period if needed
        NotificationScheduler.scheduleAlarms(context)
    }
}
