package com.example.budgetquest.ui.plan

import android.app.DatePickerDialog
import android.content.Context // [新增]
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraPrimaryButton
import com.example.budgetquest.ui.common.GlassActionTextButton
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    planId: Int?,
    initialStartDate: Long = -1L,
    initialEndDate: Long = -1L,
    showBackButton: Boolean = true,
    onBackClick: () -> Unit,
    onSaveClick: (Int) -> Unit,
    onDeleteClick: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var showDeleteExpensesConfirmation by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val uiState = viewModel.planUiState
    val supportedCurrencies = viewModel.supportedCurrencies

    val context = LocalContext.current

    // [新增] 檢查是否為「第一次設定幣別」
    // 我們直接檢查 SharedPreferences 中是否存在 "base_currency" 這個 Key
    // 如果不存在，代表使用者從未儲存過計畫或設定過幣別 -> 顯示選擇器
    // 如果存在，代表已經設定過 -> 隱藏選擇器
    val isFirstTimeCurrencySetup = remember {
        val prefs = context.getSharedPreferences("budget_quest_settings", Context.MODE_PRIVATE)
        !prefs.contains("base_currency")
    }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    val calendar = remember { Calendar.getInstance() }

    val startDatePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                viewModel.updatePlanState(startDate = calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(uiState.startDate) {
        val c = Calendar.getInstance().apply { timeInMillis = uiState.startDate }
        startDatePickerDialog.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    val endDatePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                viewModel.updatePlanState(endDate = calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(uiState.endDate) {
        val c = Calendar.getInstance().apply { timeInMillis = uiState.endDate }
        endDatePickerDialog.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    var isDeleteExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.dialog_title_delete_plan)) },
            text = { Text(stringResource(R.string.dialog_msg_delete_plan)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetDate = viewModel.planUiState.startDate
                        viewModel.deletePlan {
                            showDeleteConfirmation = false
                            onDeleteClick(targetDate)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.fail)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = AppTheme.colors.surface,
            titleContentColor = AppTheme.colors.textPrimary,
            textContentColor = AppTheme.colors.textSecondary
        )
    }

    if (showDeleteExpensesConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteExpensesConfirmation = false },
            title = { Text(stringResource(R.string.dialog_title_clear_expenses)) },
            text = { Text(stringResource(R.string.dialog_msg_clear_expenses)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearPlanExpenses()
                        showDeleteExpensesConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.fail)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExpensesConfirmation = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = AppTheme.colors.surface,
            titleContentColor = AppTheme.colors.textPrimary,
            textContentColor = AppTheme.colors.textSecondary
        )
    }

    LaunchedEffect(planId, initialStartDate, initialEndDate) {
        if (planId != null && planId != -1) {
            viewModel.loadPlan(planId)
        } else {
            viewModel.initDates(initialStartDate, initialEndDate)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (planId == null) stringResource(R.string.title_create_plan)
                        else stringResource(R.string.title_edit_plan),
                        color = AppTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        GlassIconButton(
                            onClick = { debounce(onBackClick) },
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                )
            )
        },
        snackbarHost = {
            if (uiState.errorMessageId != null) {
                Snackbar(
                    containerColor = AppTheme.colors.fail,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(uiState.errorMessageId!!))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 計畫名稱
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    GlassTextField(
                        value = uiState.planName,
                        onValueChange = { viewModel.updatePlanState(planName = it) },
                        placeholder = stringResource(R.string.hint_plan_name),
                        label = stringResource(R.string.label_plan_name)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CommonGlassDateCard(
                    label = stringResource(R.string.label_start_date),
                    date = uiState.startDate,
                    onClick = { debounce { startDatePickerDialog.show() } },
                    modifier = Modifier.weight(1f)
                )
                CommonGlassDateCard(
                    label = stringResource(R.string.label_end_date),
                    date = uiState.endDate,
                    onClick = { debounce { endDatePickerDialog.show() } },
                    modifier = Modifier.weight(1f)
                )
            }

            // [修改] 幣別選擇器
            // 顯示條件：
            // 1. 必須是新增計畫模式 (planId == null 或 -1)
            // 2. [新增] 必須是「第一次設定幣別」(isFirstTimeCurrencySetup 為 true)
            // 這樣之後的新增計畫就不會顯示了
            if ((planId == null || planId == -1 || uiState.id == 0) && isFirstTimeCurrencySetup) {
                PlanCurrencySelector(
                    selectedCurrency = uiState.selectedCurrency,
                    supportedCurrencies = supportedCurrencies,
                    onCurrencySelected = { viewModel.updatePlanState(selectedCurrency = it) }
                )
            }

            // 預算設定區塊
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.label_budget_target), fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
                    GlassTextField(
                        value = uiState.totalBudget,
                        onValueChange = { viewModel.updatePlanState(totalBudget = it) },
                        placeholder = stringResource(R.string.hint_total_budget),
                        label = stringResource(R.string.label_total_budget),
                        isNumber = true
                    )
                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)
                    GlassTextField(
                        value = uiState.targetSavings,
                        onValueChange = { viewModel.updatePlanState(targetSavings = it) },
                        placeholder = stringResource(R.string.hint_target_savings),
                        label = stringResource(R.string.label_target_savings),
                        isNumber = true
                    )
                }
            }

            val diff = uiState.endDate - uiState.startDate
            val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            val safeDays = if (days > 0) days else 1
            val budget = uiState.totalBudget.toIntOrNull() ?: 0
            val savings = uiState.targetSavings.toIntOrNull() ?: 0
            val dailyAvailable = (budget - savings) / safeDays

            // 每日可用金額
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.label_daily_available), fontSize = 14.sp, color = AppTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                stringResource(R.string.format_currency, dailyAvailable),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.success
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // [優化] 顯示選擇的幣別
                            Text(
                                uiState.selectedCurrency,
                                fontSize = 14.sp,
                                color = AppTheme.colors.success.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = AppTheme.colors.success.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (planId != null && planId != -1) {
                // 進階選項
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { debounce { isDeleteExpanded = !isDeleteExpanded } }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.label_advanced_options), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                            Icon(
                                imageVector = if (isDeleteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary
                            )
                        }

                        AnimatedVisibility(visible = isDeleteExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                GlassActionTextButton(
                                    text = stringResource(R.string.btn_clear_expenses),
                                    icon = Icons.Default.Refresh,
                                    onClick = { debounce { showDeleteExpensesConfirmation = true } },
                                    isDanger = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                GlassActionTextButton(
                                    text = stringResource(R.string.btn_delete_plan),
                                    icon = Icons.Default.Delete,
                                    onClick = { debounce { showDeleteConfirmation = true } },
                                    isDanger = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AuroraPrimaryButton(
                text = if (planId == null) stringResource(R.string.btn_start_plan) else stringResource(R.string.btn_save_changes),
                onClick = {
                    debounce {
                        viewModel.savePlan(onSuccess = { savedId ->
                            onSaveClick(savedId)
                        })
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 幣別選擇器 UI (保持不變)
// [修改] 幣別選擇器 UI
@Composable
fun PlanCurrencySelector(
    selectedCurrency: String,
    supportedCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            // [提取] 說明文字
            Text(
                text = stringResource(R.string.desc_currency_impact),
                fontSize = 12.sp,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = AppTheme.colors.accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedCurrency,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(AppTheme.colors.surface)
                ) {
                    supportedCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = currency,
                                    color = if (currency == selectedCurrency) AppTheme.colors.accent else AppTheme.colors.textPrimary
                                )
                            },
                            onClick = {
                                onCurrencySelected(currency)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommonGlassDateCard(label: String, date: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dateFormat = stringResource(R.string.format_date_short)
    val formatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }

    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatter.format(Date(date)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
        }
    }
}