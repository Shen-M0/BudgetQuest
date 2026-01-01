package com.example.budgetquest.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("budget_quest_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_DAILY_REMINDER = "daily_reminder"
        private const val KEY_REMINDER_TIME = "reminder_time"
        private const val KEY_PLAN_END_REMINDER = "plan_end_reminder"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    // [修正] 預設改為 false
    var isDailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDER, value).apply()

    var reminderTime: String
        get() = prefs.getString(KEY_REMINDER_TIME, "20:00") ?: "20:00"
        set(value) = prefs.edit().putString(KEY_REMINDER_TIME, value).apply()

    // [修正] 預設改為 false
    var isPlanEndReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_PLAN_END_REMINDER, false)
        set(value) = prefs.edit().putBoolean(KEY_PLAN_END_REMINDER, value).apply()

    var isDarkModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
}