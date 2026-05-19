package com.aboneliklerim.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import com.aboneliklerim.app.Subscription
import com.aboneliklerim.app.NotificationReceiver


object NotificationScheduler {

    fun scheduleAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val subs = loadData(context)
        
        // Calculate all trigger times first
        val activeNotifications = subs
            .filter { !it.isArchived && it.notifyDaysBefore >= 0 }
            .mapNotNull { sub ->
                val time = calculateNextTriggerTime(sub)
                if (time != null && time > System.currentTimeMillis()) {
                    sub to time
                } else null
            }
            .sortedBy { it.second } // Schedule nearest ones first
            .take(400) // Android system limit is 500, we stay safe at 400

        activeNotifications.forEach { (sub, triggerTime) ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("sub_id", sub.id)
                putExtra("sub_name", sub.name)
                putExtra("days_remaining", sub.notifyDaysBefore)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                sub.id.hashCode(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerTime, 600000, pendingIntent) 
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: Exception) {
                // Catch IllegalStateException (limit reached) or SecurityException
                e.printStackTrace()
            }
        }
    }

    fun cancelAlarm(context: Context, subId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            subId.hashCode(), 
            intent, 
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextTriggerTime(sub: Subscription): Long? {
        try {
            val now = Calendar.getInstance()
            
            val timeParts = sub.notifyTime.split(":")
            val notifyHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
            val notifyMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            var payCal = getNextPaymentDate(sub.startDate, sub.period) ?: return null
            
            // Setup trigger date for the upcoming payment
            var triggerCal = payCal.clone() as Calendar
            triggerCal.add(Calendar.DAY_OF_YEAR, -sub.notifyDaysBefore)
            triggerCal.set(Calendar.HOUR_OF_DAY, notifyHour)
            triggerCal.set(Calendar.MINUTE, notifyMinute)
            triggerCal.set(Calendar.SECOND, 0)
            triggerCal.set(Calendar.MILLISECOND, 0)

            // If this trigger date is already in the past (e.g., payment is tomorrow, but we wanted 3 days before notification)
            // We should find the NEXT payment date and schedule for that one instead to avoid missing alarms.
            if (triggerCal.before(now) && sub.period != "one-time") {
                val nextRef = payCal.clone() as Calendar
                nextRef.add(Calendar.DAY_OF_YEAR, 1)
                payCal = getNextPaymentDate(sub.startDate, sub.period, nextRef) ?: return null
                
                triggerCal = payCal.clone() as Calendar
                triggerCal.add(Calendar.DAY_OF_YEAR, -sub.notifyDaysBefore)
                triggerCal.set(Calendar.HOUR_OF_DAY, notifyHour)
                triggerCal.set(Calendar.MINUTE, notifyMinute)
            }

            return if (triggerCal.after(now)) triggerCal.timeInMillis else null
        } catch (e: Exception) { return null }
    }

    private fun loadData(context: Context): List<Subscription> {
        val json = context.getSharedPreferences("AboneliklerimData", Context.MODE_PRIVATE).getString("subs_list", null) ?: return emptyList()
        val type = object : TypeToken<MutableList<Subscription>>() {}.type
        return Gson().fromJson(json, type)
    }
}
