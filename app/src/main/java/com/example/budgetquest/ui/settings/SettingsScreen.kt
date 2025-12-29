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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // 語言選擇對話框
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
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { debounce(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            AppearanceCard(
                isDarkMode = isDarkMode,
                onToggle = { checked ->
                    isDarkMode = checked
                    settingsRepo.isDarkModeEnabled = checked
                    onDarkModeToggle(checked)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            LanguageCard(
                onClick = { debounce { showLanguageDialog = true } }
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AppTheme.colors.divider)

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

@Composable
fun LanguageCard(onClick: () -> Unit) {
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (!currentLocales.isEmpty) currentLocales[0]?.toLanguageTag() else ""

    // [新增] 支援 zh-CN
    val displayLanguage = when (currentTag) {
        "zh-TW" -> stringResource(R.string.language_zh_tw)
        "zh-CN" -> stringResource(R.string.language_zh_cn) // [新增]
        "en" -> stringResource(R.string.language_en)
        "ja" -> stringResource(R.string.language_ja)
        else -> stringResource(R.string.language_system)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
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
    // [新增] 支援 zh-CN
    val languages = listOf(
        "" to stringResource(R.string.language_system),
        "zh-TW" to stringResource(R.string.language_zh_tw),
        "zh-CN" to stringResource(R.string.language_zh_cn), // [新增]
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

// ... CloudBackupCard, NotificationSettingsCard, SettingsSwitchItem, SettingsActionItem, AppearanceCard, SettingsItem ...
// 這些共用元件保持不變
@Composable
fun CloudBackupCard(
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
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
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.desc_cloud_backup),
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onBackupClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.background,
                                contentColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_backup), fontSize = 14.sp)
                        }

                        Button(
                            onClick = onRestoreClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.background,
                                contentColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_restore), fontSize = 14.sp)
                        }
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

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
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
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSwitchItem(title = stringResource(R.string.label_daily_reminder), checked = dailyReminder, onCheckedChange = onDailyReminderChange)

                    if (dailyReminder) {
                        SettingsActionItem(stringResource(R.string.label_reminder_time), reminderTime, onTimeClick)
                    }

                    SettingsSwitchItem(title = stringResource(R.string.label_plan_end_reminder), subtitle = stringResource(R.string.desc_plan_end_reminder), checked = planEndReminder, onCheckedChange = onPlanEndReminderChange)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onTestNotificationClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.background,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_test_notification), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

@Composable
fun SettingsSwitchItem(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 15.sp, color = AppTheme.colors.textPrimary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = AppTheme.colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppTheme.colors.accent,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = AppTheme.colors.background,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsActionItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = AppTheme.colors.textPrimary)
        Surface(
            color = AppTheme.colors.background,
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
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
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
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AppTheme.colors.accent,
                    uncheckedTrackColor = AppTheme.colors.background
                )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
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