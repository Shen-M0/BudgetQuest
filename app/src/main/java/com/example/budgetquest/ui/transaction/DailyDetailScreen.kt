package com.example.budgetquest.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch // [新增]
import androidx.compose.material.icons.filled.Close // [新增]
import androidx.compose.runtime.rememberCoroutineScope

// 日系配色
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(
    date: Long,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    budgetRepository: BudgetRepository
) {
    val expenses by produceState<List<ExpenseEntity>>(initialValue = emptyList(), key1 = date) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        budgetRepository.getExpensesByRangeStream(start, end).collect { value = it }
    }

    val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val scope = rememberCoroutineScope() // [新增 scope]

    Scaffold(
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = { Text(dateFormatter.format(Date(date)), color = JapaneseTextPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = JapaneseTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = JapaneseAccent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "新增")
            }
        }
    ) { innerPadding ->
        if (expenses.isEmpty()) {
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = JapaneseTextSecondary.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("這天沒有消費紀錄", color = JapaneseTextSecondary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("享受無支出的美好一天？", color = JapaneseTextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(expenses) { expense ->
                    // 復用 JapaneseExpenseItem 的樣式，但改為可點擊
                    JapaneseDetailItem(
                        expense = expense,
                        onClick = { onItemClick(expense.id) },
                        onDelete = { // [新增刪除邏輯]
                            scope.launch {
                                budgetRepository.deleteExpense(expense)
                            }
                        }
                    )
                }
            }
        }
    }
}

// 每日詳情專用的 Item，不含刪除鈕，但可點擊
// [修改] JapaneseDetailItem 增加刪除按鈕
@Composable
fun JapaneseDetailItem(
    expense: ExpenseEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit // [新增參數]
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JapaneseSurface)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), // 調整 Padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左側資訊
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(10.dp).background(getCategoryColorDot(expense.category), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(expense.category, color = JapaneseTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(expense.note, color = JapaneseTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 右側金額與刪除
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("- $${expense.amount}", color = JapaneseTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))

            // [新增] 刪除按鈕
            IconButton(
                onClick = { onDelete() }, // 這裡的 onDelete 會觸發上面定義的 Repository 操作
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close, // 使用 X 圖示
                    contentDescription = "刪除",
                    tint = Color(0xFFE0E0E0), // 淺灰色，避免太搶眼
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getCategoryColorDot(category: String): Color {
    return when(category) {
        "飲食" -> Color(0xFFFFAB91)
        "購物" -> Color(0xFF90CAF9)
        "交通" -> Color(0xFFFFF59D)
        "娛樂" -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}