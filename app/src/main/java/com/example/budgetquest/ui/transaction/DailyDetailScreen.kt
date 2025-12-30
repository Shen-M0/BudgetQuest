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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.budgetquest.ui.common.getSmartCategoryName // [需確認有引用]
import com.example.budgetquest.ui.common.getSmartTagName // [需確認有引用]
import androidx.compose.ui.text.style.TextOverflow // [補上遺漏的 import]

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

    // [提取] 日期格式字串
    val dateFormat = stringResource(R.string.title_date_format_detail)
    val dateFormatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }

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
                    // [提取] 返回按鈕描述 (共用 TransactionScreen 的 action_back)
                    IconButton(onClick = { debounce(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { debounce { onAddExpenseClick(uiState.date) } },
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
                    // [提取] 無紀錄提示
                    Text(stringResource(R.string.msg_no_daily_records), color = AppTheme.colors.textSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        JapaneseDetailItem(
                            expense = expense,
                            onClick = { debounce { onItemClick(expense.id) } },
                            onDelete = {
                                debounce { viewModel.deleteExpense(expense) }
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
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // [提取] 本日總支出
                Text(stringResource(R.string.label_daily_total), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                // [提取] 金額格式
                Text(stringResource(R.string.amount_currency_format, totalAmount), fontSize = 32.sp, fontWeight = FontWeight.Light, color = AppTheme.colors.textPrimary)
            }
            Surface(
                color = AppTheme.colors.background,
                shape = RoundedCornerShape(12.dp)
            ) {
                // [提取] 筆數格式
                Text(
                    stringResource(R.string.label_record_count, count),
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
            .background(AppTheme.colors.surface)
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
                // [關鍵修改] 套用多語言 Helper：分類名稱
                Text(
                    text = getSmartCategoryName(expense.category),
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                // [關鍵修改] 套用多語言 Helper：備註名稱
                Text(
                    text = getSmartTagName(expense.note),
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 右側金額與刪除
        Row(verticalAlignment = Alignment.CenterVertically) {
            // [提取] 負數金額格式
            Text(stringResource(R.string.amount_negative_format, expense.amount), color = AppTheme.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(24.dp)
            ) {
                // [提取] 刪除按鈕描述
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.content_desc_delete),
                    tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 輔助函式：取得分類顏色 (圓點) - 這裡的分類名稱通常來自 DB，暫不提取
private fun getCategoryColorDot(category: String): Color {
    return when(category) {
        "飲食" -> Color(0xFFFFAB91)
        "購物" -> Color(0xFF90CAF9)
        "交通" -> Color(0xFFFFF59D)
        "娛樂" -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}