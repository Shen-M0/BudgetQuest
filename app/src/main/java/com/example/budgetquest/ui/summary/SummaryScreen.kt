package com.example.budgetquest.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
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
import com.example.budgetquest.ui.common.getSmartNote // [新增]
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.TagManagerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// [美術] 定義全域共用的玻璃筆刷
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

// [美術] 玻璃按鈕容器
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

// [美術] 文字陰影樣式
@Composable
fun getShadowTextStyle(fontSize: Int, fontWeight: FontWeight): TextStyle {
    return TextStyle(
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = AppTheme.colors.textPrimary,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.1f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
}

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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_summary), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    // [美術] TopBar 返回按鈕：Box (Padding) -> Clip -> Clickable -> Glass Container
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(CircleShape) // 確保水波紋是圓的
                            .clickable { debounce(onBackClick) }
                    ) {
                        GlassIconContainer(modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = AppTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (uiState.plan != null) {
                item {
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
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (uiState.totalSpent > 0) {
                item {
                    CollapsiblePieChart(data = uiState.categoryStats)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            item {
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
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (uiState.filteredExpenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.msg_no_records_found), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                    }
                }
            } else {
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

@Composable
fun MinimalBudgetCard(totalBudget: Int, totalSpent: Int, message: String) {
    val remaining = totalBudget - totalSpent
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.amount_currency_format, remaining),
                    style = getShadowTextStyle(fontSize = 32, fontWeight = FontWeight.Light)
                )
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
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

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
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
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
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounceToggle() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            expanded = !expanded
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
        // 移除 padding，避免點擊範圍被擠壓
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 標題列點擊邏輯：1. clickable -> 2. padding
                    .clickable { debounceToggle() }
                    .padding(16.dp),
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
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            // [修正] 使用 getSmartCategoryName 進行翻譯
                            JapaneseCompactChip(getSmartCategoryName(cat.name, cat.resourceKey), selectedCategory == cat.name, getIconByKey(cat.iconKey)) { onCategorySelect(cat.name) }
                        }
                        item { JapaneseGlassEditButton(onEditCategory) }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            // [修正] 使用 getSmartTagName 進行翻譯
                            JapaneseCompactChip(getSmartTagName(tag.name, tag.resourceKey), selectedTag == tag.name) { onTagSelect(tag.name) }
                        }
                        item { JapaneseGlassEditButton(onEditTag) }
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
                            focusedContainerColor = AppTheme.colors.background.copy(alpha = 0.5f),
                            unfocusedContainerColor = AppTheme.colors.background.copy(alpha = 0.3f),
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = AppTheme.colors.textPrimary)
                    )
                }
            }
            if (!expanded && (selectedCategory != null || selectedTag != null || searchQuery.isNotEmpty())) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.label_filter_applied),
                    fontSize = 12.sp,
                    color = AppTheme.colors.accent,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

// 獨立實作編輯按鈕 (修復水波紋尺寸)
@Composable
fun JapaneseGlassEditButton(onClick: () -> Unit) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.desc_edit_button),
            tint = AppTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    val backgroundModifier = if (selected) {
        Modifier.background(AppTheme.colors.accent, RoundedCornerShape(8.dp))
    } else {
        Modifier
            .background(glassBrush, RoundedCornerShape(8.dp))
            .border(1.dp, borderBrush, RoundedCornerShape(8.dp))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(backgroundModifier)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) Color.White else AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
                color = if (selected) Color.White else AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
