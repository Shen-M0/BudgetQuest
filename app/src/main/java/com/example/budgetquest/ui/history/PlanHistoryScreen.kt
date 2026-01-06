package com.example.budgetquest.ui.history

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.JapaneseBudgetProgressBar
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanHistoryScreen(
    onBackClick: () -> Unit,
    onPlanClick: (Int, Long) -> Unit,
    viewModel: PlanHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_plan_history), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    GlassIconButton(
                        onClick = { debounce(onBackClick) },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(plans, key = { it.plan.id }) { planItem ->

                var itemModifier = Modifier.fillMaxWidth()

                // [動畫關鍵] 只有在 Scope 存在時才套用 SharedElement
                // 這樣在頁面滑動進入時，卡片會跟著頁面一起滑動
                // 而當點擊並導航到 Dashboard 時，這裡的設定會配合 MainActivity 的 fadeOut 觸發縮放效果
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        itemModifier = itemModifier.sharedElement(
                            state = rememberSharedContentState(key = "plan_${planItem.plan.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = FluidBoundsTransform
                        )
                    }
                }

                Box(modifier = itemModifier) {
                    PlanHistoryCard(
                        item = planItem,
                        onClick = {
                            debounce {
                                onPlanClick(planItem.plan.id, planItem.plan.startDate)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanHistoryCard(
    item: PlanHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatPattern = stringResource(R.string.format_date_history)
    val dateFormat = remember(dateFormatPattern) { SimpleDateFormat(dateFormatPattern, Locale.getDefault()) }

    val spent = item.totalSpent
    val budget = item.plan.totalBudget

    val isExpired = System.currentTimeMillis() > item.plan.endDate
    val isOngoing = item.plan.isActive && !isExpired

    GlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
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