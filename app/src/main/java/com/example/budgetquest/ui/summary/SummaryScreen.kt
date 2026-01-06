package com.example.budgetquest.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
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
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassChip
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTabRow
import com.example.budgetquest.ui.common.MinimalBudgetCard
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getShadowTextStyle
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartNote
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.TagManagerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SummaryScreen(
    planId: Int,
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit = {},
    viewModel: SummaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
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

    // 分頁狀態
    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_summary), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                    navigationIcon = {
                        GlassIconButton(
                            onClick = { debounce(onBackClick) },
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = AppTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                    )
                )

                // 頂部分頁切換 (TabRow)
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    GlassTabRow(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it },
                        tabs = listOf(
                            stringResource(R.string.tab_list_view),
                            stringResource(R.string.tab_analysis_view)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->

        // 根據 Tab 切換顯示內容
        Crossfade(targetState = selectedTabIndex, label = "TabSwitch") { tabIndex ->
            if (tabIndex == 0) {
                // === TAB 0: 明細列表 (List) ===
                TransactionListContent(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    selectedTag = selectedTag,
                    categories = categories,
                    tags = tags,
                    onSearchQueryChanged = viewModel::updateSearchQuery,
                    onCategorySelect = viewModel::toggleCategoryFilter,
                    onTagSelect = viewModel::toggleTagFilter,
                    onEditCategory = { debounce { showCategoryManager = true } },
                    onEditTag = { debounce { showTagManager = true } },
                    onItemClick = onItemClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    contentPadding = innerPadding
                )
            } else {
                // === TAB 1: 統計分析 (Stats) ===
                AnalysisStatsContent(
                    uiState = uiState,
                    contentPadding = innerPadding,
                    onToggleMerchantSort = viewModel::toggleMerchantSort
                )
            }
        }
    }
}

// === 明細列表內容 (原本的內容) ===
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionListContent(
    uiState: SummaryUiState,
    searchQuery: String,
    selectedCategory: String?,
    selectedTag: String?,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onTagSelect: (String) -> Unit,
    onEditCategory: () -> Unit,
    onEditTag: () -> Unit,
    onItemClick: (Long) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
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

        // [移除] 圓餅圖從這裡移除了，因為要搬到分析頁

        item {
            JapaneseFilterCard(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                selectedCategory = selectedCategory,
                onCategorySelect = onCategorySelect,
                selectedTag = selectedTag,
                onTagSelect = onTagSelect,
                categories = categories,
                tags = tags,
                onEditCategory = onEditCategory,
                onEditTag = onEditTag
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }

        if (uiState.filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.msg_no_records_found), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                }
            }
        } else {
            items(uiState.filteredExpenses, key = { it.id }) { expense ->
                var itemModifier = Modifier.fillMaxWidth()
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        itemModifier = itemModifier.sharedElement(
                            state = rememberSharedContentState(key = "trans_${expense.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = FluidBoundsTransform
                        )
                    }
                }

                Box(modifier = itemModifier) {
                    JapaneseExpenseItem(
                        expense = expense,
                        onClick = { onItemClick(expense.id) }
                    )
                }
            }
        }
    }
}

