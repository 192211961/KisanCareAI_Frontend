package com.simats.kisancareai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmHelper {
    private const val ALARM_REQUEST_CODE = 2001

    fun scheduleDailyTipAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyTipReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelDailyTipAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyTipReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private const val WEATHER_ALARM_CODE = 2002

    fun scheduleNextWeatherAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeatherAlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, WEATHER_ALARM_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Standard production alert slots: 7 AM, 12 PM, 6 PM
        val slots = arrayOf(Pair(7, 0), Pair(12, 0), Pair(18, 0))
        val now = Calendar.getInstance()
        var nextSlotTime: Long = 0

        for (slotPair in slots) {
            val slot = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, slotPair.first)
                set(Calendar.MINUTE, slotPair.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND,0)
            }
            if (slot.after(now)) {
                nextSlotTime = slot.timeInMillis
                break
            }
        }

        if (nextSlotTime == 0L) {
            // All slots today passed, schedule first slot tomorrow
            val firstSlot = slots[0]
            val slot = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, firstSlot.first)
                set(Calendar.MINUTE, firstSlot.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            nextSlotTime = slot.timeInMillis
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextSlotTime, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextSlotTime, pendingIntent)
        }
    }

    fun cancelWeatherAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeatherAlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, WEATHER_ALARM_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
