package org.righteffort.nanoalarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    companion object {
        const val ALARM_CHANNEL_ID = "nanoalarm_alarm_channel"
        private const val ALARM_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createAlarmNotificationChannel()
        setContent {
            NanoAlarmScreen(onScheduleAlarm = ::scheduleAlarm)
        }
    }

    private fun createAlarmNotificationChannel() {
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleAlarm(triggerAtMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val showIntent = Intent(this, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            this,
            ALARM_REQUEST_CODE,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
            alarmPendingIntent
        )
    }
}

@Composable
fun NanoAlarmScreen(onScheduleAlarm: (Long) -> Unit) {
    val context = LocalContext.current
    var minutesFromNow by remember { mutableStateOf("1") }
    var statusText by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "NanoAlarm — alarm POC")

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    value = minutesFromNow,
                    onValueChange = { minutesFromNow = it },
                    label = { Text("Minutes from now") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val minutes = minutesFromNow.toLongOrNull()
                        if (minutes == null || minutes <= 0) {
                            statusText = "Enter a positive number of minutes"
                        } else {
                            val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
                            onScheduleAlarm(triggerAtMillis)
                            statusText = "Alarm set for $minutes minute(s) from now"
                        }
                    }
                ) {
                    Text("Set alarm")
                }

                if (statusText.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = statusText
                    )
                }
            }
        }
    }
}
