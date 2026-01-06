package com.example.budgetquest.ui.common

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 處理通知與鬧鐘相關權限請求的邏輯
 * 流程：通知權限 -> 精確鬧鐘 -> 電池最佳化 -> 回呼
 */
@Composable
fun rememberNotificationPermissionHandler(
    onAllPermissionsGranted: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    // 1. 檢查是否在電池最佳化白名單中 (允許背景執行)
    fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // 2. 檢查是否允許設定精確鬧鐘 (Android 12+)
    fun checkExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 以下不需要此權限
        }
    }

    // 3. 檢查通知權限 (Android 13+)
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 以下不需要動態申請通知權限
        }
    }

    // --- 權限請求 Launchers ---

    // 用於請求通知權限
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 無論通知權限是否通過，都繼續檢查下一個權限 (精確鬧鐘)
        // 這樣可以確保即使拒絕通知，鬧鐘功能本身還是能嘗試運作
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkExactAlarmPermission()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } else if (!checkBatteryOptimization()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } else {
            // 如果全部都有了 (或是使用者拒絕了前面的，但我們還是嘗試啟用功能)
            if (isGranted) onAllPermissionsGranted()
        }
    }

    // --- 觸發流程的函式 ---
    // 這是這個 Composable 回傳給外部呼叫的 Lambda
    return remember {
        {
            // 步驟 1: 檢查通知權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !checkNotificationPermission()) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // 步驟 2: 檢查精確鬧鐘 (Android 12+)
            // 這裡使用 else if 是因為通知權限是非同步的，如果需要請求通知，後續流程會在 callback 中繼續
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkExactAlarmPermission()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            // 步驟 3: 檢查電池最佳化
            else if (!checkBatteryOptimization()) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            // 步驟 4: 全部通過
            else {
                onAllPermissionsGranted()
            }
        }
    }
}