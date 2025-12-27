package com.example.budgetquest.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.budgetquest.data.SettingsRepository
import com.example.budgetquest.worker.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import com.example.budgetquest.data.BackupManager
import com.example.budgetquest.ui.AppViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.plan.PlanViewModel // 用來獲取 DAO 或 Repository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 日系配色 (保持與其他頁面一致)
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    val settingsRepo = remember { SettingsRepository(context) }

    // 狀態
    var dailyReminder by remember { mutableStateOf(settingsRepo.isDailyReminderEnabled) }
    var reminderTime by remember { mutableStateOf(settingsRepo.reminderTime) }
    var planEndReminder by remember { mutableStateOf(settingsRepo.isPlanEndReminderEnabled) }

    // 取得資料庫實體 (為了 Checkpoint)
    // 透過 application context 拿到 database
    val app = context.applicationContext as com.example.budgetquest.BudgetQuestApplication
    val budgetDao = app.container.budgetRepository.let {
        // 這裡需要一點技巧拿到 DAO，或是直接在 Repository 加一個 checkpoint 方法
        // 為了不改太多架構，我們假設 Repository 可以執行 raw query
        // 但最好的方式是：
        com.example.budgetquest.data.BudgetDatabase.getDatabase(context).budgetDao()
    }

    // [備份] 檔案建立器
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    backupManager.backupDatabase(uri, budgetDao)
                    Toast.makeText(context, "備份成功！", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "備份失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    // [還原] 檔案選取器
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    backupManager.restoreDatabase(uri)
                    Toast.makeText(context, "還原成功！請重新啟動 APP 以載入資料", Toast.LENGTH_LONG).show()
                    // 選用：強制重啟或退出
                    // System.exit(0)
                } catch (e: Exception) {
                    Toast.makeText(context, "還原失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 定義排程邏輯
    fun updateWorker(enabled: Boolean, time: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("daily_reminder") // 先取消舊的

        if (enabled) {
            val parts = time.split(":").map { it.toInt() }
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0])
                set(Calendar.MINUTE, parts[1])
                set(Calendar.SECOND, 0)
            }
            if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("daily_reminder")
                .build()

            workManager.enqueueUniquePeriodicWork("daily_reminder", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
        }
    }

    // 權限請求器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                dailyReminder = true
                settingsRepo.isDailyReminderEnabled = true
                updateWorker(true, reminderTime)
            } else {
                dailyReminder = false
                settingsRepo.isDailyReminderEnabled = false
                updateWorker(false, reminderTime)
            }
        }
    )

    Scaffold(
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = { Text("設定", color = JapaneseTextPrimary, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = JapaneseTextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(20.dp)) {

            // [修改] 使用新的可折疊卡片元件
            NotificationSettingsCard(
                dailyReminder = dailyReminder,
                reminderTime = reminderTime,
                planEndReminder = planEndReminder,
                onDailyReminderChange = { isChecked ->
                    if (isChecked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                dailyReminder = true
                                settingsRepo.isDailyReminderEnabled = true
                                updateWorker(true, reminderTime)
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            dailyReminder = true
                            settingsRepo.isDailyReminderEnabled = true
                            updateWorker(true, reminderTime)
                        }
                    } else {
                        dailyReminder = false
                        settingsRepo.isDailyReminderEnabled = false
                        updateWorker(false, reminderTime)
                    }
                },
                onTimeClick = {
                    TimePickerDialog(context, { _, h, m ->
                        val timeStr = String.format("%02d:%02d", h, m)
                        reminderTime = timeStr
                        settingsRepo.reminderTime = timeStr
                        updateWorker(true, timeStr)
                    }, reminderTime.split(":")[0].toInt(), reminderTime.split(":")[1].toInt(), true).show()
                },
                onPlanEndReminderChange = {
                    planEndReminder = it
                    settingsRepo.isPlanEndReminderEnabled = it
                },
                onTestNotificationClick = {
                    // 發送一次性測試通知
                    val testRequest = OneTimeWorkRequestBuilder<ReminderWorker>().build()
                    WorkManager.getInstance(context).enqueue(testRequest)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // [新增] 雲端備份卡片
            CloudBackupCard(
                onBackupClick = {
                    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    backupLauncher.launch("BudgetQuest_Backup_$dateStr.db")
                },
                onRestoreClick = {
                    restoreLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                }
            )
        }
    }
}

// [新增] 備份卡片 UI
@Composable
fun CloudBackupCard(
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 日系配色
    val JapaneseSurface = Color.White
    val JapaneseTextPrimary = Color(0xFF455A64)
    val JapaneseTextSecondary = Color(0xFF90A4AE)

    Card(
        colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, null, tint = JapaneseTextPrimary, modifier = Modifier.size(20.dp)) // 需 import Cloud icon
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("雲端備份與還原", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = JapaneseTextPrimary)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = JapaneseTextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color(0xFFF7F9FC), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "將資料備份至 Google Drive (需登入 Drive App)",
                        fontSize = 12.sp,
                        color = JapaneseTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onBackupClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78909C)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp)) // 需 import Upload
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("備份", fontSize = 14.sp)
                        }

                        Button(
                            onClick = onRestoreClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECEFF1), contentColor = JapaneseTextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp)) // 需 import Download
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("還原", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
// [新增] 可折疊的通知設定卡片
@Composable
fun NotificationSettingsCard(
    dailyReminder: Boolean,
    reminderTime: String,
    planEndReminder: Boolean,
    onDailyReminderChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    onPlanEndReminderChange: (Boolean) -> Unit,
    onTestNotificationClick: () -> Unit
) {
    // 控制展開狀態，預設為 false (收起)
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 標題列 (點擊可切換展開)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = JapaneseTextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("通知設定", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = JapaneseTextPrimary)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = JapaneseTextSecondary
                )
            }

            // 展開的內容
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = JapaneseBg, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. 每日提醒開關
                    SettingsSwitchItem(title = "每日記帳提醒", checked = dailyReminder, onCheckedChange = onDailyReminderChange)

                    // 2. 時間選擇 (有開啟才顯示)
                    if (dailyReminder) {
                        SettingsActionItem("提醒時間", reminderTime, onTimeClick)
                    }

                    // 3. 計畫結束通知
                    SettingsSwitchItem(title = "計畫結算通知", subtitle = "計畫結束當天通知", checked = planEndReminder, onCheckedChange = onPlanEndReminderChange)

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. [美化] 測試通知按鈕
                    // 使用柔和的淺色背景按鈕，符合日系風格
                    Button(
                        onClick = onTestNotificationClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFECEFF1), // 淺灰藍背景
                            contentColor = JapaneseTextPrimary  // 深色文字
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("發送測試通知", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "點擊後若無反應，請檢查手機設定中的通知權限。",
                        fontSize = 11.sp,
                        color = JapaneseTextSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

// 保持原本的 Item 元件，稍微調整樣式
@Composable
fun SettingsSwitchItem(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 15.sp, color = JapaneseTextPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = JapaneseTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = JapaneseAccent,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = JapaneseBg,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsActionItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = JapaneseTextPrimary)
        Surface(
            color = JapaneseBg,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = JapaneseTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}