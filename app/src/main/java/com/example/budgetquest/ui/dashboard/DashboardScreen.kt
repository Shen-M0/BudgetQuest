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
import androidx.compose.ui.res.stringArrayResource
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
    planId: Int = -1,
    targetDate: Long = -1L,
    trigger: Long = 0L,
    onConsumeNavigationArgs: () -> Unit = {}, // 確保這個函式被呼叫
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

    // [修正] UI 層也保留一個簡單的防呆，確保 trigger 改變才動作
    var lastHandledTrigger by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(planId, targetDate, trigger) {
        // 只有當 trigger 是新的，才執行
        if (trigger > 0L && trigger != lastHandledTrigger) {

            if (planId != -1) {
                // [修正] 傳入 trigger 給 ViewModel 做雙重檢查
                viewModel.setViewingPlanId(planId, trigger)
            } else if (targetDate != -1L) {
                viewModel.switchToCalendarDate(targetDate, trigger)
            }

            // 更新 UI 記錄
            lastHandledTrigger = trigger

            // [關鍵修正] 執行完畢後，立刻通知 MainActivity 清除參數 (覆寫為 -1)
            // 這能確保下次返回時，參數已經乾淨了，不會再次觸發任何邏輯
            onConsumeNavigationArgs()
        }
    }

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

    // ❌ [已刪除] 這裡原本有一個 LaunchedEffect(initialPlanId) 會繞過防呆直接跳轉
    // 現在已經移除，確保所有的跳轉都必須經過上面的 trigger 檢查

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

    // ... (Scaffold 及其後的內容保持不變，請直接複製您原本的 UI 代碼) ...
    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.viewMode == ViewMode.Calendar) {
                            IconButton(
                                onClick = { debounce { viewModel.prevMonth() } },
                                modifier = Modifier.offset(x = (-8).dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTheme.colors.textPrimary)
                            }
                        }

                        Text(
                            text = if (uiState.viewMode == ViewMode.Focus) {
                                stringResource(R.string.date_format_focus, uiState.currentYear, uiState.currentMonth + 1)
                            } else {
                                stringResource(R.string.date_format_calendar, uiState.currentYear, uiState.currentMonth + 1)
                            },
                            color = AppTheme.colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
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
                            Icon(Icons.Default.DateRange,
                                stringResource(R.string.action_switch_to_calendar), tint = iconTint)
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
                            Icon(Icons.Default.Star,
                                stringResource(R.string.action_subscribe), tint = iconTint)
                        }
                        if (uiState.activePlan != null) {
                            IconButton(
                                onClick = { debounce { onEditPlanClick(uiState.activePlan?.id) } },
                                modifier = Modifier.onGloballyPositioned { editBtnCoords = it }
                            ) {
                                Icon(Icons.Default.Edit, stringResource(R.string.action_edit_plan), tint = iconTint)
                            }
                            IconButton(
                                onClick = { debounce { onSummaryClick(uiState.activePlan?.id) } },
                                modifier = Modifier.onGloballyPositioned { listBtnCoords = it }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.action_view_list), tint = iconTint)
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { debounce { viewModel.toggleViewMode() } },
                            modifier = Modifier.onGloballyPositioned { calendarToggleBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Face,
                                stringResource(R.string.action_switch_to_focus), tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onHistoryClick() } },
                            modifier = Modifier.onGloballyPositioned { historyBtnCoords = it }
                        ) {
                            Icon(Icons.Default.History,
                                stringResource(R.string.action_view_history), tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onEditPlanClick(null) } },
                            modifier = Modifier.onGloballyPositioned { addPlanBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Add, stringResource(R.string.action_add_plan), tint = iconTint)
                        }
                        IconButton(
                            onClick = { debounce { onSettingsClick() } },
                            modifier = Modifier.onGloballyPositioned { settingsBtnCoords = it }
                        ) {
                            Icon(Icons.Default.Settings, stringResource(R.string.action_settings), tint = iconTint)
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

                    // 這裡檢查是否為空狀態
                    if (uiState.dailyStates.isEmpty() && uiState.activePlan == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.viewMode == ViewMode.Focus && uiState.activePlan == null) {
                                // [修正] 使用新的 DashboardEmptyState 元件
                                DashboardEmptyState(
                                    onCreateClick = {
                                        debounce {
                                            onEmptyDateClick(
                                                System.currentTimeMillis(),
                                                System.currentTimeMillis() + 86400000L * 30
                                            )
                                        }
                                    }
                                )
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
                                    itemsIndexed(
                                        uiState.dailyStates,
                                        key = { index, dayState ->
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
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = (-12).dp)
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
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = 12.dp)
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

    if (isTutorialMode && isTutorialReady && coachMarkStep > 0) {
        val target = when (coachMarkStep) {
            1 -> focusToggleBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_mode), stringResource(R.string.tutorial_desc_to_calendar), position = CoachMarkPosition.Bottom) }
            2 -> subBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_subscribe), stringResource(R.string.tutorial_desc_subscribe), position = CoachMarkPosition.Bottom) }
            3 -> editBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_edit), stringResource(R.string.tutorial_desc_edit), position = CoachMarkPosition.Bottom) }
            4 -> listBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_list), stringResource(R.string.tutorial_desc_list), position = CoachMarkPosition.Bottom) }
            5 -> fabCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_record), stringResource(R.string.tutorial_desc_record), position = CoachMarkPosition.Top) }
            6 -> statusCardCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_today), stringResource(R.string.tutorial_desc_today), isCircle = false, position = CoachMarkPosition.Bottom) }
            7 -> cardCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_status), stringResource(R.string.tutorial_desc_status), isCircle = false, position = CoachMarkPosition.Bottom, extraHeight = gridHeight) }
            8 -> {
                LaunchedEffect(Unit) {
                    viewModel.toggleViewMode()
                    delay(500)
                    coachMarkStep = 9
                }
                null
            }
            9 -> calendarToggleBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_mode), stringResource(R.string.tutorial_desc_to_focus), position = CoachMarkPosition.Bottom) }
            10 -> historyBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_history), stringResource(R.string.tutorial_desc_history), position = CoachMarkPosition.Bottom) }
            11 -> addPlanBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_new_plan), stringResource(R.string.tutorial_desc_new_plan), position = CoachMarkPosition.Bottom) }
            12 -> settingsBtnCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_settings), stringResource(R.string.tutorial_desc_settings), position = CoachMarkPosition.Bottom) }
            13 -> cardCoords?.let { CoachMarkTarget(it,
                stringResource(R.string.tutorial_title_calendar), stringResource(R.string.tutorial_desc_calendar), isCircle = false, position = CoachMarkPosition.Bottom, extraHeight = gridHeight) }
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

