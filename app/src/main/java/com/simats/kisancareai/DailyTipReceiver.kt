package com.simats.kisancareai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class DailyTipReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("kisanmitra_settings", Context.MODE_PRIVATE)
        val isTipsEnabled = prefs.getBoolean("farming_tips", true)

        if (isTipsEnabled) {
            // Only show notification if it's NOT a boot event
            if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
                showNotification(context)
            }
            // Always reschedule to ensure the chain continues
            AlarmHelper.scheduleDailyTipAlarm(context)
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_tips_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Farming Tips",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily agricultural advice at 6:00 AM"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Get today's tip
        val today = Calendar.getInstance().time
        val tipIndex = DailyTipsManager.getTipIndexForDate(today)
        val title = context.getString(DailyTipsManager.getTipTitleResId(tipIndex))
        val desc = context.getString(DailyTipsManager.getTipDescResId(tipIndex))

        val intent = Intent(context, PersonalizedTipsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_lightbulb)
            .setContentTitle(title)
            .setContentText(desc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
