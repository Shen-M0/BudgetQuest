package com.example.budgetquest.ui.plan

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// [美術] 定義玻璃筆刷 (保持全域一致)
@Composable
private fun getGlassBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AppTheme.colors.surface.copy(alpha = 0.65f),
            AppTheme.colors.surface.copy(alpha = 0.35f)
        )
    )
}

@Composable
private fun getBorderBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.textPrimary.copy(alpha = 0.25f),
            AppTheme.colors.textPrimary.copy(alpha = 0.10f)
        )
    )
}

// [美術] 1. 玻璃圓形按鈕容器 (用於 TopAppBar)
@Composable
fun GlassIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// [美術] 2. 極光漸層主要按鈕 (用於 Save/Start)
@Composable
fun AuroraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 建立一個從 Accent 到稍亮顏色的水平漸層
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            AppTheme.colors.accent,
            AppTheme.colors.accent.copy(alpha = 0.7f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = AppTheme.colors.accent, spotColor = AppTheme.colors.accent) // 光暈陰影
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// [美術] 3. 警示玻璃按鈕 (用於 Delete/Clear)
@Composable
fun GlassDangerButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassBrush = getGlassBrush()
    // 紅色邊框
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.fail.copy(alpha = 0.5f),
            AppTheme.colors.fail.copy(alpha = 0.2f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(glassBrush) // 玻璃背景
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp)) // 紅色邊框
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.fail,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = AppTheme.colors.fail,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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

    // [美術] 取得筆刷
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

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
                        // [美術] 優化：使用玻璃圓鈕
                        // 順序：padding -> clip -> clickable (確保水波紋為圓形)
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(CircleShape)
                                .clickable { debounce(onBackClick) }
                        ) {
                            GlassIconContainer(modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                            }
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
            JapaneseInputCard(label = stringResource(R.string.label_plan_name)) {
                JapaneseTextField(
                    value = uiState.planName,
                    onValueChange = { viewModel.updatePlanState(planName = it) },
                    placeholder = stringResource(R.string.hint_plan_name)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                JapaneseDateCard(
                    label = stringResource(R.string.label_start_date),
                    date = uiState.startDate,
                    onClick = { debounce { startDatePickerDialog.show() } },
                    modifier = Modifier.weight(1f)
                )
                JapaneseDateCard(
                    label = stringResource(R.string.label_end_date),
                    date = uiState.endDate,
                    onClick = { debounce { endDatePickerDialog.show() } },
                    modifier = Modifier.weight(1f)
                )
            }

            JapaneseInputCard(label = stringResource(R.string.label_budget_target)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    JapaneseTextField(
                        value = uiState.totalBudget,
                        onValueChange = { viewModel.updatePlanState(totalBudget = it) },
                        placeholder = stringResource(R.string.hint_total_budget),
                        label = stringResource(R.string.label_total_budget),
                        isNumber = true
                    )
                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)
                    JapaneseTextField(
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassBrush)
                    .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(glassBrush)
                        .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
                        .clickable { debounce { isDeleteExpanded = !isDeleteExpanded } }
                        .padding(20.dp)
                ) {
                    Column {
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
                                // [美術] 優化：使用警示玻璃按鈕 (清空消費)
                                GlassDangerButton(
                                    text = stringResource(R.string.btn_clear_expenses),
                                    icon = Icons.Default.Refresh,
                                    onClick = { debounce { showDeleteExpensesConfirmation = true } }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // [美術] 優化：使用警示玻璃按鈕 (刪除計畫)
                                GlassDangerButton(
                                    text = stringResource(R.string.btn_delete_plan),
                                    icon = Icons.Default.Delete,
                                    onClick = { debounce { showDeleteConfirmation = true } }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // [美術] 優化：使用極光漸層主要按鈕 (開始/儲存)
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

// --- 輔助元件 ---

@Composable
fun JapaneseInputCard(label: String, content: @Composable () -> Unit) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

@Composable
fun JapaneseDateCard(label: String, date: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dateFormat = stringResource(R.string.format_date_short)
    val formatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(vertical = 24.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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