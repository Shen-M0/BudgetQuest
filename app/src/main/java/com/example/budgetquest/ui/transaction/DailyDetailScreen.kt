package com.example.budgetquest.ui.transaction

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartNote
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.BoundsTransform
import com.example.budgetquest.ui.common.FluidBoundsTransform
import androidx.compose.animation.core.FastOutSlowInEasing


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DailyDetailScreen(
    date: Long,
    onBackClick: () -> Unit,
    onAddExpenseClick: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: DailyDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
    // [新增] 接收轉場 Scope
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    LaunchedEffect(date) {
        viewModel.setDate(date)
    }

    val expenses by viewModel.expenses.collectAsState()

    val totalAmount = remember(expenses) { expenses.sumOf { it.amount } }

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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(dateFormatter.format(Date(date)), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
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
                onClick = { debounce { onAddExpenseClick(date) } }
            )
        }
    ) { innerPadding ->

        // [新增] 準備轉場 Modifier
        var contentModifier = Modifier
            .padding(innerPadding)
            .padding(20.dp)
            .fillMaxSize()

        // 如果 Scope 存在，則加入 sharedElement
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                contentModifier = contentModifier.sharedElement(
                    state = rememberSharedContentState(key = "day_$date"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    // [使用] 引用共通的動畫設定
                    boundsTransform = FluidBoundsTransform
                )
            }
        }

        // [修改] 將 modifier 應用在 Column 上
        Column(
            modifier = contentModifier
        ) {
            TotalAmountCard(totalAmount)
            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    // [修改] 準備 Item 的轉場 Modifier
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
                            // [移除] onDelete 參數
                        )
                    }
                }
            }
        }
    }
}

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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左側內容群組
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 琉璃珠分類圓點
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
                    Text(text = displayCategory, color = AppTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = displayNote, color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // 右側內容群組 - [移除] 刪除按鈕
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.amount_negative_format, expense.amount),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // 這裡原本的刪除按鈕 Box 已移除
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