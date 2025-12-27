package com.example.budgetquest.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// 日系配色
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)

// 基礎色 (用於動態調整)
private val BaseGreen = Color(0xFFA5D6A7) // 淺綠
private val DeepGreen = Color(0xFF66BB6A) // 深綠
private val BaseRed = Color(0xFFEF9A9A)   // 淺紅
private val DeepRed = Color(0xFFEF5350)   // 深紅
private val BaseYellow = Color(0xFFFFF59D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddExpenseClick: () -> Unit,
    onDayClick: (Long) -> Unit,
    onSummaryClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onEditPlanClick: (Int?) -> Unit,
    onEmptyDateClick: (Long) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val titleFormatter = SimpleDateFormat("yyyy 年 M 月", Locale.getDefault())
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (uiState.viewMode == ViewMode.Calendar) {
                        "${uiState.currentYear} / ${uiState.currentMonth + 1}"
                    } else {
                        if (uiState.activePlan != null) titleFormatter.format(Date(uiState.activePlan!!.startDate)) else "Budget Quest"
                    }

                    if (uiState.viewMode == ViewMode.Calendar) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.prevMonth() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = JapaneseTextPrimary) }
                            Text(titleText, color = JapaneseTextPrimary, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                            IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = JapaneseTextPrimary) }
                        }
                    } else {
                        Text(titleText, color = JapaneseTextPrimary, fontWeight = FontWeight.Medium, fontSize = 20.sp)
                    }
                },
                actions = {
                    val iconTint = JapaneseTextSecondary
                    // 切換模式按鈕 (固定在最左)
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(if (uiState.viewMode == ViewMode.Focus) Icons.Default.DateRange else Icons.Default.Face, null, tint = iconTint)
                    }

                    if (uiState.viewMode == ViewMode.Calendar) {
                        // --- 月曆模式 ---
                        // 1. 歷史紀錄
                        IconButton(onClick = onHistoryClick) { Icon(Icons.Default.History, null, tint = iconTint) }

                        // [已調換] 2. 新增計畫 (原本在最後)
                        IconButton(onClick = { onEditPlanClick(null) }) { Icon(Icons.Default.Add, null, tint = iconTint) }

                        // [已調換] 3. 設定 (原本在中間)
                        IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null, tint = iconTint) }

                    } else {
                        // --- 專注模式 ---
                        // 1. 訂閱管理
                        IconButton(onClick = onSubscriptionClick) { Icon(Icons.Default.Star, null, tint = iconTint) }

                        // [已調換] 2. 編輯計畫 (原本在最後)
                        IconButton(onClick = { onEditPlanClick(uiState.activePlan?.id) }) { Icon(Icons.Default.Edit, null, tint = iconTint) }

                        // [已調換] 3. 消費紀錄/總覽 (原本在中間)
                        IconButton(onClick = onSummaryClick) { Icon(Icons.AutoMirrored.Filled.List, null, tint = iconTint) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        },
        floatingActionButton = {
            if (uiState.viewMode == ViewMode.Focus) {
                FloatingActionButton(
                    onClick = onAddExpenseClick,
                    containerColor = JapaneseAccent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.viewMode == ViewMode.Focus) {
                JapaneseStatusHeader(uiState.todayAvailable, uiState.isExpired)
                Spacer(modifier = Modifier.height(24.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(uiState.dailyStates) { index, dayState ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(durationMillis = 300, delayMillis = index * 10)
                        ) + fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 10))
                    ) {
                        JapaneseDayGridItem(
                            dayState = dayState,
                            // [修改] 只有在 Focus 模式才顯示金額
                            showBalance = uiState.viewMode == ViewMode.Focus,
                            onClick = { date ->
                                if (uiState.viewMode == ViewMode.Calendar) {
                                    if (dayState.baseLimit > 0) viewModel.selectPlanByDate(date) else onEmptyDateClick(date)
                                } else {
                                    onDayClick(date)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// [修改 1] 金額跳動動畫
@Composable
fun JapaneseStatusHeader(amount: Int, isExpired: Boolean) {
    val isPositive = amount >= 0
    val titleText = if (isExpired) "計畫結餘" else "今日可用額度"

    // [新增] 數字動畫 (1秒內完成)
    val animatedAmount by rememberAnimatedNumber(amount)

    val statusText = if (isExpired) {
        if (isPositive) " 達成目標 🎉 " else " 超出預算 💸 "
    } else {
        if (isPositive) " 資金充裕 " else " 注意節流 "
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titleText, style = MaterialTheme.typography.bodyMedium, color = JapaneseTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$ $animatedAmount", // 顯示動畫數值
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = if (isPositive) JapaneseTextPrimary else Color(0xFFEF9A9A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = if (isPositive) BaseGreen.copy(alpha = 0.3f) else BaseRed.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            ) {
                Text(text = statusText, fontSize = 12.sp, color = JapaneseTextPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }
}

// [修改 2] 動態顏色與數字跳動
@Composable
fun JapaneseDayGridItem(
    dayState: DailyState,
    showBalance: Boolean, // [新增參數]
    onClick: (Long) -> Unit
) {

    // 1. 計算顏色強度
    val backgroundColor = when (dayState.status) {
        DayStatus.Future -> Color.Transparent
        DayStatus.Neutral -> BaseYellow
        DayStatus.Success -> {
            // 餘額越多，綠色越深
            // 比例：餘額 / 基礎額度 (若 > 100% 則最深)
            val ratio = if (dayState.baseLimit > 0)
                (dayState.balance.toFloat() / dayState.baseLimit).coerceIn(0f, 1.5f)
            else 0f
            // 顏色插值 (BaseGreen -> DeepGreen)
            interpolateColor(BaseGreen, DeepGreen, ratio / 1.5f)
        }
        DayStatus.Fail -> {
            // 透支越多，紅色越深
            val ratio = if (dayState.baseLimit > 0)
                (abs(dayState.balance).toFloat() / dayState.baseLimit).coerceIn(0f, 2f)
            else 0f
            interpolateColor(BaseRed, DeepRed, ratio / 2f)
        }
    }

    // 2. 數字動畫
    val animatedBalance by rememberAnimatedNumber(dayState.balance)

    val shape = RoundedCornerShape(12.dp)
    val borderModifier = if (dayState.isToday) Modifier.border(1.dp, JapaneseTextSecondary, shape) else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(backgroundColor)
            .then(borderModifier)
            .clickable { onClick(dayState.date) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayState.dayOfMonth.toString(),
                color = if (dayState.status == DayStatus.Future) Color.LightGray else JapaneseTextPrimary,
                fontWeight = if (dayState.isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            // 顯示結餘 (只在非未來日期顯示)
            if (showBalance && dayState.status != DayStatus.Future) {
                Text(
                    text = "$animatedBalance",
                    fontSize = 9.sp,
                    color = JapaneseTextPrimary.copy(alpha = 0.6f)
                )
            }

        }
    }
}

// 輔助函式：顏色插值
fun interpolateColor(start: Color, end: Color, fraction: Float): Color {
    val startArgb = start.toArgb()
    val endArgb = end.toArgb()
    return Color(ColorUtils.blendARGB(startArgb, endArgb, fraction))
}

@Composable
fun rememberAnimatedNumber(target: Int): State<Int> {
    val animatable = remember { androidx.compose.animation.core.Animatable(target.toFloat()) }
    val result = remember { mutableIntStateOf(target) }

    LaunchedEffect(target) {
        // 如果當前數值與目標差距過大（例如從 0 到 1500，通常是初始化），直接 Snap
        // 或者我們可以簡單判斷：如果是第一次賦值 (假設初始是 0 或 -1)，直接 Snap
        // 但這裡最穩的做法是：如果 animatable 的值是初始預設值(例如 0)，且目標不是 0，我們就視為初始化

        // 邏輯優化：
        // 如果是第一次 composition，Animatable 會是初始值。
        // 我們希望第一次直接到位。

        // 這裡使用一個 trick：比較當前值與目標值
        // 但由於 compose 重組特性，我們直接用 snapTo 當作初始化
        // 為了區分 "初始化" 和 "變更"，我們可以用一個 Boolean
    }

    // 更簡單的寫法：
    // 使用 animateIntAsState 但控制 animationSpec
    // 如果是第一次，spec = snap()，否則 tween()

    var isFirstLaunch by remember { mutableStateOf(true) }

    val animationState = animateIntAsState(
        targetValue = target,
        animationSpec = if (isFirstLaunch) {
            androidx.compose.animation.core.snap()
        } else {
            tween(durationMillis = 800)
        },
        finishedListener = { isFirstLaunch = false } // 動畫(或snap)結束後，標記為非第一次
    )

    // 強制在第一次 recomposition 後就將 isFirstLaunch 設為 false，確保後續變動都有動畫
    // 但因為 snap() 也是一種動畫，finishedListener 會被呼叫。
    // 為了保險，我們加一個 SideEffect
    SideEffect {
        if (isFirstLaunch && animationState.value == target) {
            isFirstLaunch = false
        }
    }

    return animationState
}
// 請確認有這個變數，如果沒有，請補上
private val JapaneseAccent = Color(0xFF78909C)