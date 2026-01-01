package com.example.budgetquest.ui.dashboard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.budgetquest.ui.common.AuroraFloatingActionButton
import com.example.budgetquest.ui.common.CoachMarkOverlay
import com.example.budgetquest.ui.common.CoachMarkPosition
import com.example.budgetquest.ui.common.CoachMarkTarget
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.common.FluidBoundsTransform // 引用共通動畫常數
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun DashboardScreen(
    initialPlanId: Int? = null,
    planId: Int = -1,
    targetDate: Long = -1L,
    trigger: Long = 0L,
    onConsumeNavigationArgs: () -> Unit = {},
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
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
    // 這是從 MainActivity 傳來的 Scope (用於跨頁面轉場)
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var lastHandledTrigger by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(planId, targetDate, trigger) {
        if (trigger > 0L && trigger != lastHandledTrigger) {
            if (planId != -1) {
                viewModel.setViewingPlanId(planId, trigger)
            } else if (targetDate != -1L) {
                viewModel.switchToCalendarDate(targetDate, trigger)
            }
            lastHandledTrigger = trigger
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
        containerColor = Color.Transparent,
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
                    Row(modifier = Modifier.padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.viewMode == ViewMode.Focus) {
                            GlassIconButton(
                                onClick = { debounce { viewModel.toggleViewMode() } },
                                size = 38.dp,
                                modifier = Modifier.onGloballyPositioned { focusToggleBtnCoords = it }
                            ) {
                                Icon(Icons.Default.DateRange, stringResource(R.string.action_switch_to_calendar), tint = iconTint, modifier = Modifier.size(20.dp))
                            }

                            if (uiState.activePlan != null) {
                                GlassIconButton(
                                    onClick = {
                                        debounce {
                                            val plan = uiState.activePlan
                                            if (plan != null) onSubscriptionClick(plan.id, plan.startDate, plan.endDate)
                                        }
                                    },
                                    size = 38.dp,
                                    modifier = Modifier.onGloballyPositioned { subBtnCoords = it }
                                ) {
                                    Icon(Icons.Default.Star, stringResource(R.string.action_subscribe), tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                                GlassIconButton(
                                    onClick = { debounce { onEditPlanClick(uiState.activePlan?.id) } },
                                    size = 38.dp,
                                    modifier = Modifier.onGloballyPositioned { editBtnCoords = it }
                                ) {
                                    Icon(Icons.Default.Edit, stringResource(R.string.action_edit_plan), tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                                GlassIconButton(
                                    onClick = { debounce { onSummaryClick(uiState.activePlan?.id) } },
                                    size = 38.dp,
                                    modifier = Modifier.onGloballyPositioned { listBtnCoords = it }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.action_view_list), tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                            } else {
                                GlassIconButton(
                                    onClick = { debounce { onHistoryClick() } },
                                    size = 38.dp
                                ) {
                                    Icon(Icons.Default.History, stringResource(R.string.action_view_history), tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                                GlassIconButton(
                                    onClick = { debounce { onEditPlanClick(null) } },
                                    size = 38.dp
                                ) {
                                    Icon(Icons.Default.Add, stringResource(R.string.action_add_plan), tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                                GlassIconButton(
                                    onClick = { debounce { onSettingsClick() } },
                                    size = 38.dp
                                ) {
                                    Icon(Icons.Default.Settings, stringResource(R.string.action_settings), tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                            }
                        } else {
                            GlassIconButton(
                                onClick = { debounce { viewModel.toggleViewMode() } },
                                size = 38.dp,
                                modifier = Modifier.onGloballyPositioned { calendarToggleBtnCoords = it }
                            ) {
                                Icon(Icons.Default.Face, stringResource(R.string.action_switch_to_focus), tint = iconTint, modifier = Modifier.size(20.dp))
                            }
                            GlassIconButton(
                                onClick = { debounce { onHistoryClick() } },
                                size = 38.dp,
                                modifier = Modifier.onGloballyPositioned { historyBtnCoords = it }
                            ) {
                                Icon(Icons.Default.History, stringResource(R.string.action_view_history), tint = iconTint, modifier = Modifier.size(20.dp))
                            }
                            GlassIconButton(
                                onClick = { debounce { onEditPlanClick(null) } },
                                size = 38.dp,
                                modifier = Modifier.onGloballyPositioned { addPlanBtnCoords = it }
                            ) {
                                Icon(Icons.Default.Add, stringResource(R.string.action_add_plan), tint = iconTint, modifier = Modifier.size(20.dp))
                            }
                            GlassIconButton(
                                onClick = { debounce { onSettingsClick() } },
                                size = 38.dp,
                                modifier = Modifier.onGloballyPositioned { settingsBtnCoords = it }
                            ) {
                                Icon(Icons.Default.Settings, stringResource(R.string.action_settings), tint = iconTint, modifier = Modifier.size(20.dp))
                            }
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
            if (uiState.activePlan != null) {
                AuroraFloatingActionButton(
                    onClick = {
                        debounce {
                            val plan = uiState.activePlan!!
                            val viewCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, uiState.currentYear)
                                set(Calendar.MONTH, uiState.currentMonth)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val viewMonthStart = viewCal.timeInMillis
                            var targetDate = maxOf(viewMonthStart, plan.startDate)
                            targetDate = minOf(targetDate, plan.endDate)

                            onAddExpenseClick(targetDate)
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { fabCoords = it }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // [關鍵優化] 加入局部的 SharedTransitionLayout 來處理專注模式與月曆模式的切換
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = uiState.viewMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "mode_transition"
                ) { targetMode ->
                    Column(modifier = Modifier.fillMaxSize()) {

                        // 1. Status Card (只在專注模式顯示)
                        if (targetMode == ViewMode.Focus && uiState.activePlan != null) {
                            Box(modifier = Modifier.onGloballyPositioned { statusCardCoords = it }) {
                                DashboardStatusCard(
                                    todayAvailable = uiState.todayAvailable,
                                    isExpired = uiState.isExpired
                                )
                            }
                        } else {
                            // 在月曆模式下，給一點頂部間距讓畫面不擁擠
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 2. Calendar GlassCard (共用元素)
                        // 這是兩個模式間變化的主要區域
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                                // [關鍵] 標記為共用元素，key="calendar_card"
                                // 這會讓此卡片在 StatusCard 消失時，自動平滑地擴展到上方
                                .sharedElement(
                                    state = rememberSharedContentState(key = "calendar_card"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = FluidBoundsTransform
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp)
                            ) {
                                Box(modifier = Modifier.onGloballyPositioned { cardCoords = it }) {
                                    WeekHeader()
                                }

                                if (uiState.isLoading) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = AppTheme.colors.accent)
                                    }
                                } else if (uiState.dailyStates.isEmpty() && uiState.activePlan == null) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.viewMode == ViewMode.Focus && uiState.activePlan == null) {
                                            DashboardEmptyState(
                                                onCreateClick = { debounce { onEmptyDateClick(-1L, -1L) } }
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
                                            // ... (Pager 內容保持不變)
                                            val pageTargetDate = remember(page, startCalForPager) {
                                                (startCalForPager.clone() as Calendar).apply { add(Calendar.MONTH, page) }
                                            }
                                            val pageYear = pageTargetDate.get(Calendar.YEAR)
                                            val pageMonth = pageTargetDate.get(Calendar.MONTH)
                                            val isDataReady = pageYear == uiState.currentYear && pageMonth == uiState.currentMonth

                                            if (isDataReady) {
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
                                                            if (dayState.status == DayStatus.Empty) "empty_$index" else "${dayState.date}_${dayState.status}"
                                                        }
                                                    ) { index, dayState ->
                                                        if (dayState.status == DayStatus.Empty) {
                                                            Box(modifier = Modifier.aspectRatio(1f))
                                                        } else {
                                                            val scale = remember(dayState.date) { Animatable(0.92f) }
                                                            val alpha = remember(dayState.date) { Animatable(0f) }
                                                            LaunchedEffect(dayState.date) {
                                                                scale.snapTo(0.92f)
                                                                alpha.snapTo(0f)
                                                                launch { scale.animateTo(1f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
                                                                launch { alpha.animateTo(1f, animationSpec = tween(200)) }
                                                            }

                                                            Box(
                                                                modifier = Modifier.graphicsLayer {
                                                                    scaleX = scale.value
                                                                    scaleY = scale.value
                                                                    this.alpha = alpha.value
                                                                }
                                                            ) {
                                                                // 這裡使用從 MainActivity 傳來的 Scope (用於 DailyDetail 轉場)
                                                                var itemModifier = Modifier as Modifier
                                                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                                                    with(sharedTransitionScope) {
                                                                        itemModifier = itemModifier.sharedElement(
                                                                            state = rememberSharedContentState(key = "day_${dayState.date}"),
                                                                            animatedVisibilityScope = animatedVisibilityScope,
                                                                            boundsTransform = FluidBoundsTransform
                                                                        )
                                                                    }
                                                                }

                                                                Box(modifier = itemModifier) {
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
                                                }
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize())
                                            }
                                        }

                                        if (uiState.viewMode == ViewMode.Focus && totalPageCount > 1) {
                                            if (pagerState.currentPage > 0) {
                                                IconButton(
                                                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                                                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                                }
                                            }
                                            if (pagerState.currentPage < totalPageCount - 1) {
                                                IconButton(
                                                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                                                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = 12.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                                }
                                            }
                                        }
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
            1 -> focusToggleBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_mode), stringResource(R.string.tutorial_desc_to_calendar), position = CoachMarkPosition.Bottom) }
            2 -> subBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_subscribe), stringResource(R.string.tutorial_desc_subscribe), position = CoachMarkPosition.Bottom) }
            3 -> editBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_edit), stringResource(R.string.tutorial_desc_edit), position = CoachMarkPosition.Bottom) }
            4 -> listBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_list), stringResource(R.string.tutorial_desc_list), position = CoachMarkPosition.Bottom) }
            5 -> fabCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_record), stringResource(R.string.tutorial_desc_record), position = CoachMarkPosition.Top) }
            6 -> statusCardCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_today), stringResource(R.string.tutorial_desc_today), isCircle = false, position = CoachMarkPosition.Bottom) }
            7 -> cardCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_status), stringResource(R.string.tutorial_desc_status), isCircle = false, position = CoachMarkPosition.Bottom, extraHeight = gridHeight) }
            8 -> {
                LaunchedEffect(Unit) {
                    viewModel.toggleViewMode()
                    delay(500)
                    coachMarkStep = 9
                }
                null
            }
            9 -> calendarToggleBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_mode), stringResource(R.string.tutorial_desc_to_focus), position = CoachMarkPosition.Bottom) }
            10 -> historyBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_history), stringResource(R.string.tutorial_desc_history), position = CoachMarkPosition.Bottom) }
            11 -> addPlanBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_new_plan), stringResource(R.string.tutorial_desc_new_plan), position = CoachMarkPosition.Bottom) }
            12 -> settingsBtnCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_settings), stringResource(R.string.tutorial_desc_settings), position = CoachMarkPosition.Bottom) }
            13 -> cardCoords?.let { CoachMarkTarget(it, stringResource(R.string.tutorial_title_calendar), stringResource(R.string.tutorial_desc_calendar), isCircle = false, position = CoachMarkPosition.Bottom, extraHeight = gridHeight) }
            else -> null
        }

        if (target == null && coachMarkStep != 8 && coachMarkStep <= 13) {
            LaunchedEffect(Unit) { coachMarkStep++ }
        }

        if (target != null) {
            CoachMarkOverlay(target = target, onNext = { coachMarkStep++ }, onSkip = {
                coachMarkStep = 0
                isTutorialReady = false
                if (uiState.viewMode == ViewMode.Calendar) viewModel.toggleViewMode()
                onTutorialFinished()
            })
        } else if (coachMarkStep > 13) {
            coachMarkStep = 0
            isTutorialReady = false
            if (uiState.viewMode == ViewMode.Calendar) viewModel.toggleViewMode()
            onTutorialFinished()
        }
    }
}

