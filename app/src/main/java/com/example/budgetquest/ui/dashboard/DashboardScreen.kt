package com.example.budgetquest.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.CoachMarkOverlay
import com.example.budgetquest.ui.common.CoachMarkPosition
import com.example.budgetquest.ui.common.CoachMarkTarget
import com.example.budgetquest.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    initialPlanId: Int? = null,
    isTutorialMode: Boolean = false,
    onTutorialFinished: () -> Unit = {},
    onAddExpenseClick: (Long) -> Unit,
    onDayClick: (Long) -> Unit,
    onSummaryClick: (Int?) -> Unit,
    onSubscriptionClick: (Int, Long, Long) -> Unit,
    onEditPlanClick: (Int?) -> Unit,
    onEmptyDateClick: (Long, Long) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // [優化] 防手震機制
    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val gridHeight = remember(screenWidth) {
        val totalPadding = 32.dp
        val itemWidth = (screenWidth - totalPadding) / 7
        val gap = 8.dp
        (itemWidth * 6) + (gap * 5) + 16.dp
    }

    var coachMarkStep by remember { mutableStateOf(0) }
    var isTutorialReady by remember { mutableStateOf(false) }

    var focusToggleBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var calendarToggleBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var subBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var editBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var listBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var fabCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var statusCardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var historyBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var addPlanBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var settingsBtnCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var cardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(initialPlanId) {
        if (initialPlanId != null && initialPlanId != -1) {
            viewModel.selectPlanById(initialPlanId)
        }
    }

    LaunchedEffect(isTutorialMode, uiState.activePlan) {
        if (isTutorialMode && uiState.activePlan != null) {
            snapshotFlow { uiState.activePlan }.filterNotNull().first()

            if (uiState.viewMode == ViewMode.Calendar) {
                viewModel.toggleViewMode()
                snapshotFlow { uiState.viewMode }.filter { it == ViewMode.Focus }.first()
            }

            delay(300)
            coachMarkStep = 1
            isTutorialReady = true
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.viewMode == ViewMode.Focus) {
                        Text(
                            text = "${uiState.currentYear} 年 ${uiState.currentMonth + 1} 月",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { debounce { viewModel.prevMonth() } }) { // [優化] 防手震
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                            }
                            Text(
                                text = "${uiState.currentYear} / ${uiState.currentMonth + 1}",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(onClick = { debounce { viewModel.nextMonth() } }) { // [優化] 防手震
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AppTheme.colors.textPrimary)
                            }
                        }
                    }
                },
                actions = {
                    val iconTint = AppTheme.colors.textSecondary

                    if (uiState.viewMode == ViewMode.Focus) {
                        IconButton(
                            onClick = { debounce { viewModel.toggleViewMode() } }, // [優化] 防手震
                            modifier = Modifier.onGloballyPositioned { focusToggleBtnCoords = it }
                        ) {
                            Icon(Icons.Default.DateRange, "切換至月曆", tint = iconTint)
                        }
                        IconButton(
                            onClick = {
                                debounce { // [優化] 防手震
                                    val plan = uiState.activePlan
                                    if (plan != null) {
                                        onSubscriptionClick(plan.id, plan.startDate, plan.endDate)
                                    }
                                }
                            },
                            modifier = Modifier.onGloballyPositioned { subBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Star, "訂閱", tint = iconTint)
                        }
                        if (uiState.activePlan != null) {
                            IconButton(
                                onClick = { debounce { onEditPlanClick(uiState.activePlan?.id) } }, // [優化] 防手震
                                modifier = Modifier.onGloballyPositioned { editBtnCoords = it }
                            ) {
                                Icon(Icons.Default.Edit, "編輯計畫", tint = iconTint)
                            }
                            IconButton(
                                onClick = { debounce { onSummaryClick(uiState.activePlan?.id) } }, // [優化] 防手震
                                modifier = Modifier.onGloballyPositioned { listBtnCoords = it }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, "詳細清單", tint = iconTint)
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { debounce { viewModel.toggleViewMode() } }, // [優化] 防手震
                            modifier = Modifier.onGloballyPositioned { calendarToggleBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Face, "切換至專注", tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onHistoryClick() } }, // [優化] 防手震
                            modifier = Modifier.onGloballyPositioned { historyBtnCoords = it }
                        ) {
                            Icon(Icons.Default.History, "歷史紀錄", tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onEditPlanClick(null) } }, // [優化] 防手震
                            modifier = Modifier.onGloballyPositioned { addPlanBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Add, "新增計畫", tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onSettingsClick() } }, // [優化] 防手震
                            modifier = Modifier.onGloballyPositioned { settingsBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Settings, "設定", tint = iconTint)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    debounce { // [優化] 防手震
                        val targetDate = if (uiState.activePlan != null && uiState.isExpired) {
                            uiState.activePlan!!.startDate
                        } else {
                            System.currentTimeMillis()
                        }
                        onAddExpenseClick(targetDate)
                    }
                },
                containerColor = AppTheme.colors.accent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.onGloballyPositioned { fabCoords = it }
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.viewMode == ViewMode.Focus && uiState.activePlan != null) {
                Box(modifier = Modifier.onGloballyPositioned { statusCardCoords = it }) {
                    DashboardStatusCard(
                        todayAvailable = uiState.todayAvailable,
                        isExpired = uiState.isExpired
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                ) {
                    Box(modifier = Modifier.onGloballyPositioned { cardCoords = it }) {
                        WeekHeader()
                    }

                    if (uiState.dailyStates.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (uiState.viewMode == ViewMode.Focus && uiState.activePlan == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("目前沒有進行中的計畫", color = AppTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            debounce { // [優化] 防手震
                                                onEmptyDateClick(System.currentTimeMillis(), System.currentTimeMillis() + 86400000L * 30)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent)
                                    ) {
                                        Text("建立新計畫", color = Color.White)
                                    }
                                }
                            } else {
                                CircularProgressIndicator(color = AppTheme.colors.accent)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(bottom = 64.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(uiState.dailyStates) { index, dayState ->
                                if (dayState.status == DayStatus.Empty) {
                                    Box(modifier = Modifier.aspectRatio(1f))
                                } else {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        JapaneseDayGridItem(
                                            dayState = dayState,
                                            showBalance = uiState.viewMode == ViewMode.Focus,
                                            onClick = { date ->
                                                // [優化] 格子點擊防手震
                                                debounce {
                                                    if (uiState.viewMode == ViewMode.Calendar) {
                                                        if (dayState.baseLimit > 0 || dayState.status == DayStatus.Success || dayState.status == DayStatus.Fail) {
                                                            viewModel.selectPlanByDate(date)
                                                        } else {
                                                            scope.launch {
                                                                val (start, end) = viewModel.calculateSmartDates(date)
                                                                onEmptyDateClick(start, end)
                                                            }
                                                        }
                                                    } else {
                                                        onDayClick(date)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 教學引導 Overlay ---
    if (isTutorialMode && isTutorialReady && coachMarkStep > 0) {
        val target = when (coachMarkStep) {
            1 -> focusToggleBtnCoords?.let { CoachMarkTarget(it, "模式切換", "點擊切換至「月曆模式」，\n查看整月概況。", position = CoachMarkPosition.Bottom) }
            2 -> subBtnCoords?.let { CoachMarkTarget(it, "固定訂閱", "設定房租、Netflix 等固定支出，\n系統自動記帳。", position = CoachMarkPosition.Bottom) }
            3 -> editBtnCoords?.let { CoachMarkTarget(it, "編輯計畫", "修改當前計畫的預算或日期。", position = CoachMarkPosition.Bottom) }
            4 -> listBtnCoords?.let { CoachMarkTarget(it, "詳細清單", "查看所有消費紀錄與報表分析。", position = CoachMarkPosition.Bottom) }
            5 -> fabCoords?.let { CoachMarkTarget(it, "記一筆", "快速記錄消費。\n長按可補記其他日期。", position = CoachMarkPosition.Top) }
            6 -> statusCardCoords?.let { CoachMarkTarget(it, "今日額度", "顯示今天還能花多少錢。\n綠色安全，紅色超支。", isCircle = false, position = CoachMarkPosition.Bottom) }
            7 -> cardCoords?.let { CoachMarkTarget(it, "每日狀態", "月曆顯示每天的結餘狀況。\n「綠色」表示省錢成功。\n「紅色」表示透支消費。", isCircle = false, position = CoachMarkPosition.Bottom, extraHeight = gridHeight) }
            8 -> {
                LaunchedEffect(Unit) {
                    viewModel.toggleViewMode()
                    delay(500)
                    coachMarkStep = 9
                }
                null
            }
            9 -> calendarToggleBtnCoords?.let { CoachMarkTarget(it, "模式切換", "點擊切換回「專注模式」，\n控制每日預算。", position = CoachMarkPosition.Bottom) }
            10 -> historyBtnCoords?.let { CoachMarkTarget(it, "歷史回顧", "查看過去所有計畫的執行成果。", position = CoachMarkPosition.Bottom) }
            11 -> addPlanBtnCoords?.let { CoachMarkTarget(it, "新增計畫", "舊計畫結束後，在此建立新計畫。", position = CoachMarkPosition.Bottom) }
            12 -> settingsBtnCoords?.let { CoachMarkTarget(it, "設定", "深色模式、提醒通知與資料備份。", position = CoachMarkPosition.Bottom) }
            13 -> cardCoords?.let { CoachMarkTarget(it, "月曆總覽", "點擊「綠色、紅色」日期，\n進入當日計畫的消費。\n點擊「空白」日期快速建立計畫。", isCircle = false, position = CoachMarkPosition.Bottom, extraHeight = gridHeight) }
            else -> null
        }

        if (target == null && coachMarkStep != 8 && coachMarkStep <= 13) {
            LaunchedEffect(Unit) { coachMarkStep++ }
        }

        if (target != null) {
            CoachMarkOverlay(
                target = target,
                onNext = { coachMarkStep++ },

            )
        } else if (coachMarkStep > 13) {
            coachMarkStep = 0
            isTutorialReady = false
            if (uiState.viewMode == ViewMode.Calendar) viewModel.toggleViewMode()
            onTutorialFinished()
        }
    }
}

// 數字跳動動畫元件
@Composable
fun RollingNumberText(
    targetValue: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color
) {
    var lastValue by rememberSaveable { mutableIntStateOf(-1) }
    var isReadyToAnimate by rememberSaveable { mutableStateOf(false) }

    val startValue = if (lastValue != -1) lastValue.toFloat() else targetValue.toFloat()
    val animatable = remember { Animatable(startValue) }

    LaunchedEffect(Unit) {
        if (!isReadyToAnimate) {
            delay(500)
            isReadyToAnimate = true
        }
    }

    LaunchedEffect(targetValue) {
        if (isReadyToAnimate) {
            animatable.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            animatable.snapTo(targetValue.toFloat())
        }
        lastValue = targetValue
    }

    Text(
        text = "$ ${animatable.value.toInt()}",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}

// ... 輔助元件保持不變 ...
@Composable
fun DashboardStatusCard(todayAvailable: Int, isExpired: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isExpired) "計畫結餘" else "今日可用",
                fontSize = 14.sp,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            RollingNumberText(
                targetValue = todayAvailable,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                color = if (todayAvailable >= 0) AppTheme.colors.success else AppTheme.colors.fail
            )
        }
    }
}

@Composable
fun WeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val days = listOf("日", "一", "二", "三", "四", "五", "六")
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun JapaneseDayGridItem(
    dayState: DailyState,
    showBalance: Boolean,
    onClick: (Long) -> Unit
) {
    val backgroundColor = when (dayState.status) {
        DayStatus.Success -> AppTheme.colors.success.copy(alpha = 0.15f)
        DayStatus.Fail -> AppTheme.colors.fail.copy(alpha = 0.15f)
        DayStatus.Future -> AppTheme.colors.surface
        else -> AppTheme.colors.surface
    }

    val textColor = when (dayState.status) {
        DayStatus.Success -> AppTheme.colors.success
        DayStatus.Fail -> AppTheme.colors.fail
        else -> AppTheme.colors.textSecondary
    }

    val borderModifier = if (dayState.isToday) {
        Modifier.border(1.5.dp, AppTheme.colors.accent, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable { onClick(dayState.date) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayState.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (dayState.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (dayState.isToday) AppTheme.colors.accent else AppTheme.colors.textPrimary
        )

        if (showBalance && dayState.status != DayStatus.Future) {
            Text(
                text = "${dayState.balance}",
                fontSize = 10.sp,
                color = textColor,
                maxLines = 1
            )
        }
    }
}