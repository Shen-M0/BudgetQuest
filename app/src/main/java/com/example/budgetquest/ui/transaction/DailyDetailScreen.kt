package com.example.budgetquest.ui.transaction

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(
    date: Long,
    onBackClick: () -> Unit,
    onAddExpenseClick: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: DailyDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    LaunchedEffect(date) {
        viewModel.setDate(date)
    }

    val uiState by viewModel.uiState.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    // [優化] 快取日期格式化物件
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.getDefault()) }

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
                title = {
                    Text(
                        dateFormatter.format(Date(uiState.date)),
                        color = AppTheme.colors.textPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { debounce(onBackClick) }) { // [優化] 防手震
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { debounce { onAddExpenseClick(uiState.date) } }, // [優化] 防手震
                containerColor = AppTheme.colors.accent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp)) {

            DailySummaryCard(
                totalAmount = expenses.sumOf { it.amount },
                count = expenses.size
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("本日無消費紀錄", color = AppTheme.colors.textSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // [優化] items 已有 key，這很好
                    items(expenses, key = { it.id }) { expense ->
                        JapaneseDetailItem(
                            expense = expense,
                            onClick = { debounce { onItemClick(expense.id) } }, // [優化] 防手震
                            onDelete = {
                                debounce { viewModel.deleteExpense(expense) } // [優化] 防手震
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummaryCard(totalAmount: Int, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface), // [修正] 卡片色
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("本日總支出", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$ $totalAmount", fontSize = 32.sp, fontWeight = FontWeight.Light, color = AppTheme.colors.textPrimary)
            }
            Surface(
                color = AppTheme.colors.background, // [修正] 標籤背景
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "$count 筆",
                    fontSize = 12.sp,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun JapaneseDetailItem(
    expense: ExpenseEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface) // [修正] 列表項目背景
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左側資訊
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(10.dp).background(getCategoryColorDot(expense.category), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(expense.category, color = AppTheme.colors.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    expense.note,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        // 右側金額與刪除
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("- $${expense.amount}", color = AppTheme.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "刪除",
                    tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f), // [修正] 刪除鈕顏色
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 輔助函式：取得分類顏色 (圓點)
private fun getCategoryColorDot(category: String): Color {
    return when(category) {
        "飲食" -> Color(0xFFFFAB91)
        "購物" -> Color(0xFF90CAF9)
        "交通" -> Color(0xFFFFF59D)
        "娛樂" -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}