package com.example.budgetquest.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHistoryScreen(
    onBackClick: () -> Unit,
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
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                // [提取] 標題
                title = { Text(stringResource(R.string.title_plan_history), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    // [提取] 返回按鈕描述 (共用)
                    IconButton(onClick = { debounce(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(plans, key = { it.plan.id }) { planItem ->
                PlanHistoryCard(planItem)
            }
        }
    }
}

@Composable
fun PlanHistoryCard(item: PlanHistoryItem) {
    // [提取] 日期格式
    val dateFormatPattern = stringResource(R.string.format_date_history)
    val dateFormat = remember(dateFormatPattern) { SimpleDateFormat(dateFormatPattern, Locale.getDefault()) }

    val spent = item.totalSpent
    val budget = item.plan.totalBudget

    val isExpired = System.currentTimeMillis() > item.plan.endDate
    val isOngoing = item.plan.isActive && !isExpired

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.plan.planName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )

                if (isOngoing) {
                    // [提取] 進行中
                    Text(stringResource(R.string.status_ongoing), fontSize = 12.sp, color = AppTheme.colors.success, fontWeight = FontWeight.Bold)
                } else {
                    // [提取] 已結束
                    Text(stringResource(R.string.status_ended), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // [提取] 日期範圍格式 (yyyy/MM/dd - yyyy/MM/dd)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                // [提取] 已花費金額格式
                Text(stringResource(R.string.label_spent_amount, spent), fontSize = 12.sp, color = AppTheme.colors.fail)
                // [提取] 預算金額格式
                Text(stringResource(R.string.label_budget_amount, budget), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            }
        }
    }
}