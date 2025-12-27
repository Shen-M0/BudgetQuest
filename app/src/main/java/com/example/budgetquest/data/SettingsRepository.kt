package com.example.budgetquest.data

import android.content.Context
import androidx.core.content.edit

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("budget_quest_settings", Context.MODE_PRIVATE)

    var isDailyReminderEnabled: Boolean
        get() = prefs.getBoolean("daily_reminder", false)
        set(value) = prefs.edit { putBoolean("daily_reminder", value) }

    // 儲存格式 "HH:mm"
    var reminderTime: String
        get() = prefs.getString("reminder_time", "21:00") ?: "21:00"
        set(value) = prefs.edit { putString("reminder_time", value) }

    var isPlanEndReminderEnabled: Boolean
        get() = prefs.getBoolean("plan_end_reminder", true)
        set(value) = prefs.edit { putBoolean("plan_end_reminder", value) }
}