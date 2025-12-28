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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
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
    onDeleteClick: () -> Unit = { onBackClick() },
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.planUiState
    val context = LocalContext.current

    // [優化] 防手震
    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    // [優化] 快取 Calendar 與 DatePicker，避免重複建立
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
    // 當日期改變時，更新 Dialog 狀態
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
            title = { Text("刪除計畫") },
            text = { Text("確定要刪除此計畫嗎？刪除後無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // [優化] 刪除操作防手震
                        debounce {
                            viewModel.deletePlan(onSuccess = onDeleteClick)
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.fail)
                ) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") }
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
                title = { Text(if (planId == null) "建立計畫" else "編輯計畫", color = AppTheme.colors.textPrimary) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { debounce(onBackClick) }) { // [優化] 防手震
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        },
        snackbarHost = {
            if (uiState.errorMessage != null) {
                Snackbar(
                    containerColor = AppTheme.colors.fail,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp)
                ) { Text(uiState.errorMessage!!) }
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
            JapaneseInputCard(label = "計畫名稱") {
                JapaneseTextField(
                    value = uiState.planName,
                    onValueChange = { viewModel.updatePlanState(planName = it) },
                    placeholder = "我的記帳計畫"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                JapaneseDateCard(
                    label = "開始日期",
                    date = uiState.startDate,
                    onClick = { debounce { startDatePickerDialog.show() } }, // [優化] 防手震
                    modifier = Modifier.weight(1f)
                )
                JapaneseDateCard(
                    label = "結束日期",
                    date = uiState.endDate,
                    onClick = { debounce { endDatePickerDialog.show() } }, // [優化] 防手震
                    modifier = Modifier.weight(1f)
                )
            }

            JapaneseInputCard(label = "預算與目標") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    JapaneseTextField(
                        value = uiState.totalBudget,
                        onValueChange = { viewModel.updatePlanState(totalBudget = it) },
                        placeholder = "總預算",
                        label = "總預算",
                        isNumber = true
                    )
                    HorizontalDivider(color = AppTheme.colors.divider)
                    JapaneseTextField(
                        value = uiState.targetSavings,
                        onValueChange = { viewModel.updatePlanState(targetSavings = it) },
                        placeholder = "目標存錢",
                        label = "目標存錢",
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
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("每日可用", fontSize = 14.sp, color = AppTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$ $dailyAvailable",
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
                        .clickable { debounce { isDeleteExpanded = !isDeleteExpanded } }, // [優化] 防手震
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
                            Text("進階選項", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                            Icon(
                                imageVector = if (isDeleteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary
                            )
                        }

                        AnimatedVisibility(visible = isDeleteExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Button(
                                    onClick = { debounce { showDeleteConfirmation = true } }, // [優化] 防手震
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
                                    Text("刪除此計畫")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // [優化] 儲存按鈕防手震
                    debounce {
                        viewModel.savePlan(onSuccess = { savedId ->
                            onSaveClick(savedId)
                        })
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent)
            ) {
                Text(if (planId == null) "開始計畫" else "儲存變更", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- 輔助元件 (使用 AppTheme) ---

@Composable
fun JapaneseInputCard(label: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface), // [修正]
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
    val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface), // [修正]
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatter.format(Date(date)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary) // [修正]
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
                color = AppTheme.colors.textPrimary // [修正]
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent, // 透明背景
                unfocusedContainerColor = Color.Transparent
            ),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            singleLine = true
        )
    }
}