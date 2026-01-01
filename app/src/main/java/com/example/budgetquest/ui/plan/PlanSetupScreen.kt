package com.example.budgetquest.ui.plan

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable // 雖然 GlassCard 封裝了，但部分邏輯可能還需要
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // 用於 GlassCard 內部排版
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton // 用於 TopBar 的標準 IconButton (如果需要)
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

// [移除] 所有本地定義的樣式與元件

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
    val context = LocalContext.current

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
                        // [優化] 使用 GlassIconButton (圓形水波紋)
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
            // [優化] 使用 GlassCard 和 GlassTextField (輸入計畫名稱)
            // GlassCard 預設帶有邊框 (Border)，符合您的需求
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
                // [優化] 日期選擇器使用 CommonGlassDateCard
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

            // [優化] 預算設定區塊
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

            // [優化] 每日可用金額
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.label_daily_available), fontSize = 14.sp, color = AppTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.format_currency, dailyAvailable),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.success
                        )
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
                // [優化] 進階選項 (開關型按鈕)
                // 使用 GlassCard 並傳入 onClick，這樣就會有帶邊框的點擊效果，符合「深色模式切換」的風格
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
                                // [優化] 使用 GlassActionTextButton (紅色危險按鈕)
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

            // [優化] 使用 AuroraPrimaryButton
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

// 為了方便重用，這裡定義一個小型的 DateCard，使用 GlassCard 封裝
@Composable
fun CommonGlassDateCard(label: String, date: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dateFormat = stringResource(R.string.format_date_short)
    val formatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }

    // 使用 GlassCard 封裝，自動獲得邊框與點擊效果
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