fun JapaneseExpenseItem(expense: ExpenseEntity, onDelete: () -> Unit) {
    val dateFormat = stringResource(R.string.format_date_month_day)
    val dateFormatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // [美術] 琉璃珠分類圓點
        val categoryColor = getCategoryColorDot(expense.category)
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.4f),
                            categoryColor
                        ),
                        center = Offset.Unspecified,
                        radius = Float.POSITIVE_INFINITY
                    ),
                    shape = CircleShape
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // [修正] 使用智慧翻譯 (分類)
                Text(text = getSmartCategoryName(expense.category), color = AppTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = stringResource(R.string.format_category_date, " ", dateFormatter.format(Date(expense.date))),
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // [修正] 使用智慧翻譯 (備註+自動扣款)
            Text(text = getSmartNote(expense.note), color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.amount_negative_format, expense.amount), color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            // [修正] 圓形水波紋刪除按鈕
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.content_desc_delete),
                    tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getCategoryColorDot(category: String): Color {
    // 這裡維持使用原始字串判斷，因為資料庫存的是 Key 或原始值，尚未翻譯
    return when(category) {
        "飲食", "餐饮", "Food", "食事", "cat_food" -> Color(0xFFFFAB91)
        "購物", "购物", "Shopping", "買い物", "cat_shopping" -> Color(0xFF90CAF9)
        "交通", "Transport", "cat_transport" -> Color(0xFFFFF59D)
        "娛樂", "娱乐", "Entertainment", "エンタメ", "cat_entertainment" -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}

@Composable
fun PieChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.totalAmount }

    val rawPercentages = remember(data, total) {
        data.map {
            if (total > 0) (it.totalAmount.toFloat() / total * 100).toInt() else 0
        }.toMutableList()
    }
    val sumPercentage = remember(rawPercentages) { rawPercentages.sum() }
    val correctedPercentages = remember(data, rawPercentages, sumPercentage) {
        val diff = 100 - sumPercentage
        if (diff > 0 && total > 0) {
            val maxIndex = data.indices.maxByOrNull { data[it].totalAmount } ?: 0
            rawPercentages[maxIndex] += diff
        }
        rawPercentages
    }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                // ... (Canvas 繪圖邏輯維持不變) ...
                val strokeWidth = 35.dp.toPx()
                val gapAngle = if (data.size > 1) 2f else 0f

                val innerRadius = (size.minDimension - strokeWidth) / 2
                val halfSize = size / 2.0f
                val topLeft = Offset(halfSize.width - innerRadius, halfSize.height - innerRadius)
                val sizeObj = Size(innerRadius * 2, innerRadius * 2)

                var currentStartAngle = -90f

                data.forEach { stat ->
                    val sweepAngle = (stat.totalAmount.toFloat() / total) * 360f
                    val adjustedSweep = if (sweepAngle > gapAngle) sweepAngle - gapAngle else maxOf(sweepAngle - 0.5f, 0.1f)

                    val hsl = floatArrayOf(0f, 0f, 0f)
                    ColorUtils.colorToHSL(stat.color.toArgb(), hsl)
                    hsl[2] = (hsl[2] + 0.15f).coerceIn(0f, 1f)
                    val lightColor = Color(ColorUtils.HSLToColor(hsl))
                    hsl[2] = (hsl[2] - 0.2f).coerceIn(0f, 1f)
                    val darkColor = Color(ColorUtils.HSLToColor(hsl))

                    val sweepFraction = adjustedSweep / 360f
                    val gradientColors = listOf(lightColor, stat.color, darkColor)
                    val colorStops = listOf(0.0f, sweepFraction * 0.5f, sweepFraction)

                    val brush = Brush.sweepGradient(
                        colorStops = colorStops.zip(gradientColors).toTypedArray(),
                        center = Offset(halfSize.width, halfSize.height)
                    )

                    rotate(degrees = currentStartAngle, pivot = center) {
                        drawArc(
                            brush = brush,
                            startAngle = 0f,
                            sweepAngle = adjustedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = sizeObj,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }
                    currentStartAngle += sweepAngle
                }

                if (total == 0) {
                    val emptyBrush = Brush.sweepGradient(
                        colors = listOf(Color.Gray.copy(alpha = 0.1f), Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.1f))
                    )
                    drawArc(
                        brush = emptyBrush,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = sizeObj,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.label_total), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                Text(
                    text = stringResource(R.string.amount_currency_format, total),
                    style = getShadowTextStyle(fontSize = 20, fontWeight = FontWeight.ExtraBold)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            data.forEachIndexed { index, stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 12.dp)
                            .background(stat.color, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        // [修正] 使用智慧翻譯 (圖例分類)
                        Text(
                            text = getSmartCategoryName(stat.name),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${correctedPercentages.getOrElse(index) { 0 }}% (${stringResource(R.string.amount_currency_format, stat.totalAmount)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AppTheme.colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}