package com.example.budgetquest.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.JapaneseBudgetProgressBar
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

// [美術] 定義全域一致的玻璃筆刷
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
    // 斜向漸層邊框 (左上 -> 右下)
    return Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.textPrimary.copy(alpha = 0.25f),
            AppTheme.colors.textPrimary.copy(alpha = 0.10f)
        )
    )
}

// [美術] 玻璃圓形按鈕容器 (統一風格)
@Composable
fun GlassIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = modifier
            .size(40.dp) // 統一大小
            .clip(CircleShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHistoryScreen(
    onBackClick: () -> Unit,
    // [功能新增] 點擊計畫時的回調 (PlanID, TargetDate)
    onPlanClick: (Int, Long) -> Unit,
    viewModel: PlanHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val plans by viewModel.allPlans.collectAsState()

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    Scaffold(
        // [美術] 背景透明
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_plan_history), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    // [美術優化] 返回按鈕：圓形水波紋
                    // 順序：padding -> clip(Circle) -> clickable
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(CircleShape)
                            .clickable { debounce(onBackClick) }
                    ) {
                        GlassIconContainer {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = AppTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                // [美術] 頂欄透明
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(plans, key = { it.plan.id }) { planItem ->
                PlanHistoryCard(
                    item = planItem,
                    onClick = {
                        debounce {
                            // [功能新增] 跳轉邏輯
                            // 傳入 Plan ID，並使用該計畫的 startDate 作為跳轉日期 (最早月份)
                            onPlanClick(planItem.plan.id, planItem.plan.startDate)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlanHistoryCard(
    item: PlanHistoryItem,
    onClick: () -> Unit // [功能新增] 接收點擊事件
) {
    val dateFormatPattern = stringResource(R.string.format_date_history)
    val dateFormat = remember(dateFormatPattern) { SimpleDateFormat(dateFormatPattern, Locale.getDefault()) }

    val spent = item.totalSpent
    val budget = item.plan.totalBudget

    val isExpired = System.currentTimeMillis() > item.plan.endDate
    val isOngoing = item.plan.isActive && !isExpired

    // [美術] 取得筆刷
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    // [美術] 改用 Box + 玻璃質感
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)) // 1. 先裁切形狀
            .background(glassBrush)          // 2. 背景
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp)) // 3. 邊框
            .clickable { onClick() }         // 4. 設定點擊 (水波紋會符合 24dp 圓角矩形)
            .padding(24.dp)                  // 5. 最後才是內距
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.plan.planName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )

                if (isOngoing) {
                    Text(stringResource(R.string.status_ongoing), fontSize = 12.sp, color = AppTheme.colors.success, fontWeight = FontWeight.Bold)
                } else {
                    Text(stringResource(R.string.status_ended), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                stringResource(
                    R.string.format_date_range_history,
                    dateFormat.format(Date(item.plan.startDate)),
                    dateFormat.format(Date(item.plan.endDate))
                ),
                fontSize = 12.sp,
                color = AppTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            JapaneseBudgetProgressBar(totalBudget = budget, totalSpent = spent)

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.label_spent_amount, spent), fontSize = 12.sp, color = AppTheme.colors.fail)
                Text(stringResource(R.string.label_budget_amount, budget), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            }
        }
    }
}