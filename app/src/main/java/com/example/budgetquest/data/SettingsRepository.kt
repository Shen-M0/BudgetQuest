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

    // [新增] 深色模式設定 (預設跟隨系統: -1, 淺色: 0, 深色: 1)
    // 為了簡單，我們這裡用 Boolean: false=淺色, true=深色
    var isDarkModeEnabled: Boolean
        get() = prefs.getBoolean("is_dark_mode", false)
        set(value) = prefs.edit { putBoolean("is_dark_mode", value) }

    // [新增] 是否第一次開啟 App
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit { putBoolean("is_first_launch", value) }

}