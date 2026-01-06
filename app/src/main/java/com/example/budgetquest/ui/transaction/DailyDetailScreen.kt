package com.example.budgetquest.ui.transaction

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraFloatingActionButton
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartNote
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// 快照物件
data class DailyUiSnapshot(
    val date: Long,
    val expenses: List<ExpenseEntity>,
    val totalAmount: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DailyDetailScreen(
    date: Long,
    onBackClick: () -> Unit,
    onAddExpenseClick: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: DailyDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    // 初始化
    LaunchedEffect(date) {
        viewModel.setDate(date)
    }

    val currentDate by viewModel.currentDate.collectAsState(initial = date)
    val expenses by viewModel.expenses.collectAsState()
    val canGoPrev by viewModel.canGoPrevious.collectAsState()
    val canGoNext by viewModel.canGoNext.collectAsState()

    val totalAmount = remember(expenses) {
        expenses.filter { !it.excludeFromBudget }.sumOf { it.amount }
    }

    // 建立快照
    val currentSnapshot = remember(currentDate, expenses, totalAmount) {
        DailyUiSnapshot(currentDate, expenses, totalAmount)
    }

    val dateFormatPattern = stringResource(R.string.format_date_standard)
    val dateFormatter = remember(dateFormatPattern) { SimpleDateFormat(dateFormatPattern, Locale.getDefault()) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    var isSlidingNext by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth().offset(x = (-24).dp), contentAlignment = Alignment.Center) {
                        Text(dateFormatter.format(Date(currentDate)), color = AppTheme.colors.textPrimary, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    GlassIconButton(
                        onClick = { debounce(onBackClick) },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                )
            )
        },
        floatingActionButton = {
            AuroraFloatingActionButton(
                onClick = { debounce { onAddExpenseClick(currentDate) } }
            )
        }
    ) { innerPadding ->

        // 外層容器 SharedElement
        var containerModifier = Modifier
            .padding(innerPadding)
            .padding(20.dp)
            .fillMaxSize()

        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                containerModifier = containerModifier.sharedElement(
                    state = rememberSharedContentState(key = "day_$date"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = FluidBoundsTransform
                )
            }
        }

        var lastSwipeTime by remember { mutableLongStateOf(0L) }
        val swipeThresholdTime = 400L

        Box(modifier = containerModifier) {

            // 全螢幕手勢偵測
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val now = System.currentTimeMillis()
                            if (now - lastSwipeTime > swipeThresholdTime && abs(dragAmount) > 30) {
                                if (dragAmount > 0) { // 右滑 -> 上一天
                                    if (canGoPrev) {
                                        isSlidingNext = false
                                        viewModel.goToPreviousDay()
                                        lastSwipeTime = now
                                    }
                                } else { // 左滑 -> 下一天
                                    if (canGoNext) {
                                        isSlidingNext = true
                                        viewModel.goToNextDay()
                                        lastSwipeTime = now
                                    }
                                }
                            }
                        }
                    }
            ) {
                // 內容滑動動畫區
                AnimatedContent(
                    targetState = currentSnapshot,
                    // [關鍵修正] 設定 contentKey
                    // 只有當 snapshot.date 改變時，才視為畫面切換，執行 transitionSpec。
                    // 如果 date 沒變 (例如只是 expenses 列表更新)，則不執行滑動動畫，直接原地重組。
                    contentKey = { snapshot -> snapshot.date },
                    transitionSpec = {
                        val duration = 400
                        if (isSlidingNext) {
                            (slideInHorizontally(animationSpec = tween(duration)) { width -> width } + fadeIn(animationSpec = tween(duration))).togetherWith(
                                slideOutHorizontally(animationSpec = tween(duration)) { width -> -width/2 } + fadeOut(animationSpec = tween(duration)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(duration)) { width -> -width } + fadeIn(animationSpec = tween(duration))).togetherWith(
                                slideOutHorizontally(animationSpec = tween(duration)) { width -> width/2 } + fadeOut(animationSpec = tween(duration)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "DateSlide",
                    modifier = Modifier.fillMaxSize()
                ) { targetSnapshot ->

                    Column(modifier = Modifier.fillMaxSize()) {
                        TotalAmountCard(targetSnapshot.totalAmount)
                        Spacer(modifier = Modifier.height(20.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(targetSnapshot.expenses, key = { it.id }) { expense ->

                                // 列表項目 SharedElement
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
                                    JapaneseTransactionItem(
                                        expense = expense,
                                        onClick = { debounce { onItemClick(expense.id) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 懸浮箭頭按鈕 (修正版)
            if (canGoPrev) {
                NavigationHintArrow(
                    isLeft = true,
                    onClick = {
                        isSlidingNext = false
                        viewModel.goToPreviousDay()
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            if (canGoNext) {
                NavigationHintArrow(
                    isLeft = false,
                    onClick = {
                        isSlidingNext = true
                        viewModel.goToNextDay()
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

// 導航提示箭頭 (維持您喜歡的樣式)
@Composable
fun NavigationHintArrow(
    isLeft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            // 寬度 48dp, 高度 120dp 的點擊區域
            .size(width = 48.dp, height = 120.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 無水波紋，僅作為提示
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isLeft) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            // 淺灰色，透明度 0.3f，符合"暗示"需求
            tint = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp) // 圖示大小
        )
    }
}

// ... (TotalAmountCard, JapaneseTransactionItem, getCategoryColorDot 保持不變)
@Composable
fun TotalAmountCard(totalAmount: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(stringResource(R.string.label_total_expense), fontSize = 14.sp, color = AppTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.amount_currency_format, totalAmount), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
        }
    }
}

@Composable
fun JapaneseTransactionItem(
    expense: ExpenseEntity,
    onClick: () -> Unit,
) {
    val categoryColor = getCategoryColorDot(expense.category)
    val displayCategory = getSmartCategoryName(expense.category)
    val displayNote = getSmartNote(expense.note)

    val itemAlpha = if (expense.excludeFromBudget) 0.5f else 1f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = displayCategory, color = AppTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                        if (expense.excludeFromBudget) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.text_excluded_label), color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = displayNote, color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

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

private fun getCategoryColorDot(rawCategory: String): Color {
    return when {
        rawCategory in listOf("飲食", "餐饮", "Food", "食事", "cat_food") -> Color(0xFFFFAB91)
        rawCategory in listOf("購物", "购物", "Shopping", "買い物", "cat_shopping") -> Color(0xFF90CAF9)
        rawCategory in listOf("交通", "Transport", "cat_transport") -> Color(0xFFFFF59D)
        rawCategory in listOf("娛樂", "娱乐", "Entertainment", "エンタメ", "cat_entertainment") -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}