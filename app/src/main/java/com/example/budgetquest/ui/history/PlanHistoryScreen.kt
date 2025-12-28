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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.JapaneseBudgetProgressBar // [import]
import com.example.budgetquest.ui.theme.AppTheme // [import]
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: PlanHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val plans by viewModel.allPlans.collectAsState()

    // [優化] 防手震
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
                title = { Text("計畫歷程", color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { debounce(onBackClick) }) { // [優化] 防手震
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
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
            // [優化] 加入 key (假設 PlanHistoryItem.plan 有 id)
            items(plans, key = { it.plan.id }) { planItem ->
                PlanHistoryCard(planItem)
            }
        }
    }
}

@Composable
fun PlanHistoryCard(item: PlanHistoryItem) {
    // [優化] 快取 formatter，避免列表捲動時重複建立物件
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

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
                    Text("進行中", fontSize = 12.sp, color = AppTheme.colors.success, fontWeight = FontWeight.Bold)
                } else {
                    Text("已結束", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${dateFormat.format(Date(item.plan.startDate))} - ${dateFormat.format(Date(item.plan.endDate))}",
                fontSize = 12.sp,
                color = AppTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            JapaneseBudgetProgressBar(totalBudget = budget, totalSpent = spent)

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("已花費 $$spent", fontSize = 12.sp, color = AppTheme.colors.fail)
                Text("預算 $$budget", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            }
        }
    }
}