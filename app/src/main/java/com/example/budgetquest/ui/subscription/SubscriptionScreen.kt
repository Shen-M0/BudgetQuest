package com.example.budgetquest.ui.subscription

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.data.RecurringExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.*
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.SubTagManagerDialog
import com.example.budgetquest.ui.transaction.EditButton
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.theme.AppTheme // 引入主題

val periods = listOf("MONTH" to "每月", "WEEK" to "每週", "DAY" to "每天", "CUSTOM" to "自訂")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    planId: Int,
    startDate: Long,
    endDate: Long,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
    val list by viewModel.recurringList.collectAsState()
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

    LaunchedEffect(planId, startDate, endDate) {
        viewModel.initialize(planId, startDate, endDate)
    }

    // [優化] 快取 Calendar 與 DateFormat
    val startCalendar = remember { Calendar.getInstance() }
    val endCalendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    // [優化] 快取 DatePicker
    val startDatePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                startCalendar.set(y, m, d)
                viewModel.updateUiState(startDate = startCalendar.timeInMillis)
            },
            startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(uiState.startDate) {
        startCalendar.timeInMillis = uiState.startDate
        startDatePickerDialog.updateDate(startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH))
    }

    val endDatePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                endCalendar.set(y, m, d)
                viewModel.updateUiState(endDate = endCalendar.timeInMillis)
            },
            endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(uiState.endDate) {
        val time = uiState.endDate ?: System.currentTimeMillis()
        endCalendar.timeInMillis = time
        endDatePickerDialog.updateDate(endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH))
    }

    val categories by viewModel.visibleCategories.collectAsState()
    val subTags by viewModel.visibleSubTags.collectAsState()
    var showCategoryManager by remember { mutableStateOf(false) }
    var showSubTagManager by remember { mutableStateOf(false) }

    if (showCategoryManager) {
        val allCategories by viewModel.allCategories.collectAsState()
        CategoryManagerDialog(allCategories, { showCategoryManager = false }, viewModel::addCategory, viewModel::toggleCategoryVisibility, viewModel::deleteCategory)
    }
    if (showSubTagManager) {
        val allSubTags by viewModel.allSubTags.collectAsState()
        SubTagManagerDialog(allSubTags, { showSubTagManager = false }, viewModel::addSubTag, viewModel::toggleSubTagVisibility, viewModel::deleteSubTag)
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("固定收支", color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { debounce(onBackClick) }) { // [優化] 防手震
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // [優化] 使用 remember 的 formatter
                        JapaneseDateButton("開始", dateFormatter.format(Date(uiState.startDate))) {
                            debounce { startDatePickerDialog.show() } // [優化] 防手震
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            JapaneseTextField(value = uiState.amount, onValueChange = { viewModel.updateUiState(amount = it) }, label = "金額", isNumber = true)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JapaneseDateButton("結束", if (uiState.endDate != null) dateFormatter.format(Date(uiState.endDate!!)) else "無限期") {
                            debounce { endDatePickerDialog.show() } // [優化] 防手震
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(periods) { (key, label) ->
                                JapaneseCompactChip(label, uiState.frequency == key) { viewModel.updateUiState(frequency = key) }
                            }
                        }
                    }
                    if (uiState.frequency == "CUSTOM") {
                        JapaneseTextField(value = uiState.customDays, onValueChange = { viewModel.updateUiState(customDays = it) }, label = "間隔天數", isNumber = true)
                    }

                    HorizontalDivider(color = AppTheme.colors.divider)

                    Text("分類與名稱", fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [優化] 加上 key
                        items(categories, key = { it.id }) { category ->
                            JapaneseCompactChip(
                                label = category.name,
                                selected = uiState.category == category.name,
                                icon = getIconByKey(category.iconKey)
                            ) { viewModel.updateUiState(category = category.name) }
                        }
                        item { EditButton { debounce { showCategoryManager = true } } } // [優化] 防手震
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [優化] 加上 key
                        items(subTags, key = { it.id }) { tag ->
                            JapaneseCompactChip(label = tag.name, selected = uiState.note == tag.name) {
                                viewModel.updateUiState(note = tag.name)
                            }
                        }
                        item { EditButton { debounce { showSubTagManager = true } } } // [優化] 防手震
                    }

                    JapaneseTextField(value = uiState.note, onValueChange = { viewModel.updateUiState(note = it) }, label = "名稱 (或點擊上方選項)")

                    Button(
                        onClick = {
                            debounce { // [優化] 儲存防手震
                                viewModel.addSubscription { onSaveSuccess() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("加入清單", color = Color.White) }
                }
            }

            Text("訂閱中:", color = AppTheme.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // [優化] 加上 key (假設 RecurringExpenseEntity 有 id)
                items(list, key = { it.id }) { item ->
                    JapaneseSubscriptionItem(
                        item,
                        onDelete = {
                            debounce { viewModel.deleteSubscription(item) } // [優化] 刪除防手震
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JapaneseTextField(value: String, onValueChange: (String) -> Unit, label: String, isNumber: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = AppTheme.colors.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = AppTheme.colors.accent,
            unfocusedContainerColor = AppTheme.colors.background,
            focusedContainerColor = AppTheme.colors.background,
            // [新增] 確保輸入文字顏色正確
            focusedTextColor = AppTheme.colors.textPrimary,
            unfocusedTextColor = AppTheme.colors.textPrimary
        ),
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        singleLine = true
    )
}

@Composable
fun JapaneseDateButton(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
        Text(value, fontSize = 14.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Surface(
        // [修改] 選中時用 accent，未選中用 background
        color = if (selected) AppTheme.colors.accent else AppTheme.colors.background,
        contentColor = if (selected) Color.White else AppTheme.colors.textSecondary,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
fun JapaneseSubscriptionItem(item: RecurringExpenseEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AppTheme.colors.surface).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = AppTheme.colors.background, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = AppTheme.colors.accent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.note, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
                Text("$${item.amount} / ${item.frequency}", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            }
        }
        IconButton(onClick = onDelete) {
            // [修改] 刪除圖示顏色微調
            Icon(Icons.Default.Delete, null, tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f))
        }
    }
}