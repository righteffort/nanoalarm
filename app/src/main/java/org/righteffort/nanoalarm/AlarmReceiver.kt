package org.righteffort.nanoalarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val wakeLock = context.getSystemService(Context.POWER_SERVICE).let { it as PowerManager }
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nanoalarm:alarm_receiver")
        wakeLock.acquire(10_000L)
        try {
            val fullScreenIntent = Intent(context, RingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, MainActivity.ALARM_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.alarm_notification_title))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .build()

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(1, notification)
            }
            context.startActivity(fullScreenIntent)
        } finally {
            wakeLock.release()
        }
    }
}
