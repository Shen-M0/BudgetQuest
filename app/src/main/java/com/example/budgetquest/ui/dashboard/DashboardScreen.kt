package com.example.budgetquest.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val calendarAnchorYear = 1900

    val pagerConfig = remember(uiState.activePlan, uiState.viewMode) {
        if (uiState.viewMode == ViewMode.Focus && uiState.activePlan != null) {
            val plan = uiState.activePlan!!
            val startCal = Calendar.getInstance().apply { timeInMillis = plan.startDate }
            val endCal = Calendar.getInstance().apply { timeInMillis = plan.endDate }
            val yearDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
            val monthDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH) + (yearDiff * 12)
            Triple(monthDiff + 1, startCal, false)
        } else {
            val totalMonths = 4800
            val startCal = Calendar.getInstance().apply {
                set(calendarAnchorYear, 0, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            Triple(totalMonths, startCal, true)
        }
    }
    val (totalPageCount, startCalForPager, isInfiniteMode) = pagerConfig

    val targetPageIndex = remember(uiState.currentYear, uiState.currentMonth, pagerConfig) {
        if (isInfiniteMode) {
            val yearDiff = uiState.currentYear - calendarAnchorYear
            val monthDiff = (yearDiff * 12) + uiState.currentMonth
            monthDiff.coerceIn(0, totalPageCount - 1)
        } else {
            val currentCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, uiState.currentYear)
                set(Calendar.MONTH, uiState.currentMonth)
            }
            val planStartCal = startCalForPager
            val yearDiff = currentCal.get(Calendar.YEAR) - planStartCal.get(Calendar.YEAR)
            val monthDiff = currentCal.get(Calendar.MONTH) - planStartCal.get(Calendar.MONTH) + (yearDiff * 12)
            monthDiff.coerceIn(0, maxOf(0, totalPageCount - 1))
        }
    }

    val pagerState = rememberPagerState(
        initialPage = targetPageIndex,
        pageCount = { totalPageCount }
    )

    LaunchedEffect(targetPageIndex) {
        if (pagerState.currentPage != targetPageIndex) {
            pagerState.scrollToPage(targetPageIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val newCal = (startCalForPager.clone() as Calendar).apply { add(Calendar.MONTH, pagerState.currentPage) }
        val targetYear = newCal.get(Calendar.YEAR)
        val targetMonth = newCal.get(Calendar.MONTH)
        if (targetYear != uiState.currentYear || targetMonth != uiState.currentMonth) {
            viewModel.updateMonth(targetYear, targetMonth)
        }
    }

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
        if (initialPlanId != null && initialPlanId != -1) viewModel.selectPlanById(initialPlanId)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        // [Issue 1 解決] 移除 end padding，改用自然排列
                    ) {
                        if (uiState.viewMode == ViewMode.Calendar) {
                            IconButton(
                                onClick = { debounce { viewModel.prevMonth() } },
                                // [Issue 1 解決] 將左按鈕向左偏移，固定位置與大小
                                modifier = Modifier.offset(x = (-8).dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                            }
                        } else {
                            // 專注模式補位，保持文字對齊 (Optional，這裡不加也可以)
                        }

                        Text(
                            text = if (uiState.viewMode == ViewMode.Focus) {
                                "${uiState.currentYear}年 ${uiState.currentMonth + 1}月"
                            } else {
                                "${uiState.currentYear} / ${uiState.currentMonth + 1}"
                            },
                            color = AppTheme.colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            // [Issue 1 解決] 微調文字邊距
                            modifier = Modifier.padding(start = if (uiState.viewMode == ViewMode.Calendar) 0.dp else 16.dp)
                        )

                        if (uiState.viewMode == ViewMode.Calendar) {
                            IconButton(onClick = { debounce { viewModel.nextMonth() } }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AppTheme.colors.textPrimary)
                            }
                        }
                    }
                },
                actions = {
                    val iconTint = AppTheme.colors.textSecondary
                    if (uiState.viewMode == ViewMode.Focus) {
                        IconButton(
                            onClick = { debounce { viewModel.toggleViewMode() } },
                            modifier = Modifier.onGloballyPositioned { focusToggleBtnCoords = it }
                        ) {
                            Icon(Icons.Default.DateRange, "切換至月曆", tint = iconTint)
                        }
                        IconButton(
                            onClick = {
                                debounce {
                                    val plan = uiState.activePlan
                                    if (plan != null) onSubscriptionClick(plan.id, plan.startDate, plan.endDate)
                                }
                            },
                            modifier = Modifier.onGloballyPositioned { subBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Star, "訂閱", tint = iconTint)
                        }
                        if (uiState.activePlan != null) {
                            IconButton(
                                onClick = { debounce { onEditPlanClick(uiState.activePlan?.id) } },
                                modifier = Modifier.onGloballyPositioned { editBtnCoords = it }
                            ) {
                                Icon(Icons.Default.Edit, "編輯計畫", tint = iconTint)
                            }
                            IconButton(
                                onClick = { debounce { onSummaryClick(uiState.activePlan?.id) } },
                                modifier = Modifier.onGloballyPositioned { listBtnCoords = it }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, "詳細清單", tint = iconTint)
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { debounce { viewModel.toggleViewMode() } },
                            modifier = Modifier.onGloballyPositioned { calendarToggleBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Face, "切換至專注", tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onHistoryClick() } },
                            modifier = Modifier.onGloballyPositioned { historyBtnCoords = it }
                        ) {
                            Icon(Icons.Default.History, "歷史紀錄", tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onEditPlanClick(null) } },
                            modifier = Modifier.onGloballyPositioned { addPlanBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Add, "新增計畫", tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onSettingsClick() } },
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
                    debounce {
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

                    if (uiState.dailyStates.isEmpty() && uiState.activePlan == null) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (uiState.viewMode == ViewMode.Focus && uiState.activePlan == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("目前沒有進行中的計畫", color = AppTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { debounce { onEmptyDateClick(System.currentTimeMillis(), System.currentTimeMillis() + 86400000L * 30) } },
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
                        Box(modifier = Modifier.weight(1f)) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = true,
                                modifier = Modifier.fillMaxSize()
                            ){ page ->
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    contentPadding = PaddingValues(bottom = 64.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // [修復閃退] 確保 Key 唯一性：日期 + 狀態 + 索引 (針對 Empty)
                                    itemsIndexed(
                                        uiState.dailyStates,
                                        key = { index, dayState ->
                                            // 如果是 Empty，加上 index 來區分多個空白格子
                                            if (dayState.status == DayStatus.Empty) {
                                                "empty_$index"
                                            } else {
                                                "${dayState.date}_${dayState.status}"
                                            }
                                        }
                                    ) { index, dayState ->
                                        if (dayState.status == DayStatus.Empty) {
                                            Box(modifier = Modifier.aspectRatio(1f))
                                        } else {
                                            JapaneseDayGridItem(
                                                dayState = dayState,
                                                showBalance = uiState.viewMode == ViewMode.Focus,
                                                onClick = { date ->
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
                                                            if (dayState.status != DayStatus.Neutral) {
                                                                onDayClick(date)
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.viewMode == ViewMode.Focus && totalPageCount > 1) {
                                if (pagerState.currentPage > 0) {
                                    IconButton(
                                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                                        modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            null,
                                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                if (pagerState.currentPage < totalPageCount - 1) {
                                    IconButton(
                                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                                        modifier = Modifier.align(Alignment.CenterEnd).offset(x = 12.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            null,
                                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
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
    // ... Tutorial Overlay ...
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
                onSkip = {
                    coachMarkStep = 0
                    isTutorialReady = false
                    if (uiState.viewMode == ViewMode.Calendar) viewModel.toggleViewMode()
                    onTutorialFinished()
                }
            )
        } else if (coachMarkStep > 13) {
            coachMarkStep = 0
            isTutorialReady = false
            if (uiState.viewMode == ViewMode.Calendar) viewModel.toggleViewMode()
            onTutorialFinished()
        }
    }
}

// ... RollingNumberText, DashboardStatusCard, WeekHeader, JapaneseDayGridItem ...
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
    showBalance: Boolean, // 這個參數剛好可以用來區分模式 (Focus=true, Calendar=false)
    onClick: (Long) -> Unit
) {
    val backgroundColor = when (dayState.status) {
        DayStatus.Success -> AppTheme.colors.success.copy(alpha = 0.15f)
        DayStatus.Fail -> AppTheme.colors.fail.copy(alpha = 0.15f)
        DayStatus.Neutral -> Color.Gray.copy(alpha = 0.1f)
        DayStatus.Future -> Color.Gray.copy(alpha = 0.1f)
        else -> AppTheme.colors.surface
    }

    val textColor = when (dayState.status) {
        DayStatus.Success -> AppTheme.colors.success
        DayStatus.Fail -> AppTheme.colors.fail
        DayStatus.Neutral -> AppTheme.colors.textSecondary.copy(alpha = 0.5f)
        DayStatus.Future -> AppTheme.colors.textSecondary.copy(alpha = 0.5f)
        else -> AppTheme.colors.textSecondary
    }

    val borderModifier = if (dayState.isToday) {
        Modifier.border(1.5.dp, AppTheme.colors.accent, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    // [關鍵修正] 決定是否啟用點擊
    // 邏輯：
    // 1. 如果不是 Neutral (正常日期) -> 永遠可點
    // 2. 如果是 Neutral (灰色日期) -> 只有在 "不顯示餘額" (即月曆模式) 時才可點
    // 這樣專注模式的補位空白依然不可點，但月曆模式的空白日期可以點擊建立計畫
    val isClickable = dayState.status != DayStatus.Neutral || !showBalance

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(enabled = isClickable) { onClick(dayState.date) } // 使用新的判斷邏輯
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayState.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (dayState.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (dayState.isToday) AppTheme.colors.accent else textColor
        )

        if (showBalance && dayState.status != DayStatus.Future && dayState.status != DayStatus.Neutral) {
            Text(
                text = "${dayState.balance}",
                fontSize = 10.sp,
                color = textColor,
                maxLines = 1
            )
        }
    }
}