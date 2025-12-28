package com.example.budgetquest.ui.transaction

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    initialDate: Long,
    expenseId: Long,
    onBackClick: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    viewModel: TransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.visibleCategories.collectAsState()
    val tags by viewModel.visibleTags.collectAsState()

    var showCategoryManager by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }

    // [優化] 防手震計時器
    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    if (showCategoryManager) {
        val allCategories by viewModel.allCategories.collectAsState()
        CategoryManagerDialog(
            categories = allCategories,
            onDismiss = { showCategoryManager = false },
            onAddCategory = viewModel::addCategory,
            onToggleVisibility = viewModel::toggleCategoryVisibility,
            onDelete = viewModel::deleteCategory
        )
    }

    if (showTagManager) {
        val allTags by viewModel.allTags.collectAsState()
        TagManagerDialog(
            tags = allTags,
            onDismiss = { showTagManager = false },
            onAddTag = viewModel::addTag,
            onToggleVisibility = viewModel::toggleTagVisibility,
            onDelete = viewModel::deleteTag
        )
    }

    val context = LocalContext.current

    // [優化] 使用 remember 快取 Calendar 與 Format，避免重繪時重複建立物件
    val calendar = remember { Calendar.getInstance() }
    // 每次 uiState.date 改變時，更新 calendar
    LaunchedEffect(uiState.date) {
        calendar.timeInMillis = uiState.date
    }

    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                viewModel.setDate(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    // 當日期改變時，更新 Dialog 的選中日期
    LaunchedEffect(uiState.date) {
        datePickerDialog.updateDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    LaunchedEffect(expenseId, initialDate) {
        if (expenseId != -1L) {
            viewModel.loadExpense(expenseId)
        } else {
            viewModel.reset()
            if (initialDate > 0) viewModel.setDate(initialDate)
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == -1L) "記一筆" else "編輯消費", color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
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
            modifier = Modifier.padding(innerPadding).padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 金額與日期卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 日期按鈕
                    Row(
                        modifier = Modifier.clickable { debounce { datePickerDialog.show() } }, // [優化] 防手震
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateFormatter.format(Date(uiState.date)), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                    }

                    // 金額輸入
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$", fontSize = 24.sp, color = AppTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        JapaneseTransparentInput(
                            value = uiState.amount,
                            onValueChange = { viewModel.updateAmount(it) },
                            placeholder = "0",
                            isNumber = true,
                            fontSize = 32
                        )
                    }
                }
            }

            // 2. 分類與備註卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("分類", fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    // 分類列表
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [優化] 加入 key，提升列表渲染效能
                        items(categories, key = { it.id }) { cat ->
                            JapaneseCompactChip(
                                label = cat.name,
                                selected = uiState.category == cat.name,
                                icon = getIconByKey(cat.iconKey)
                            ) { viewModel.updateCategory(cat.name) }
                        }
                        item {
                            EditButton { debounce { showCategoryManager = true } } // [優化] 防手震
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                    Text("備註", fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    // 常用標籤列表
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [優化] 加入 key
                        items(tags, key = { it.id }) { tag ->
                            JapaneseCompactChip(tag.name, uiState.note == tag.name) { viewModel.updateNote(tag.name) }
                        }
                        item {
                            EditButton { debounce { showTagManager = true } } // [優化] 防手震
                        }
                    }

                    // 備註輸入
                    JapaneseTransparentInput(
                        value = uiState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        placeholder = "輸入項目名稱...",
                        fontSize = 16
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 儲存按鈕
            Button(
                onClick = {
                    // [優化] 儲存按鈕防手震
                    debounce {
                        viewModel.saveExpense {
                            onSaveSuccess(uiState.date)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent)
            ) {
                Text("儲存", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 透明背景輸入框 (支援深色模式)
@Composable
fun JapaneseTransparentInput(value: String, onValueChange: (String) -> Unit, placeholder: String, isNumber: Boolean = false, fontSize: Int) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = fontSize.sp,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.5f) // [修正]
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = fontSize.sp,
                color = AppTheme.colors.textPrimary, // [修正]
                lineHeight = fontSize.sp,
                fontWeight = if (isNumber) FontWeight.Bold else FontWeight.Normal
            ),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            singleLine = true,
            cursorBrush = SolidColor(AppTheme.colors.accent), // [修正]
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 復用 SummaryScreen 的 Chip (支援深色模式)
@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        color = if (selected) AppTheme.colors.accent else AppTheme.colors.background, // [修正] 未選中時用背景色
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

// 日系風格的編輯小按鈕 (支援深色模式)
@Composable
fun EditButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = AppTheme.colors.background, // [修正]
        modifier = Modifier.size(32.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}