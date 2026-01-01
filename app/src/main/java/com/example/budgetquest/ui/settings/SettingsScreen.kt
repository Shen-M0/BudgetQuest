package com.example.budgetquest.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.budgetquest.R
import com.example.budgetquest.data.BackupManager
import com.example.budgetquest.data.SettingsRepository
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.GlassActionTextButton
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.worker.ReminderWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onReplayOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backupManager = remember { BackupManager(context) }
    val settingsRepo = remember { SettingsRepository(context) }

    var dailyReminder by remember { mutableStateOf(settingsRepo.isDailyReminderEnabled) }
    var reminderTime by remember { mutableStateOf(settingsRepo.reminderTime) }
    var planEndReminder by remember { mutableStateOf(settingsRepo.isPlanEndReminderEnabled) }
    var isDarkMode by remember { mutableStateOf(settingsRepo.isDarkModeEnabled) }

    var showLanguageDialog by remember { mutableStateOf(false) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageTag ->
                val localeList = if (languageTag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(languageTag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
                showLanguageDialog = false
            }
        )
    }

    val app = context.applicationContext as com.example.budgetquest.BudgetQuestApplication
    val budgetDao = app.container.budgetRepository.let {
        com.example.budgetquest.data.BudgetDatabase.getDatabase(context).budgetDao()
    }

    val msgBackupSuccess = stringResource(R.string.msg_backup_success)

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    backupManager.backupDatabase(uri, budgetDao)
                    Toast.makeText(context, msgBackupSuccess, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_backup_failed, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    backupManager.restoreDatabase(uri)
                    Toast.makeText(context, context.getString(R.string.msg_restore_success), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_restore_failed, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun updateWorker(enabled: Boolean, time: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("daily_reminder")
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    GlassIconButton(
                        onClick = { debounce(onBackClick) },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            AppearanceCard(
                isDarkMode = isDarkMode,
                onToggle = { checked ->
                    isDarkMode = checked
                    settingsRepo.isDarkModeEnabled = checked
                    onDarkModeToggle(checked)
                }
            )

            LanguageCard(
                onClick = { debounce { showLanguageDialog = true } }
            )

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
                    debounce {
                        TimePickerDialog(context, { _, h, m ->
                            val timeStr = String.format("%02d:%02d", h, m)
                            reminderTime = timeStr
                            settingsRepo.reminderTime = timeStr
                            updateWorker(true, timeStr)
                        }, reminderTime.split(":")[0].toInt(), reminderTime.split(":")[1].toInt(), true).show()
                    }
                },
                onPlanEndReminderChange = {
                    planEndReminder = it
                    settingsRepo.isPlanEndReminderEnabled = it
                },
                onTestNotificationClick = {
                    debounce {
                        val testRequest = OneTimeWorkRequestBuilder<ReminderWorker>().build()
                        WorkManager.getInstance(context).enqueue(testRequest)
                    }
                }
            )

            CloudBackupCard(
                onBackupClick = {
                    debounce {
                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                        backupLauncher.launch("BudgetQuest_Backup_$dateStr.db")
                    }
                },
                onRestoreClick = {
                    debounce {
                        restoreLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                    }
                }
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = stringResource(R.string.title_tutorial),
                subtitle = stringResource(R.string.desc_replay_tutorial),
                onClick = { debounce(onReplayOnboarding) }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// 統一的 Switch 樣式，解決「看起來像點」的問題
@Composable
private fun BudgetQuestSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    // 設定一個明顯的軌道顏色，讓開關在未選取時也是實心的膠囊狀
    val uncheckedTrackColor = if (isDark) {
        Color.White.copy(alpha = 0.3f)
    } else {
        Color.Black.copy(alpha = 0.2f)
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = AppTheme.colors.accent,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent, // 移除邊框
            uncheckedTrackColor = uncheckedTrackColor, // 使用實心顏色取代透明
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent // 移除邊框
        )
    )
}

// [移除] ContrastBackgroundRow (不再需要)

@Composable
fun LanguageCard(onClick: () -> Unit) {
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (!currentLocales.isEmpty) currentLocales[0]?.toLanguageTag() else ""

    val displayLanguage = when (currentTag) {
        "zh-TW" -> stringResource(R.string.language_zh_tw)
        "zh-CN" -> stringResource(R.string.language_zh_cn)
        "en" -> stringResource(R.string.language_en)
        "ja" -> stringResource(R.string.language_ja)
        else -> stringResource(R.string.language_system)
    }

    GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.title_language), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    Text(displayLanguage, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "" to stringResource(R.string.language_system),
        "zh-TW" to stringResource(R.string.language_zh_tw),
        "zh-CN" to stringResource(R.string.language_zh_cn),
        "en" to stringResource(R.string.language_en),
        "ja" to stringResource(R.string.language_ja)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_language)) },
        text = {
            Column {
                languages.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(tag) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            color = AppTheme.colors.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = AppTheme.colors.surface,
        titleContentColor = AppTheme.colors.textPrimary,
        textContentColor = AppTheme.colors.textPrimary
    )
}

@Composable
fun CloudBackupCard(
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, null, tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.title_cloud_backup), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .background(AppTheme.colors.surface.copy(alpha = 0.3f))
                        .padding(20.dp)
                ) {
                    Text(
                        stringResource(R.string.desc_cloud_backup),
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassActionTextButton(
                            text = stringResource(R.string.btn_backup),
                            icon = Icons.Default.Upload,
                            onClick = onBackupClick,
                            modifier = Modifier.weight(1f)
                        )

                        GlassActionTextButton(
                            text = stringResource(R.string.btn_restore),
                            icon = Icons.Default.Download,
                            onClick = onRestoreClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

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
    var expanded by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.title_notification_settings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .background(AppTheme.colors.surface.copy(alpha = 0.3f))
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // 適度間距
                ) {
                    // [修正] 直接使用 SettingsSwitchItem (已移除外部背景色)
                    SettingsSwitchItem(title = stringResource(R.string.label_daily_reminder), checked = dailyReminder, onCheckedChange = onDailyReminderChange)

                    if (dailyReminder) {
                        SettingsActionItem(stringResource(R.string.label_reminder_time), reminderTime, onTimeClick)
                    }

                    SettingsSwitchItem(title = stringResource(R.string.label_plan_end_reminder), subtitle = stringResource(R.string.desc_plan_end_reminder), checked = planEndReminder, onCheckedChange = onPlanEndReminderChange)

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassActionTextButton(
                        text = stringResource(R.string.btn_test_notification),
                        icon = Icons.Default.Notifications,
                        onClick = onTestNotificationClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.msg_check_notification_permission),
                        fontSize = 11.sp,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

// [修正] 移除 ContrastBackgroundRow，恢復透明背景，但保留 BudgetQuestSwitch
@Composable
fun SettingsSwitchItem(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = AppTheme.colors.textPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = AppTheme.colors.textSecondary)
        }
        // [保留] 統一的 BudgetQuestSwitch
        BudgetQuestSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// [修正] 移除 ContrastBackgroundRow，恢復透明背景
@Composable
fun SettingsActionItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = AppTheme.colors.textPrimary)
        Surface(
            color = AppTheme.colors.background.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AppearanceCard(isDarkMode: Boolean, onToggle: (Boolean) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.title_dark_mode), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    Text(if (isDarkMode) stringResource(R.string.status_on) else stringResource(R.string.status_off), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }
            BudgetQuestSwitch(
                checked = isDarkMode,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = AppTheme.colors.textPrimary)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}