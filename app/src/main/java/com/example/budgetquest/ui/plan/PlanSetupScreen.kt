package com.example.budgetquest.ui.plan

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Refresh // [新增] 用於清空帳目的圖示


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    planId: Int?,
    initialStartDate: Long = -1L,
    initialEndDate: Long = -1L,
    showBackButton: Boolean = true,
    onBackClick: () -> Unit,
    onSaveClick: (Int) -> Unit,
    // [修改 1] onDeleteClick 改為接收 Long (日期)
    onDeleteClick: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {

    // [新增] 清空帳目的確認框狀態
    var showDeleteExpensesConfirmation by remember { mutableStateOf(false) }
    // [修正 1] 補上這行狀態宣告，紅色錯誤就會消失
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
    // 刪除計畫的確認框狀態
    var showDeleteConfirmation by remember { mutableStateOf(false) }


    // [原本的] 刪除計畫 Dialog
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

    // [新增] 清空帳目 Dialog
    if (showDeleteExpensesConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteExpensesConfirmation = false },
            // 這裡建議在 strings.xml 新增: dialog_title_clear_expenses
            title = { Text(stringResource(R.string.dialog_title_clear_expenses)) },
            // 這裡建議在 strings.xml 新增: dialog_msg_clear_expenses
            text = { Text(stringResource(R.string.dialog_msg_clear_expenses)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // [注意] 您需要在 PlanViewModel 中實作 clearPlanExpenses 函式
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
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                // [提取] 標題 (根據狀態切換)
                title = {
                    Text(
                        if (planId == null) stringResource(R.string.title_create_plan)
                        else stringResource(R.string.title_edit_plan),
                        color = AppTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { debounce(onBackClick) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        },
        snackbarHost = {
            if (uiState.errorMessageId != null) { // 改為 errorMessageId
                Snackbar(
                    containerColor = AppTheme.colors.fail,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 使用 stringResource 來顯示錯誤訊息
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
            // [提取] 計畫名稱標籤
            JapaneseInputCard(label = stringResource(R.string.label_plan_name)) {
                JapaneseTextField(
                    value = uiState.planName,
                    onValueChange = { viewModel.updatePlanState(planName = it) },
                    // [提取] 計畫名稱提示
                    placeholder = stringResource(R.string.hint_plan_name)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // [提取] 開始日期
                JapaneseDateCard(
                    label = stringResource(R.string.label_start_date),
                    date = uiState.startDate,
                    onClick = { debounce { startDatePickerDialog.show() } },
                    modifier = Modifier.weight(1f)
                )
                // [提取] 結束日期
                JapaneseDateCard(
                    label = stringResource(R.string.label_end_date),
                    date = uiState.endDate,
                    onClick = { debounce { endDatePickerDialog.show() } },
                    modifier = Modifier.weight(1f)
                )
            }

            // [提取] 預算與目標標籤
            JapaneseInputCard(label = stringResource(R.string.label_budget_target)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    JapaneseTextField(
                        value = uiState.totalBudget,
                        onValueChange = { viewModel.updatePlanState(totalBudget = it) },
                        // [提取] 總預算
                        placeholder = stringResource(R.string.hint_total_budget),
                        label = stringResource(R.string.label_total_budget),
                        isNumber = true
                    )
                    HorizontalDivider(color = AppTheme.colors.divider)
                    JapaneseTextField(
                        value = uiState.targetSavings,
                        onValueChange = { viewModel.updatePlanState(targetSavings = it) },
                        // [提取] 目標存錢
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // [提取] 每日可用
                        Text(stringResource(R.string.label_daily_available), fontSize = 14.sp, color = AppTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        // [提取] 金額格式
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { debounce { isDeleteExpanded = !isDeleteExpanded } },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // [提取] 進階選項
                            Text(stringResource(R.string.label_advanced_options), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                            Icon(
                                imageVector = if (isDeleteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary
                            )
                        }

                        AnimatedVisibility(visible = isDeleteExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                // [新增] 清空帳目按鈕 (放在上方，作為次級破壞操作)
                                Button(
                                    onClick = { debounce { showDeleteExpensesConfirmation = true } },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.fail.copy(alpha = 0.1f),
                                        contentColor = AppTheme.colors.fail
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    // 使用 Refresh 或 DeleteSweep 圖示代表「重置/清空」
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // 建議新增字串資源: btn_clear_expenses
                                    Text(stringResource(R.string.btn_clear_expenses))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // [原本的] 刪除計畫按鈕 (放在最底部，作為最終破壞操作)
                                Button(
                                    onClick = { debounce { showDeleteConfirmation = true } },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.fail.copy(alpha = 0.1f),
                                        contentColor = AppTheme.colors.fail
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_delete_plan))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    debounce {
                        viewModel.savePlan(onSuccess = { savedId ->
                            onSaveClick(savedId)
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent)
            ) {
                // [提取] 底部按鈕文字 (開始/儲存)
                Text(
                    if (planId == null) stringResource(R.string.btn_start_plan)
                    else stringResource(R.string.btn_save_changes),
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- 輔助元件 (JapaneseInputCard, JapaneseDateCard, JapaneseTextField) ---

@Composable
fun JapaneseInputCard(label: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

@Composable
fun JapaneseDateCard(label: String, date: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // [提取] 日期格式
    val dateFormat = stringResource(R.string.format_date_short)
    val formatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatter.format(Date(date)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
        }
    }
}

@Composable
fun JapaneseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String? = null,
    isNumber: Boolean = false
) {
    Column {
        if (label != null) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                color = AppTheme.colors.textPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            singleLine = true
        )
    }
}