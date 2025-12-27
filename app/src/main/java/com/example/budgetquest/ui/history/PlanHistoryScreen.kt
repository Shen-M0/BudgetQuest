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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.data.PlanEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.JapaneseBudgetProgressBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: PlanHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val plans by viewModel.allPlans.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF7F9FC),
        topBar = {
            TopAppBar(
                title = { Text("計畫歷程", color = Color(0xFF455A64), fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF455A64)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F9FC))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(plans) { planItem ->
                PlanHistoryCard(planItem)
            }
        }
    }
}

@Composable
fun PlanHistoryCard(item: PlanHistoryItem) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val spent = item.totalSpent
    val budget = item.plan.totalBudget

    // [新增邏輯] 判斷是否過期
    // 如果 "現在時間" 大於 "計畫結束時間"，就視為已結束
    val isExpired = System.currentTimeMillis() > item.plan.endDate
    // 顯示狀態：如果 isActive 且 沒過期 才算進行中，否則都是已結束
    val isOngoing = item.plan.isActive && !isExpired

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(item.plan.planName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))

                // [修改] 根據新的判斷顯示文字
                if (isOngoing) {
                    Text("進行中", fontSize = 12.sp, color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold)
                } else {
                    // 對於過去補填的計畫，這裡會正確顯示已結束
                    Text("已結束", fontSize = 12.sp, color = Color(0xFF90A4AE))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${dateFormat.format(Date(item.plan.startDate))} - ${dateFormat.format(Date(item.plan.endDate))}",
                fontSize = 12.sp, color = Color(0xFF90A4AE)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 使用新版反向進度條
            JapaneseBudgetProgressBar(totalBudget = budget, totalSpent = spent)

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("已花費 $$spent", fontSize = 12.sp, color = Color(0xFFEF9A9A))
                Text("預算 $$budget", fontSize = 12.sp, color = Color(0xFF90A4AE))
            }
        }
    }
}