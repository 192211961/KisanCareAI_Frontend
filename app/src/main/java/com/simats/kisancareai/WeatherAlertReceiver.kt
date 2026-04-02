package com.simats.kisancareai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class WeatherAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("kisanmitra_settings", Context.MODE_PRIVATE)
        val isWeatherEnabled = prefs.getBoolean("weather_alerts", true)

        if (isWeatherEnabled) {
            // Only show notification if NOT triggered by system boot
            if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
                showNotification(context)
            }
            // Always schedule the next alarm slot
            AlarmHelper.scheduleNextWeatherAlarm(context)
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weather_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic weather updates (7 AM, 12 PM, 6 PM)"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Weather Update"
        val desc = "Check the latest weather forecast for your farm."

        val intent = Intent(context, WeatherForecastActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_partly_cloudy)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