// ... (RollingNumberText, DashboardStatusCard, WeekHeader, JapaneseDayGridItem, DashboardEmptyState 保持不變)
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
            animatable.animateTo(targetValue.toFloat(), animationSpec = tween(800, easing = FastOutSlowInEasing))
            lastValue = targetValue
        } else {
            animatable.snapTo(targetValue.toFloat())
        }
    }
    Text(text = "$ ${animatable.value.toInt()}", fontSize = fontSize, fontWeight = fontWeight, color = color)
}

@Composable
fun DashboardStatusCard(
    todayAvailable: Int,
    isExpired: Boolean
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
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
                fontWeight = FontWeight.Bold,
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
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = when (dayState.status) {
        DayStatus.Success -> Brush.verticalGradient(
            colors = listOf(
                AppTheme.colors.success.copy(alpha = 0.3f),
                AppTheme.colors.success.copy(alpha = 0.1f)
            )
        )
        DayStatus.Fail -> Brush.verticalGradient(
            colors = listOf(
                AppTheme.colors.fail.copy(alpha = 0.3f),
                AppTheme.colors.fail.copy(alpha = 0.1f)
            )
        )
        DayStatus.Neutral, DayStatus.Future -> Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.02f))
            } else {
                listOf(Color.Black.copy(alpha = 0.04f), Color.Transparent)
            }
        )
        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val textColor = when (dayState.status) {
        DayStatus.Success -> AppTheme.colors.success
        DayStatus.Fail -> AppTheme.colors.fail
        DayStatus.Neutral -> if (isDark) {
            AppTheme.colors.textPrimary.copy(alpha = 0.5f)
        } else {
            AppTheme.colors.textPrimary.copy(alpha = 0.5f)
        }
        DayStatus.Future -> if (isDark) {
            AppTheme.colors.textPrimary.copy(alpha = 0.3f)
        } else {
            AppTheme.colors.textPrimary.copy(alpha = 0.3f)
        }
        else -> AppTheme.colors.textSecondary
    }

    val borderModifier = if (dayState.isToday) {
        Modifier.border(1.5.dp, AppTheme.colors.accent, RoundedCornerShape(14.dp))
    } else if (dayState.status != DayStatus.Empty) {
        Modifier.border(0.5.dp, AppTheme.colors.textSecondary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
    } else {
        Modifier
    }

    val isClickable = dayState.status != DayStatus.Neutral || !showBalance

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .then(borderModifier)
            .clickable(enabled = isClickable) { onClick(dayState.date) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayState.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (dayState.isToday) FontWeight.Bold else FontWeight.Medium,
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = AppTheme.colors.accent.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = AppTheme.colors.accent
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.msg_no_active_plan),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.msg_empty_state_subtitle),
            fontSize = 15.sp,
            color = AppTheme.colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.accent,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
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