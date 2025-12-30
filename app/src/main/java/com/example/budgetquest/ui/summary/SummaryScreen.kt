package com.example.budgetquest.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.TagEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.JapaneseBudgetProgressBar
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName // [新增]
import com.example.budgetquest.ui.common.getSmartTagName // [新增]
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.EditButton
import com.example.budgetquest.ui.transaction.TagManagerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    planId: Int,
    onBackClick: () -> Unit,
    viewModel: SummaryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    LaunchedEffect(planId) {
        viewModel.setPlanId(planId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    val categories by viewModel.visibleCategories.collectAsState()
    val tags by viewModel.visibleTags.collectAsState()

    var showCategoryManager by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }

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
        CategoryManagerDialog(allCategories, { showCategoryManager = false }, viewModel::addCategory, viewModel::toggleCategoryVisibility, viewModel::deleteCategory)
    }
    if (showTagManager) {
        val allTags by viewModel.allTags.collectAsState()
        TagManagerDialog(allTags, { showTagManager = false }, viewModel::addTag, viewModel::toggleTagVisibility, viewModel::deleteTag)
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_summary), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { debounce(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
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
            if (uiState.plan != null) {
                // [提取] 狀態訊息
                val message = when (uiState.budgetStatus) {
                    BudgetStatus.Achieved -> stringResource(R.string.msg_goal_achieved)
                    BudgetStatus.Exceeded -> stringResource(R.string.msg_budget_exceeded)
                    BudgetStatus.None -> stringResource(R.string.msg_no_plan_data)
                }

                MinimalBudgetCard(
                    totalBudget = uiState.plan!!.totalBudget,
                    totalSpent = uiState.totalSpent,
                    message = message
                )
            }

            if (uiState.totalSpent > 0) {
                CollapsiblePieChart(data = uiState.categoryStats)
            }

            JapaneseFilterCard(
                searchQuery = searchQuery,
                onSearchQueryChanged = viewModel::updateSearchQuery,
                selectedCategory = selectedCategory,
                onCategorySelect = viewModel::toggleCategoryFilter,
                selectedTag = selectedTag,
                onTagSelect = viewModel::toggleTagFilter,
                categories = categories,
                tags = tags,
                onEditCategory = { debounce { showCategoryManager = true } },
                onEditTag = { debounce { showTagManager = true } }
            )

            if (uiState.filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.msg_no_records_found), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
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
                            onDelete = { debounce { viewModel.deleteExpense(expense) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalBudgetCard(totalBudget: Int, totalSpent: Int, message: String) {
    val remaining = totalBudget - totalSpent

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.amount_currency_format, remaining), fontSize = 32.sp, fontWeight = FontWeight.Light, color = AppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.label_remaining_budget), fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            JapaneseBudgetProgressBar(
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, fontSize = 13.sp, lineHeight = 20.sp, color = AppTheme.colors.textSecondary)
        }
    }
}

@Composable
fun CollapsiblePieChart(data: List<CategoryStat>) {
    var expanded by remember { mutableStateOf(false) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounceToggle() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            expanded = !expanded
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .clickable { debounceToggle() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.label_expense_distribution), color = AppTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
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
fun JapaneseFilterCard(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String) -> Unit,
    selectedTag: String?,
    onTagSelect: (String) -> Unit,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    onEditCategory: () -> Unit,
    onEditTag: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounceToggle() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            expanded = !expanded
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { debounceToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_filter_conditions), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            // [套用 Helper] 分類篩選
                            JapaneseCompactChip(getSmartCategoryName(cat.name, cat.resourceKey), selectedCategory == cat.name, getIconByKey(cat.iconKey)) { onCategorySelect(cat.name) }
                        }
                        item { EditButton(onEditCategory) }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            // [套用 Helper] 備註篩選
                            JapaneseCompactChip(getSmartTagName(tag.name, tag.resourceKey), selectedTag == tag.name) { onTagSelect(tag.name) }
                        }
                        item { EditButton(onEditTag) }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.hint_search_note), color = AppTheme.colors.textSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppTheme.colors.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AppTheme.colors.accent,
                            focusedContainerColor = AppTheme.colors.background,
                            unfocusedContainerColor = AppTheme.colors.background,
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = AppTheme.colors.textPrimary)
                    )
                }
            }
            if (!expanded && (selectedCategory != null || selectedTag != null || searchQuery.isNotEmpty())) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.label_filter_applied), fontSize = 12.sp, color = AppTheme.colors.accent)
            }
        }
    }
}

@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
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
fun JapaneseExpenseItem(expense: ExpenseEntity, onDelete: () -> Unit) {
    val dateFormat = stringResource(R.string.format_date_month_day)
    val dateFormatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppTheme.colors.surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.size(10.dp).background(getCategoryColorDot(expense.category), CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // [套用 Helper] 列表分類顯示
                Text(text = getSmartCategoryName(expense.category), color = AppTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = stringResource(R.string.format_category_date, " ", dateFormatter.format(Date(expense.date))),
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = expense.note, color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.amount_negative_format, expense.amount), color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, stringResource(R.string.content_desc_delete), tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
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
fun PieChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.totalAmount }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                var startAngle = -90f
                data.forEach { stat ->
                    val sweepAngle = (stat.totalAmount.toFloat() / total) * 360f
                    drawArc(color = stat.color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                    startAngle += sweepAngle
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEach { stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(stat.color, shape = CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    // [套用 Helper] 圓餅圖分類顯示
                    Text(
                        text = stringResource(R.string.format_pie_chart_legend, getSmartCategoryName(stat.name), stat.totalAmount, (stat.totalAmount.toFloat() / total * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.textPrimary)
                    )
                }
            }
        }
    }
}