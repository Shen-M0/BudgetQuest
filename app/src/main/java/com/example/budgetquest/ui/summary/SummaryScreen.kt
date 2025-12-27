package com.example.budgetquest.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.JapaneseBudgetProgressBar
import com.example.budgetquest.ui.transaction.categories
import com.example.budgetquest.ui.transaction.quickTags
import java.text.SimpleDateFormat
import java.util.*
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.TagManagerDialog
import com.example.budgetquest.ui.transaction.EditButton // 確保能引用到 EditButton
import com.example.budgetquest.ui.common.getIconByKey
// 日系配色
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onBackClick: () -> Unit,
    viewModel: SummaryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    // [新增] 觀察資料庫列表
    val categories by viewModel.visibleCategories.collectAsState()
    val tags by viewModel.visibleTags.collectAsState()

    // [新增] Dialog 狀態
    var showCategoryManager by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }

    // [新增] Dialog 顯示
    if (showCategoryManager) {
        val allCategories by viewModel.allCategories.collectAsState()
        CategoryManagerDialog(allCategories, { showCategoryManager = false }, viewModel::addCategory, viewModel::toggleCategoryVisibility, viewModel::deleteCategory)
    }
    if (showTagManager) {
        val allTags by viewModel.allTags.collectAsState()
        TagManagerDialog(allTags, { showTagManager = false }, viewModel::addTag, viewModel::toggleTagVisibility, viewModel::deleteTag)
    }

    Scaffold(
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = { Text("消費記錄", color = JapaneseTextPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = JapaneseTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 極簡儀表板
            if (uiState.plan != null) {
                MinimalBudgetCard(
                    totalBudget = uiState.plan!!.totalBudget,
                    totalSpent = uiState.totalSpent,
                    message = uiState.resultMessage
                )
            }

            // 2. 圓餅圖
            if (uiState.totalSpent > 0) {
                CollapsiblePieChart(data = uiState.categoryStats)
            }

            // 3. 搜尋與篩選 (更新參數)
            JapaneseFilterCard(
                searchQuery = searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                selectedCategory = selectedCategory,
                onCategorySelect = viewModel::onCategoryFilterChanged,
                selectedTag = selectedTag,
                onTagSelect = viewModel::onTagFilterChanged,
                // [新增] 傳入列表與回呼
                categories = categories,
                tags = tags,
                onEditCategory = { showCategoryManager = true },
                onEditTag = { showTagManager = true }
            )

            // 4. 消費列表
            if (uiState.filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("暫無紀錄", color = JapaneseTextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.filteredExpenses, key = { it.id }) { expense ->
                        JapaneseExpenseItem(
                            expense = expense,
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }
}

// [修改] 更新 FilterCard 接收動態列表
@Composable
fun JapaneseFilterCard(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String) -> Unit,
    selectedTag: String?,
    onTagSelect: (String) -> Unit,
    // [新增參數]
    categories: List<com.example.budgetquest.data.CategoryEntity>,
    tags: List<com.example.budgetquest.data.TagEntity>,
    onEditCategory: () -> Unit,
    onEditTag: () -> Unit
) {
    // [新增] 展開狀態，預設為 false (收起) 或 true (展開) 看你喜好
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. 標題列 (永遠顯示，點擊可切換)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }, // 點擊標題切換
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("篩選條件", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = JapaneseTextPrimary)

                // 箭頭圖示
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = JapaneseTextSecondary
                )
            }

            // 2. 內容區 (可折疊)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp), // 展開後加一點上方間距
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 分類選擇
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            JapaneseCompactChip(cat.name, selectedCategory == cat.name, getIconByKey(cat.iconKey)) { onCategorySelect(cat.name) }
                        }
                        item { EditButton(onEditCategory) }
                    }

                    // 標籤選擇
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            JapaneseCompactChip(tag.name, selectedTag == tag.name) { onTagSelect(tag.name) }
                        }
                        item { EditButton(onEditTag) }
                    }

                    // 搜尋框
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜尋備註...", color = JapaneseTextSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = JapaneseTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = JapaneseAccent,
                            focusedContainerColor = JapaneseBg,
                            unfocusedContainerColor = JapaneseBg
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = JapaneseTextPrimary)
                    )
                }
            }

            // 如果收起且有設定篩選條件，可以顯示一個小的摘要文字 (選用)
            if (!expanded && (selectedCategory != null || selectedTag != null || searchQuery.isNotEmpty())) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "已套用篩選",
                    fontSize = 12.sp,
                    color = JapaneseAccent
                )
            }
        }
    }
}

// 復用 SubscriptionScreen 的 Chip 樣式
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

@Composable
fun MinimalBudgetCard(totalBudget: Int, totalSpent: Int, message: String) {
    val progress = (totalSpent.toFloat() / totalBudget).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1200))
    val remaining = totalBudget - totalSpent

    Card(
        colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$ $remaining", fontSize = 32.sp, fontWeight = FontWeight.Light, color = JapaneseTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("剩餘可用", fontSize = 12.sp, color = JapaneseTextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            JapaneseBudgetProgressBar(
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, fontSize = 13.sp, lineHeight = 20.sp, color = JapaneseTextSecondary)
        }
    }
}

@Composable
fun CollapsiblePieChart(data: List<CategoryStat>) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(JapaneseSurface).clickable { expanded = !expanded }.padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("消費分佈", color = JapaneseTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = JapaneseTextSecondary, modifier = Modifier.size(20.dp))
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                PieChart(data = data)
            }
        }
    }
}

@Composable
fun JapaneseExpenseItem(expense: ExpenseEntity, onDelete: () -> Unit) {
    val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(JapaneseSurface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.size(10.dp).background(getCategoryColorDot(expense.category), CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = expense.category, color = JapaneseTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(text = " · ${dateFormatter.format(Date(expense.date))}", color = JapaneseTextSecondary.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = expense.note, color = JapaneseTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "- $${expense.amount}", color = JapaneseTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "刪除", tint = Color(0xFFE0E0E0), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun getCategoryColorDot(category: String): Color {
    return when(category) {
        "飲食" -> Color(0xFFFFAB91)
        "購物" -> Color(0xFF90CAF9)
        "交通" -> Color(0xFFFFF59D)
        "娛樂" -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}

@Composable
fun PieChart(
    data: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.totalAmount }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左邊畫圓餅
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                var startAngle = -90f // 從 12 點鐘方向開始
                data.forEach { stat ->
                    val sweepAngle = (stat.totalAmount.toFloat() / total) * 360f

                    drawArc(
                        color = stat.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true, // 填滿圓餅
                        // style = Stroke(width = 50f) // 如果想要甜甜圈圖，可以打開這個
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // 2. 右邊顯示圖例 (Legend)
        Column(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(stat.color, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${stat.name}: $${stat.totalAmount} (${(stat.totalAmount.toFloat() / total * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}