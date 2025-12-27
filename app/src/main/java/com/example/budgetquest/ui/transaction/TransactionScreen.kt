package com.example.budgetquest.ui.transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.*
import com.example.budgetquest.ui.common.getIconByKey
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
// 日系配色
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    initialDate: Long,
    expenseId: Long,
    navigateBack: () -> Unit,
    viewModel: TransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // [新增] 觀察資料庫的分類與標籤
    val categories by viewModel.visibleCategories.collectAsState()
    val tags by viewModel.visibleTags.collectAsState()

    // [新增] 管理介面狀態
    var showCategoryManager by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }

    // [新增] 如果開啟管理介面，顯示 Dialog
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
    val calendar = Calendar.getInstance().apply { timeInMillis = uiState.date }
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> calendar.set(y, m, d); viewModel.updateUiState(date = calendar.timeInMillis) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    LaunchedEffect(expenseId) {
        if (expenseId != -1L) viewModel.loadExpense(expenseId) else viewModel.updateUiState(date = initialDate)
    }

    Scaffold(
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == -1L) "記一筆" else "編輯消費", color = JapaneseTextPrimary, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = navigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = JapaneseTextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 金額與日期卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 日期按鈕
                    Row(
                        modifier = Modifier.clickable { datePickerDialog.show() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = JapaneseTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateFormatter.format(Date(uiState.date)), color = JapaneseTextSecondary, fontSize = 14.sp)
                    }

                    // 金額輸入 (特大字體)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$", fontSize = 24.sp, color = JapaneseTextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        JapaneseTransparentInput(
                            value = uiState.amount,
                            onValueChange = { viewModel.updateUiState(amount = it) },
                            placeholder = "0",
                            isNumber = true,
                            fontSize = 32
                        )
                    }
                }
            }

            // 2. 分類與備註卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("分類", fontSize = 12.sp, color = JapaneseTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 資料庫的分類
                        items(categories) { cat ->
                            JapaneseCompactChip(
                                label = cat.name,
                                selected = uiState.category == cat.name,
                                icon = getIconByKey(cat.iconKey)
                            ) { viewModel.updateUiState(category = cat.name) }
                        }

                        // [新增] 末端的編輯按鈕
                        item {
                            EditButton { showCategoryManager = true }
                        }
                    }

                    HorizontalDivider(color = JapaneseBg, thickness = 1.dp)

                    Text("備註", fontSize = 12.sp, color = JapaneseTextSecondary)
                    // 常用標籤
                    // [修改] 標籤列表
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            JapaneseCompactChip(tag.name, uiState.note == tag.name) { viewModel.updateUiState(note = tag.name) }
                        }
                        // [新增] 末端的編輯按鈕
                        item {
                            EditButton { showTagManager = true }
                        }
                    }
                    // 備註輸入
                    JapaneseTransparentInput(
                        value = uiState.note,
                        onValueChange = { viewModel.updateUiState(note = it) },
                        placeholder = "輸入項目名稱...",
                        fontSize = 16
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 儲存按鈕
            Button(
                onClick = { viewModel.saveExpense(); navigateBack() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JapaneseAccent),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.amount.isNotEmpty() && uiState.note.isNotEmpty()
            ) {
                Text("儲存紀錄", fontSize = 16.sp)
            }
        }
    }
}

// 透明背景輸入框
@Composable
fun JapaneseTransparentInput(value: String, onValueChange: (String) -> Unit, placeholder: String, isNumber: Boolean = false, fontSize: Int) {
    // 使用 Box 來確保垂直置中
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = fontSize.sp,
                color = Color.LightGray
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalTextStyle.current.copy(
                fontSize = fontSize.sp,
                color = JapaneseTextPrimary,
                // [關鍵] 設定行高，避免游標跑版
                lineHeight = fontSize.sp
            ),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(JapaneseAccent), // 設定游標顏色
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 復用 SummaryScreen 的 Chip
@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        color = if (selected) JapaneseAccent else JapaneseBg,
        contentColor = if (selected) Color.White else JapaneseTextSecondary,
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

// [新增] 一個日系風格的編輯小按鈕
@Composable
fun EditButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFECEFF1), // 淺灰
        modifier = Modifier.size(32.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
        }
    }
}