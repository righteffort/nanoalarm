# Android Alarm POC — Development Spike

**Scope:** One-time development spike to prove out correct native Android alarm delivery. Target: personal Pixel device. Schedule a single one-time alarm at a single point in time; it fires and rings reliably.

Not in scope: repeat/interval logic, settings, snooze, persistence beyond the one scheduled alarm, reboot recovery, or any production polish. This is a throwaway spike — but should still be correct, not sloppy.

---

## Scheduling

- Use `AlarmManager.setAlarmClock()` for the alarm. This is the exact, non-deferrable alarm type intended for user-facing alarm-clock functionality — the OS guarantees delivery for it even in Doze, so no foreground service, wake-lock polling loop, or battery-optimization exemption is needed.
- Ref: https://developer.android.com/develop/background-work/services/alarms

## Permissions (manifest)

- `USE_EXACT_ALARM` — normal permission, auto-granted at install for alarm-clock-category apps. Do **not** use `SCHEDULE_EXACT_ALARM`.
- `USE_FULL_SCREEN_INTENT` — lets the ringing activity launch over the lock screen; alarm-category apps get this by default.
- `WAKE_LOCK` — acquire briefly on alarm fire to bring up the ringing activity/audio, release once active.
- `POST_NOTIFICATIONS` (API 33+) — required for the alarm's associated notification.
- `VIBRATE` — only if trivial to add; skip if it adds friction.

Refs:
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://source.android.com/docs/core/permissions/fsi-limits
- https://developer.android.com/about/versions/14/behavior-changes-14

Explicitly **excluded**: `SYSTEM_ALERT_WINDOW`, any foreground service, any battery-optimization-exemption request (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), `RECEIVE_BOOT_COMPLETED`. None are required for a single exact `setAlarmClock()` call, and reboot recovery is out of scope for this spike.

## Components (4 files, no more)

1. **`MainActivity`** — one `DatePicker`/`TimePicker` (or a single `EditText` for epoch millis, to skip UI entirely) + one button. On tap: compute the trigger time, call `AlarmManager.setAlarmClock()` targeting a `PendingIntent` for `AlarmReceiver`. Create the notification channel once here (`onCreate`).
2. **`AlarmReceiver`** (`BroadcastReceiver`) — receives the alarm broadcast, launches `RingingActivity` via a full-screen `Intent` (`FLAG_ACTIVITY_NEW_TASK`), and posts the notification through the alarm channel.
3. **`RingingActivity`** — `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, plays the system's default alarm sound (`RingtoneManager.TYPE_ALARM`) with `AudioAttributes.USAGE_ALARM` (`STREAM_ALARM`, so it follows alarm volume and overrides DND/silent correctly), one "Dismiss" button that stops playback and calls `finish()`. No bundled audio asset — using the system default avoids adding a binary asset file for a throwaway spike, with identical `USAGE_ALARM`/DND-bypass behavior.
4. **`AndroidManifest.xml`** — permissions above, `AlarmReceiver` and `RingingActivity` declared, notification channel importance set high.

## Audio

- Play via `AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)`. This ties playback to the system alarm volume and makes it correctly bypass Do Not Disturb/silent mode — do not route through `STREAM_NOTIFICATION` or `STREAM_MUSIC`.
- Use the device's default alarm ringtone (`RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)`) rather than bundling a sound file — same audio-attribute behavior, no extra binary asset needed for a throwaway spike.

## Acceptance for the spike: run manually by a human, not an AI agent.

- App installed on the Pixel, no manual permission grants beyond what's auto-granted.
- Pick a time a minute or two out, tap Set, lock the phone.
- Alarm fires exactly on time, screen turns on over the lock screen, sound plays at alarm volume even if phone is silenced, Dismiss stops it.
- If the phone reboots before the alarm fires, it's lost — acceptable, known limitation, not a bug for this spike.
