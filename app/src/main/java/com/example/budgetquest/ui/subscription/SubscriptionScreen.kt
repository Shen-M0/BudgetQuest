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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star // 裝飾用
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
import com.example.budgetquest.ui.transaction.categories
import java.text.SimpleDateFormat
import java.util.*
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.SubTagManagerDialog
import com.example.budgetquest.ui.transaction.EditButton
import com.example.budgetquest.ui.common.getIconByKey


private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

val subTags = listOf("Netflix", "Spotify", "YouTube Music", "Apple Music", "Disney+", "iCloud", "健身房", "電話費")
val periods = listOf("MONTH" to "每月", "WEEK" to "每週", "DAY" to "每天", "CUSTOM" to "自訂")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBackClick: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
    val list by viewModel.recurringList.collectAsState()
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    // DatePickers (保持原本邏輯，這裡省略重複宣告，請保留原本的 Picker 邏輯)
    val startCalendar = Calendar.getInstance().apply { timeInMillis = uiState.startDate }
    val startDatePickerDialog = DatePickerDialog(context, { _, y, m, d -> startCalendar.set(y, m, d); viewModel.updateUiState(startDate = startCalendar.timeInMillis) }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH))
    val endCalendar = Calendar.getInstance().apply { timeInMillis = uiState.endDate ?: System.currentTimeMillis() }
    val endDatePickerDialog = DatePickerDialog(context, { _, y, m, d -> endCalendar.set(y, m, d); viewModel.updateUiState(endDate = endCalendar.timeInMillis) }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH))

    // [新增] 觀察資料
    val categories by viewModel.visibleCategories.collectAsState()
    val subTags by viewModel.visibleSubTags.collectAsState()

    // [新增] Dialog 狀態
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
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = { Text("固定收支", color = JapaneseTextPrimary, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = JapaneseTextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // [改] 輸入區塊：卡片化
            Card(
                colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // 1. 日期與金額 (第一排)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        JapaneseDateButton("開始", SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(uiState.startDate))) { startDatePickerDialog.show() }
                        // 金額輸入框 (放在右邊)
                        Box(modifier = Modifier.weight(1f)) {
                            JapaneseTextField(value = uiState.amount, onValueChange = { viewModel.updateUiState(amount = it) }, label = "金額", isNumber = true)
                        }
                    }

                    // 2. 結束日期與週期 (第二排)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JapaneseDateButton("結束", if (uiState.endDate != null) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(uiState.endDate)) else "無限期") { endDatePickerDialog.show() }
                        Spacer(modifier = Modifier.width(16.dp))
                        // 週期選擇 (改為緊湊的 Scrollable Row)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(periods) { (key, label) ->
                                JapaneseCompactChip(label, uiState.frequency == key) { viewModel.updateUiState(frequency = key) }
                            }
                        }
                    }
                    if (uiState.frequency == "CUSTOM") {
                        JapaneseTextField(value = uiState.customDays, onValueChange = { viewModel.updateUiState(customDays = it) }, label = "間隔天數", isNumber = true)
                    }

                    HorizontalDivider(color = Color(0xFFF7F9FC))

                    // 3. 分類與服務名稱 (整合區塊)
                    Text("分類與名稱", fontSize = 12.sp, color = Color(0xFF90A4AE))

                    // [修改] 分類列表 (讀取 DB + 編輯鈕)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            JapaneseCompactChip(
                                label = category.name,
                                selected = uiState.category == category.name,
                                icon = getIconByKey(category.iconKey)
                            ) { viewModel.updateUiState(category = category.name) }
                        }
                        item { EditButton { showCategoryManager = true } }
                    }

                    // [修改] 常用服務快速選擇 (讀取 SubTags + 編輯鈕)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subTags) { tag ->
                            JapaneseCompactChip(label = tag.name, selected = uiState.note == tag.name) {
                                viewModel.updateUiState(note = tag.name)
                            }
                        }
                        item { EditButton { showSubTagManager = true } }
                    }

                    // 名稱輸入框 (手動輸入用)
                    JapaneseTextField(value = uiState.note, onValueChange = { viewModel.updateUiState(note = it) }, label = "名稱 (或點擊上方選項)")

                    Button(
                        onClick = { viewModel.addSubscription() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78909C)), // JapaneseAccent
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("加入清單") }
                }
            }

            Text("訂閱中:", color = JapaneseTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(list) { item ->
                    JapaneseSubscriptionItem(item, onDelete = { viewModel.deleteSubscription(item) })
                }
            }
        }
    }
}

// [新增] 日系元件 Helper
@Composable
fun JapaneseTextField(value: String, onValueChange: (String) -> Unit, label: String, isNumber: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = JapaneseTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = JapaneseAccent,
            unfocusedContainerColor = JapaneseBg, // 輸入框背景微灰
            focusedContainerColor = JapaneseBg
        ),
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        singleLine = true
    )
}

@Composable
fun JapaneseDateButton(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(label, fontSize = 12.sp, color = JapaneseTextSecondary)
        Text(value, fontSize = 14.sp, color = JapaneseTextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFF78909C) else Color(0xFFF7F9FC),
        contentColor = if (selected) Color.White else Color(0xFF90A4AE),
        shape = RoundedCornerShape(8.dp), //稍微方一點，比較像標籤
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(JapaneseSurface).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左側裝飾圖示
            Surface(shape = CircleShape, color = JapaneseBg, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = JapaneseAccent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.note, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = JapaneseTextPrimary)
                Text("$${item.amount} / ${item.frequency}", fontSize = 12.sp, color = JapaneseTextSecondary)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, tint = Color(0xFFE0E0E0))
        }
    }
}