// ... RollingNumberText 與其他 Composable 保持不變，可以直接使用您原本的 ...
@Composable
fun RollingNumberText(
    targetValue: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color
) {
    var lastValue by rememberSaveable { mutableIntStateOf(targetValue) }
    val animatable = remember { Animatable(lastValue.toFloat()) }

    LaunchedEffect(targetValue) {
        if (targetValue != lastValue) {
            animatable.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
            lastValue = targetValue
        } else {
            animatable.snapTo(targetValue.toFloat())
        }
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
                if (isExpired) stringResource(R.string.label_plan_balance) else stringResource(R.string.label_available_today),
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
        val days = stringArrayResource(R.array.days_of_week)
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

    val isClickable = dayState.status != DayStatus.Neutral || !showBalance

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(enabled = isClickable) { onClick(dayState.date) }
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

@Composable
fun DashboardEmptyState(
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), // 增加邊距
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. 視覺圖示 (大圓底 + Icon)
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = AppTheme.colors.accent.copy(alpha = 0.1f), // 淡淡的強調色背景
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange, // 使用月曆圖示
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = AppTheme.colors.accent // 圖示使用強調色
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 標題
        Text(
            text = stringResource(R.string.msg_no_active_plan),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 副標題 (引導文字)
        // 這裡您可以之後抽換成 stringResource，目前先用範例文字
        Text(
            text = "建立您的第一個計畫\n開始追蹤每日支出與目標",
            fontSize = 15.sp,
            color = AppTheme.colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 4. 強化版行動按鈕
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.accent,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(50), // 全圓角
            modifier = Modifier
                .fillMaxWidth(0.7f) // 寬度設為 70%，不要太寬
                .height(56.dp), // 加高按鈕，更好點擊
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp, // 加入陰影增加立體感
                pressedElevation = 2.dp
            )
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_create_plan),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}