package com.example.budgetquest.ui.transaction

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
import androidx.compose.ui.res.stringResource // 確保有 import 這行
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartNote
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    return Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.textPrimary.copy(alpha = 0.25f),
            AppTheme.colors.textPrimary.copy(alpha = 0.10f)
        )
    )
}

// [美術] 玻璃圓形按鈕容器
@Composable
fun GlassIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// [美術] 極光懸浮按鈕 (Aurora FAB)
@Composable
fun AuroraFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val gradientColors = if (isDark) {
        listOf(
            AppTheme.colors.accent.copy(alpha = 0.4f),
            AppTheme.colors.accent.copy(alpha = 0.7f)
        )
    } else {
        listOf(
            AppTheme.colors.accent.copy(alpha = 0.8f),
            AppTheme.colors.accent
        )
    }

    val gradientBrush = Brush.linearGradient(colors = gradientColors)
    val shadowColor = AppTheme.colors.accent.copy(alpha = if (isDark) 0.5f else 1f)

    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = if (isDark) 8.dp else 12.dp,
                shape = CircleShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush, CircleShape)
                .border(1.dp, Color.White.copy(alpha = if (isDark) 0.15f else 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = if (isDark) 0.9f else 1f), modifier = Modifier.size(28.dp))
        }
    }
}

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

    val expenses by viewModel.expenses.collectAsState()

    // UI 層直接計算總金額
    val totalAmount = remember(expenses) { expenses.sumOf { it.amount } }

    // [修改] 使用資源檔定義的日期格式 (建議在 strings.xml 定義 format_date_standard: "yyyy/MM/dd")
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
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(CircleShape)
                            .clickable { debounce(onBackClick) }
                    ) {
                        GlassIconContainer {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                        }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize()
        ) {
            TotalAmountCard(totalAmount)
            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    JapaneseTransactionItem(
                        expense = expense,
                        onClick = { debounce { onItemClick(expense.id) } },
                        onDelete = { debounce { viewModel.deleteExpense(expense) } }
                    )
                }
            }
        }
    }
}

@Composable
fun TotalAmountCard(totalAmount: Int) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // [修改] 使用 stringResource 替換 "總支出"
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
    onDelete: () -> Unit
) {
    val categoryColor = getCategoryColorDot(expense.category)

    // [應用修正 1] 使用智慧翻譯取得分類名稱
    val displayCategory = getSmartCategoryName(expense.category)

    // [應用修正 2 & 3] 使用智慧翻譯取得備註 (含自動扣款後綴處理)
    val displayNote = getSmartNote(expense.note)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.7f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                // [顯示] 使用翻譯後的分類
                Text(text = displayCategory, color = AppTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                // [顯示] 使用處理後的備註 (自動翻譯後綴)
                Text(text = displayNote, color = AppTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // 右側內容群組 (保持不變)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.amount_negative_format, expense.amount),
                color = AppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
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

private fun getCategoryColorDot(rawCategory: String): Color {
    return when {
        rawCategory in listOf("飲食", "餐饮", "Food", "食事", "cat_food") -> Color(0xFFFFAB91)
        rawCategory in listOf("購物", "购物", "Shopping", "買い物", "cat_shopping") -> Color(0xFF90CAF9)
        rawCategory in listOf("交通", "Transport", "cat_transport") -> Color(0xFFFFF59D)
        rawCategory in listOf("娛樂", "娱乐", "Entertainment", "エンタメ", "cat_entertainment") -> Color(0xFFCE93D8)
        else -> Color(0xFFE0E0E0)
    }
}