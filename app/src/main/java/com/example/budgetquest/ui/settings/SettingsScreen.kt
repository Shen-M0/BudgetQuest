package com.example.budgetquest.ui.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.core.os.LocaleListCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.budgetquest.R
import com.example.budgetquest.data.BackupManager
import com.example.budgetquest.data.FirebaseBackupManager
import com.example.budgetquest.data.SettingsRepository
import com.example.budgetquest.ui.common.GlassActionTextButton
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.rememberNotificationPermissionHandler
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.worker.ReminderWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val app = context.applicationContext as com.example.budgetquest.BudgetQuestApplication

    val settingsRepo = remember { SettingsRepository(context) }
    val currencyRepo = remember { app.currencyRepository }
    val backupManager = remember { BackupManager(context) }
    val firebaseBackupManager = remember { FirebaseBackupManager(context) }
    val budgetDao = remember {
        com.example.budgetquest.data.BudgetDatabase.getDatabase(context).budgetDao()
    }

    var dailyReminder by remember { mutableStateOf(settingsRepo.isDailyReminderEnabled) }
    var reminderTime by remember { mutableStateOf(settingsRepo.reminderTime) }
    var planEndReminder by remember { mutableStateOf(settingsRepo.isPlanEndReminderEnabled) }
    var isDarkMode by remember { mutableStateOf(settingsRepo.isDarkModeEnabled) }

    var currentBaseCurrency by remember { mutableStateOf(settingsRepo.baseCurrency) }
    var supportedCurrencies by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    var isCloudLoading by remember { mutableStateOf(false) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var isUserAnonymous by remember { mutableStateOf(false) }
    var lastBackupTimeDisplay by remember { mutableStateOf<String?>(null) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    // 資源字串
    val msgNoBackup = stringResource(R.string.msg_no_cloud_backup_found)
    val msgGoogleLoginSuccess = stringResource(R.string.msg_google_login_success)
    val msgLoginFailed = stringResource(R.string.error_login_failed)
    val msgGoogleLoginFailed = stringResource(R.string.error_google_login_failed)
    val msgCloudUploadSuccess = stringResource(R.string.msg_cloud_upload_success)
    val msgCloudRestoreSuccess = stringResource(R.string.msg_cloud_restore_success)
    val msgBackupFailed = stringResource(R.string.error_backup_failed)
    val msgRestoreFailed = stringResource(R.string.error_restore_failed)
    val msgLoginRequired = stringResource(R.string.error_login_required)
    val msgNetworkError = stringResource(R.string.error_network_login)
    val msgBackupSuccess = stringResource(R.string.msg_backup_success)
    val msgRestoreSuccess = stringResource(R.string.msg_restore_success)
    val msgOfflineError = stringResource(R.string.error_offline_missing_currency)
    val msgUpdateFailed = stringResource(R.string.error_currency_update_failed)

    fun refreshBackupTime() {
        scope.launch {
            val result = firebaseBackupManager.getLastBackupTime()
            result.onSuccess { timeMillis ->
                if (timeMillis != null) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    lastBackupTimeDisplay = sdf.format(Date(timeMillis))
                } else {
                    lastBackupTimeDisplay = msgNoBackup
                }
            }.onFailure {
                // 忽略錯誤或顯示預設值
            }
        }
    }

    LaunchedEffect(Unit) {
        var currencies = currencyRepo.getSupportedCurrencies()

        if (currencies.isEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    currencyRepo.getRates("TWD")
                }
                currencies = currencyRepo.getSupportedCurrencies()
            } catch (e: Exception) {
                currencies = listOf("TWD", "USD", "JPY", "CNY")
            }
        }

        if (currencies.isNotEmpty()) {
            supportedCurrencies = currencies
        } else {
            supportedCurrencies = listOf("TWD", "USD", "JPY","CNY")
        }
    }

    LaunchedEffect(Unit) {
        val user = firebaseBackupManager.getCurrentUser()
        if (user != null) {
            isUserAnonymous = user.isAnonymous
            currentUserEmail = user.email
            refreshBackupTime()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    scope.launch {
                        isCloudLoading = true
                        val authResult = firebaseBackupManager.signInWithGoogle(idToken)
                        authResult.onSuccess { user ->
                            isUserAnonymous = user.isAnonymous
                            currentUserEmail = user.email
                            Toast.makeText(context, msgGoogleLoginSuccess, Toast.LENGTH_SHORT).show()
                            refreshBackupTime()
                        }.onFailure {
                            Toast.makeText(context, "$msgLoginFailed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                        isCloudLoading = false
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "$msgGoogleLoginFailed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    backupManager.backupDatabase(uri, budgetDao)
                    Toast.makeText(context, msgBackupSuccess, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_backup_failed_format, e.message), Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, msgRestoreSuccess, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_restore_failed_format, e.message), Toast.LENGTH_LONG).show()
                }
            }
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

    val requestNotificationPermissions = rememberNotificationPermissionHandler {
        dailyReminder = true
        settingsRepo.isDailyReminderEnabled = true
        updateWorker(true, reminderTime)
    }

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

            CurrencySettingsCard(
                currentCurrency = currentBaseCurrency,
                onCurrencyClick = { showCurrencyDialog = true }
            )

            NotificationSettingsCard(
                dailyReminder = dailyReminder,
                reminderTime = reminderTime,
                planEndReminder = planEndReminder,
                onDailyReminderChange = { isChecked ->
                    if (isChecked) {
                        requestNotificationPermissions()
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

            BackupSettingsCard(
                isLoading = isCloudLoading,
                currentUserEmail = currentUserEmail,
                isUserAnonymous = isUserAnonymous,
                lastBackupTime = lastBackupTimeDisplay,
                onGoogleSignInClick = {
                    debounce {
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                onCloudUploadClick = {
                    debounce {
                        scope.launch {
                            isCloudLoading = true
                            val isLogged = if (firebaseBackupManager.getCurrentUser() == null) {
                                firebaseBackupManager.signInAnonymously()
                            } else true

                            if (isLogged) {
                                if (currentUserEmail == null) {
                                    val user = firebaseBackupManager.getCurrentUser()
                                    isUserAnonymous = user?.isAnonymous == true
                                    currentUserEmail = user?.email
                                }

                                val result = firebaseBackupManager.uploadBackup()
                                result.onSuccess {
                                    Toast.makeText(context, msgCloudUploadSuccess, Toast.LENGTH_SHORT).show()
                                    refreshBackupTime()
                                }.onFailure {
                                    Toast.makeText(context, "$msgBackupFailed: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, msgNetworkError, Toast.LENGTH_SHORT).show()
                            }
                            isCloudLoading = false
                        }
                    }
                },
                onCloudRestoreClick = {
                    debounce {
                        scope.launch {
                            isCloudLoading = true
                            val user = firebaseBackupManager.getCurrentUser()
                            if (user == null) {
                                Toast.makeText(context, msgLoginRequired, Toast.LENGTH_SHORT).show()
                            } else {
                                val result = firebaseBackupManager.restoreBackup()
                                result.onSuccess { msg ->
                                    Toast.makeText(context, msgCloudRestoreSuccess, Toast.LENGTH_LONG).show()
                                }.onFailure {
                                    Toast.makeText(context, "$msgRestoreFailed: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            isCloudLoading = false
                        }
                    }
                },
                onLocalBackupClick = {
                    debounce {
                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                        backupLauncher.launch("BudgetQuest_Backup_$dateStr.db")
                    }
                },
                onLocalRestoreClick = {
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

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentSelection = currentBaseCurrency,
            currencyList = supportedCurrencies,
            onDismiss = { showCurrencyDialog = false },
            onConfirm = { selected ->
                scope.launch {
                    settingsRepo.baseCurrency = selected
                    currentBaseCurrency = selected

                    try {
                        currencyRepo.getRates(baseCurrency = selected, forceRefresh = true)
                    } catch (e: Exception) {
                        if (e.message == "OFFLINE_MISSING_CURRENCY") {
                            Toast.makeText(context, msgOfflineError, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, msgUpdateFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                showCurrencyDialog = false
            }
        )
    }
}

@Composable
fun CurrencySettingsCard(
    currentCurrency: String,
    onCurrencyClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCurrencyClick() }
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
                    Text(stringResource(R.string.title_currency_settings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    Text(currentCurrency, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
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
fun CurrencySelectionDialog(
    currentSelection: String,
    currencyList: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_currency)) },
        text = {
            Column(
                modifier = Modifier
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                currencyList.forEach { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(code) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = if (code == currentSelection) AppTheme.colors.accent else AppTheme.colors.textPrimary
                        Text(
                            text = code,
                            fontSize = 16.sp,
                            fontWeight = if (code == currentSelection) FontWeight.Bold else FontWeight.Normal,
                            color = color
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
fun BackupSettingsCard(
    isLoading: Boolean,
    currentUserEmail: String?,
    isUserAnonymous: Boolean,
    lastBackupTime: String?,
    onGoogleSignInClick: () -> Unit,
    onCloudUploadClick: () -> Unit,
    onCloudRestoreClick: () -> Unit,
    onLocalBackupClick: () -> Unit,
    onLocalRestoreClick: () -> Unit
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
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AppTheme.colors.accent)
                } else {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .background(AppTheme.colors.surface.copy(alpha = 0.3f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.subtitle_cloud_firebase), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)

                        if (currentUserEmail != null) {
                            Text(currentUserEmail, fontSize = 10.sp, color = AppTheme.colors.textSecondary)
                        } else if (isUserAnonymous) {
                            Text(stringResource(R.string.label_anonymous_account), fontSize = 10.sp, color = AppTheme.colors.fail)
                        }
                    }

                    if (isUserAnonymous) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = AppTheme.colors.fail, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.warning_anonymous_risk),
                                fontSize = 11.sp,
                                color = AppTheme.colors.fail,
                                lineHeight = 14.sp
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.desc_cloud_sync),
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    if (currentUserEmail == null || isUserAnonymous) {
                        GlassActionTextButton(
                            text = stringResource(R.string.action_link_google),
                            icon = Icons.Default.AccountCircle,
                            onClick = onGoogleSignInClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (lastBackupTime != null) {
                        Text(
                            text = stringResource(R.string.label_last_backup, lastBackupTime),
                            fontSize = 11.sp,
                            color = AppTheme.colors.success
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.msg_no_cloud_backup_found),
                            fontSize = 11.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassActionTextButton(
                            text = stringResource(R.string.action_upload),
                            icon = Icons.Default.CloudUpload,
                            onClick = onCloudUploadClick,
                            modifier = Modifier.weight(1f)
                        )

                        GlassActionTextButton(
                            text = stringResource(R.string.action_download),
                            icon = Icons.Default.CloudDownload,
                            onClick = onCloudRestoreClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = AppTheme.colors.divider)

                    Text(stringResource(R.string.subtitle_local_backup), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)

                    Text(
                        stringResource(R.string.desc_local_backup),
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassActionTextButton(
                            text = stringResource(R.string.btn_backup),
                            icon = Icons.Default.Upload,
                            onClick = onLocalBackupClick,
                            modifier = Modifier.weight(1f)
                        )

                        GlassActionTextButton(
                            text = stringResource(R.string.btn_restore),
                            icon = Icons.Default.Download,
                            onClick = onLocalRestoreClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetQuestSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
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
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
        BudgetQuestSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

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