// === [修改] 統計分析內容 ===
@Composable
fun AnalysisStatsContent(
    uiState: SummaryUiState,
    contentPadding: PaddingValues,
    onToggleMerchantSort: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 20.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. 總覽卡片
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.label_spending_overview), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_real_total_spent), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.amount_currency_format, uiState.totalRealSpent),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )
                            if (uiState.totalRealSpent > uiState.totalSpent) {
                                Text(
                                    stringResource(R.string.msg_including_excluded, uiState.totalRealSpent - uiState.totalSpent),
                                    fontSize = 11.sp,
                                    color = AppTheme.colors.textSecondary.copy(alpha=0.7f)
                                )
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppTheme.colors.divider))
                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_budget_spent), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.amount_currency_format, uiState.totalSpent),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.accent
                            )
                        }
                    }
                }
            }
        }

        // 2. 分類圓餅圖
        if (uiState.categoryStats.isNotEmpty()) {
            item {
                CollapsiblePieChart(data = uiState.categoryStats)
            }
        }

        // 3. Need / Want
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, null, tint = AppTheme.colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.label_consumption_nature), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val totalNW = uiState.needAmount + uiState.wantAmount
                    if (totalNW > 0) {
                        val needPercent = uiState.needPercent
                        val wantPercent = uiState.wantPercent

                        Row(modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(16.dp))) {
                            // [修正] 加入 > 0 檢查，防止 weight(0f) 閃退
                            if (needPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(needPercent.toFloat())
                                        .fillMaxHeight()
                                        .background(AppTheme.colors.success.copy(alpha=0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if(needPercent > 10) Text("$needPercent%", fontSize=12.sp, fontWeight=FontWeight.Bold, color=Color.White)
                                }
                            }

                            if (wantPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(wantPercent.toFloat())
                                        .fillMaxHeight()
                                        .background(AppTheme.colors.accent.copy(alpha=0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if(wantPercent > 10) Text("$wantPercent%", fontSize=12.sp, fontWeight=FontWeight.Bold, color=Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.label_need), fontSize = 12.sp, color = AppTheme.colors.success, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.amount_currency_format, uiState.needAmount), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.label_want), fontSize = 12.sp, color = AppTheme.colors.accent, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.amount_currency_format, uiState.wantAmount), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            }
                        }
                    } else {
                        Text(stringResource(R.string.msg_no_data), fontSize = 14.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        // 4. 支付方式
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, null, tint = AppTheme.colors.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.label_payment_distribution), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.paymentStats.isNotEmpty()) {
                        uiState.paymentStats.forEach { stat ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(stat.method, fontSize = 14.sp, color = AppTheme.colors.textPrimary)
                                    Text("${stat.percentage}% (${stringResource(R.string.amount_currency_format, stat.amount)})", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(AppTheme.colors.surface.copy(alpha = 0.3f))) {
                                    Box(modifier = Modifier.fillMaxWidth(stat.percentage / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(AppTheme.colors.accent.copy(alpha=0.6f), AppTheme.colors.accent))))
                                }
                            }
                        }
                    } else {
                        Text(stringResource(R.string.msg_no_data), fontSize = 14.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        // 5. 熱門店家排行
        if (uiState.topMerchants.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.label_merchant_ranking), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                            }

                            MerchantSortToggle(
                                sortType = uiState.merchantSortType,
                                onToggle = onToggleMerchantSort
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        uiState.topMerchants.forEachIndexed { index, merchant ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp).background(color = when(index) {
                                    0 -> Color(0xFFFFD700).copy(alpha=0.8f)
                                    1 -> Color(0xFFC0C0C0).copy(alpha=0.8f)
                                    2 -> Color(0xFFCD7F32).copy(alpha=0.8f)
                                    else -> AppTheme.colors.surface.copy(alpha=0.5f)
                                }, shape = CircleShape), contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(index < 3) Color.White else AppTheme.colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(merchant.name, fontSize = 14.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (uiState.merchantSortType == MerchantSortType.Amount) {
                                        Text(stringResource(R.string.amount_currency_format, merchant.totalAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                                        Text(stringResource(R.string.format_count_times, merchant.count), fontSize = 11.sp, color = AppTheme.colors.textSecondary)
                                    } else {
                                        Text(stringResource(R.string.format_count_times, merchant.count), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                                        Text(stringResource(R.string.amount_currency_format, merchant.totalAmount), fontSize = 11.sp, color = AppTheme.colors.textSecondary)
                                    }
                                }
                            }
                            if (index < uiState.topMerchants.size - 1) {
                                HorizontalDivider(color = AppTheme.colors.divider.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// [新增] 專門的排序切換開關元件
@Composable
fun MerchantSortToggle(
    sortType: MerchantSortType,
    onToggle: () -> Unit
) {
    val isAmount = sortType == MerchantSortType.Amount

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.3f))
            .border(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clickable { onToggle() }
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SortToggleOption(
                text = stringResource(R.string.sort_by_amount),
                isSelected = isAmount
            )
            SortToggleOption(
                text = stringResource(R.string.sort_by_count),
                isSelected = !isAmount
            )
        }
    }
}

@Composable
fun SortToggleOption(
    text: String,
    isSelected: Boolean
) {
    // 選中時：有背景色、白字
    // 未選中：透明背景、灰字
    val backgroundColor = if (isSelected) AppTheme.colors.accent else Color.Transparent
    val textColor = if (isSelected) Color.White else AppTheme.colors.textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * 可展開的圓餅圖卡片
 */
@Composable
fun CollapsiblePieChart(data: List<CategoryStat>) {
    var expanded by remember { mutableStateOf(true) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
}

/**
 * 圓餅圖繪製邏輯
 */
@Composable
fun PieChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.totalAmount }
    val rawPercentages = remember(data, total) { data.map { if (total > 0) (it.totalAmount.toFloat() / total * 100).toInt() else 0 }.toMutableList() }
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
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(160.dp)) {
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

                    // 顏色處理
                    val hsl = floatArrayOf(0f, 0f, 0f)
                    ColorUtils.colorToHSL(stat.color.toArgb(), hsl)
                    hsl[2] = (hsl[2] + 0.15f).coerceIn(0f, 1f)
                    val lightColor = Color(ColorUtils.HSLToColor(hsl))
                    hsl[2] = (hsl[2] - 0.2f).coerceIn(0f, 1f)
                    val darkColor = Color(ColorUtils.HSLToColor(hsl))

                    val sweepFraction = adjustedSweep / 360f
                    val gradientColors = listOf(lightColor, stat.color, darkColor)
                    val colorStops = listOf(0.0f, sweepFraction * 0.5f, sweepFraction)
                    val brush = Brush.sweepGradient(colorStops = colorStops.zip(gradientColors).toTypedArray(), center = Offset(halfSize.width, halfSize.height))

                    rotate(degrees = currentStartAngle, pivot = center) {
                        drawArc(brush = brush, startAngle = 0f, sweepAngle = adjustedSweep, useCenter = false, topLeft = topLeft, size = sizeObj, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
                    }
                    currentStartAngle += sweepAngle
                }
                if (total == 0) {
                    drawArc(brush = Brush.sweepGradient(colors = listOf(Color.Gray.copy(alpha = 0.1f), Color.Gray.copy(alpha = 0.2f))), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = sizeObj, style = Stroke(width = strokeWidth))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.label_total), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                Text(text = stringResource(R.string.amount_currency_format, total), style = getShadowTextStyle(fontSize = 20, fontWeight = FontWeight.ExtraBold))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.forEachIndexed { index, stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(width = 12.dp, height = 12.dp).background(stat.color, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = getSmartCategoryName(stat.name), style = MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${correctedPercentages.getOrElse(index) { 0 }}% (${stringResource(R.string.amount_currency_format, stat.totalAmount)})", style = MaterialTheme.typography.labelSmall.copy(color = AppTheme.colors.textSecondary))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 篩選條件卡片 (包含搜尋、分類、標籤)
 */
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

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
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
                            GlassChip(
                                label = getSmartCategoryName(cat.name, cat.resourceKey),
                                selected = selectedCategory == cat.name,
                                icon = getIconByKey(cat.iconKey),
                                onClick = { onCategorySelect(cat.name) }
                            )
                        }
                        item {
                            GlassIconButton(onClick = onEditCategory, size = 32.dp) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            GlassChip(
                                label = getSmartTagName(tag.name, tag.resourceKey),
                                selected = selectedTag == tag.name,
                                onClick = { onTagSelect(tag.name) }
                            )
                        }
                        item {
                            GlassIconButton(onClick = onEditTag, size = 32.dp) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
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

/**
 * 列表中的單筆消費項目
 */
@Composable
fun JapaneseExpenseItem(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    val dateFormat = stringResource(R.string.format_date_month_day)
    val dateFormatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }
    val itemAlpha = if (expense.excludeFromBudget) 0.5f else 1f

    GlassCard(
        modifier = Modifier.fillMaxWidth().alpha(itemAlpha),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val categoryColor = getCategoryColorDot(expense.category)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(categoryColor.copy(alpha = 0.4f), categoryColor),
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
                    Text(
                        text = getSmartCategoryName(expense.category),
                        color = AppTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (expense.excludeFromBudget) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // [修正] 加上 stringResource()
                        Text(
                            text = stringResource(R.string.text_excluded_label),
                            color = AppTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.format_category_date, " ", dateFormatter.format(Date(expense.date))),
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getSmartNote(expense.note),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.amount_negative_format, expense.amount),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 取得分類對應的顏色圓點
private fun getCategoryColorDot(category: String): Color {
    return when(category) {
        "飲食", "餐饮", "Food", "食事", "cat_food" -> Color(0xFFFFAB91)
        "購物", "购物", "Shopping", "買い物", "cat_shopping" -> Color(0xFF90CAF9)
        "交通", "Transport", "cat_transport" -> Color(0xFFFFF59D)
        "娛樂", "娱乐", "Entertainment", "エンタメ", "cat_entertainment" -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}