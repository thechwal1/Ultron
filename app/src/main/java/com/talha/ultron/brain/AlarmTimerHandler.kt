package com.talha.ultron.brain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class AlarmTimerHandler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val alarmPattern = Pattern.compile(
        "(?i)set (?:an? )?alarm (?:for )?" +
        "(\d{1,2})(?::(\d{2}))?\s*(am|pm)?"
    )
    private val timerPattern = Pattern.compile(
        "(?i)set (?:a )?timer (?:for )?" +
        "(\d+)\s*(second|minute|hour|min|sec|hr)s?"
    )

    fun tryHandle(input: String): String? {
        val alarmMatcher = alarmPattern.matcher(input)
        if (alarmMatcher.find()) {
            val hour = alarmMatcher.group(1)?.toIntOrNull() ?: return null
            val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = alarmMatcher.group(3)
            return setAlarm(hour, minute, ampm)
        }

        val timerMatcher = timerPattern.matcher(input)
        if (timerMatcher.find()) {
            val amount = timerMatcher.group(1)?.toIntOrNull() ?: return null
            val unit = timerMatcher.group(2)?.lowercase(Locale.ROOT) ?: "minute"
            return setTimer(amount, unit)
        }

        return null
    }

    private fun setAlarm(hour: Int, minute: Int, ampm: String?): String {
        var h = hour
        if (ampm != null) {
            if (ampm.equals("pm", ignoreCase = true) && h < 12) h += 12
            if (ampm.equals("am", ignoreCase = true) && h == 12) h = 0
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, h)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", h, minute)
        return "Opening alarm clock for $timeStr."
    }

    private fun setTimer(amount: Int, unit: String): String {
        val seconds = when (unit) {
            "second", "sec" -> amount
            "minute", "min" -> amount * 60
            "hour", "hr" -> amount * 3600
            else -> amount * 60
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening timer for $amount ${unit}s."
    }
}
