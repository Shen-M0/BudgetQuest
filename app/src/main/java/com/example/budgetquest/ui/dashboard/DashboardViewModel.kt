package com.example.budgetquest.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

enum class ViewMode { Focus, Calendar }
enum class DayStatus { Future, Success, Neutral, Fail, Empty }

data class DailyState(
    val date: Long,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val available: Int,
    val spent: Int,
    val balance: Int,
    val baseLimit: Int,
    val status: DayStatus
)

data class DashboardUiState(
    val dailyStates: List<DailyState> = emptyList(),
    val todayAvailable: Int = 0,
    val isLoading: Boolean = false,
    val viewMode: ViewMode = ViewMode.Focus,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val activePlan: PlanEntity? = null,
    val isExpired: Boolean = false
)

data class CalendarState(val year: Int, val month: Int)

class DashboardViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.Focus)
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedPlanId = MutableStateFlow<Int?>(null)

    private val _calendarState = combine(_currentYear, _currentMonth) { year, month ->
        CalendarState(year, month)
    }

    private val _targetPlan = combine(
        _selectedPlanId,
        budgetRepository.getAllPlansStream()
    ) { selectedId, allPlans ->
        val today = System.currentTimeMillis()
        if (selectedId != null) {
            allPlans.find { it.id == selectedId }
        } else {
            val currentActivePlan = allPlans.find { plan ->
                today >= getStartOfDay(plan.startDate) && today <= getEndOfDay(plan.endDate)
            }
            currentActivePlan ?: allPlans.maxByOrNull { it.endDate }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _targetPlan,
        budgetRepository.getAllPlansStream(),
        budgetRepository.getAllExpensesStream(),
        _viewMode,
        _calendarState
    ) { activePlan, allPlans, expenses, mode, calState ->

        val year = calState.year
        val month = calState.month

        val dailyStates = if (mode == ViewMode.Focus) {
            if (activePlan != null) {
                generateFocusModeDays(activePlan, expenses, year, month)
            } else {
                generateEmptyCalendarDays(year, month)
            }
        } else {
            generateCalendarModeDays(year, month, allPlans, expenses)
        }

        var displayAmount = 0
        var isExpired = false

        if (activePlan != null) {
            val todayMillis = System.currentTimeMillis()
            isExpired = todayMillis > getEndOfDay(activePlan.endDate)

            if (isExpired) {
                val planExpenses = expenses.filter { it.date >= activePlan.startDate && it.date <= activePlan.endDate }
                displayAmount = activePlan.totalBudget - planExpenses.sumOf { it.amount } - activePlan.targetSavings
            } else {
                val todayStart = getStartOfDay(todayMillis)
                if (todayStart >= getStartOfDay(activePlan.startDate) && todayStart <= getEndOfDay(activePlan.endDate)) {
                    val states = generateAllPlanDays(activePlan, expenses)
                    val todayState = states.find { isSameDay(it.date, todayMillis) }
                    displayAmount = todayState?.balance ?: 0
                }
            }
        }

        DashboardUiState(
            activePlan = activePlan,
            todayAvailable = displayAmount,
            currentMonth = month,
            currentYear = year,
            dailyStates = dailyStates,
            viewMode = mode,
            isExpired = isExpired
        )
    }
        .flowOn(Dispatchers.Default) // [優化] 將繁重的計算移至背景執行緒，避免卡頓
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    init {
        viewModelScope.launch { budgetRepository.checkAndGenerateRecurringExpenses() }
    }

    fun toggleViewMode() { _viewMode.value = if (_viewMode.value == ViewMode.Focus) ViewMode.Calendar else ViewMode.Focus }

    fun updateMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
    }

    fun nextMonth() {
        val c = Calendar.getInstance().apply { set(_currentYear.value, _currentMonth.value, 1); add(Calendar.MONTH, 1) }
        updateMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }
    fun prevMonth() {
        val c = Calendar.getInstance().apply { set(_currentYear.value, _currentMonth.value, 1); add(Calendar.MONTH, -1) }
        updateMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    fun selectPlanByDate(dateMillis: Long) {
        viewModelScope.launch {
            val allPlans = budgetRepository.getAllPlans()
            val targetPlan = allPlans.find { plan -> dateMillis >= getStartOfDay(plan.startDate) && dateMillis <= getEndOfDay(plan.endDate) }

            if (targetPlan != null) {
                _selectedPlanId.value = targetPlan.id
                _viewMode.value = ViewMode.Focus
                val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
                updateMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
            }
        }
    }

    // [關鍵修正] 避免重複選擇導致模式重置
    fun selectPlanById(planId: Int) {
        viewModelScope.launch {
            // 如果選中的 ID 已經是當前的 ID，直接返回，不做任何狀態重置
            // 這樣當從設定頁面返回時，因為 ID 沒變，ViewMode 和月份就不會被強制修改
            if (_selectedPlanId.value == planId) return@launch

            _selectedPlanId.value = planId
            _viewMode.value = ViewMode.Focus // 只有在「真正切換」新計畫時，才跳轉到專注模式

            val allPlans = budgetRepository.getAllPlans()
            val plan = allPlans.find { it.id == planId }

            if (plan != null) {
                val currentViewCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, _currentYear.value)
                    set(Calendar.MONTH, _currentMonth.value)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }

                val currentViewStart = currentViewCal.timeInMillis
                val currentViewEnd = getEndOfMonth(currentViewStart)

                val planStart = getStartOfDay(plan.startDate)
                val planEnd = getEndOfDay(plan.endDate)

                val isOverlap = planStart <= currentViewEnd && planEnd >= currentViewStart

                if (!isOverlap) {
                    val c = Calendar.getInstance().apply { timeInMillis = plan.startDate }
                    updateMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
                }
            }
        }
    }

    suspend fun calculateSmartDates(clickedDate: Long): Pair<Long, Long> {
        val allPlans = budgetRepository.getAllPlans().sortedBy { it.startDate }
        val nextPlan = allPlans.firstOrNull { it.startDate > clickedDate }
        val startDate = clickedDate
        val endDate: Long = if (nextPlan != null) {
            if (isSameMonth(clickedDate, nextPlan.startDate)) nextPlan.startDate - 86400000L else getEndOfMonth(clickedDate)
        } else {
            getEndOfMonth(clickedDate)
        }
        return startDate to endDate
    }

    // --- Helpers ---
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameMonth(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }

    private fun getEndOfMonth(date: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = date }
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59)
        return c.timeInMillis
    }

    private fun getStartOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }

    private fun getEndOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
        return c.timeInMillis
    }

    // --- 生成邏輯 ---

    private fun generateAllPlanDays(plan: PlanEntity, expenses: List<ExpenseEntity>): List<DailyState> {
        val list = mutableListOf<DailyState>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = plan.startDate
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)

        val diff = plan.endDate - plan.startDate
        val totalDays = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
        val safeTotalDays = if (totalDays > 0) totalDays else 1

        val dailyBase = (plan.totalBudget - plan.targetSavings) / safeTotalDays
        val remainder = (plan.totalBudget - plan.targetSavings) % safeTotalDays

        val todayMillis = System.currentTimeMillis()
        var accumulatedCarryOver = 0

        for (i in 0 until safeTotalDays) {
            val dayStart = calendar.timeInMillis
            val dayEnd = getEndOfDay(dayStart)
            val daySpent = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }

            val extraForLastDay = if (i == safeTotalDays - 1) remainder else 0

            val currentAvailable = dailyBase + accumulatedCarryOver + extraForLastDay
            val balance = currentAvailable - daySpent
            accumulatedCarryOver = balance

            val isFutureDay = dayStart > todayMillis
            val status = if (isSameDay(dayStart, todayMillis)) {
                if (balance >= 0) DayStatus.Success else DayStatus.Fail
            } else if (dayStart > todayMillis) {
                DayStatus.Future
            } else if (balance >= 0) {
                DayStatus.Success
            } else {
                DayStatus.Fail
            }

            list.add(DailyState(
                date = dayStart,
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                isToday = isSameDay(dayStart, todayMillis),
                available = currentAvailable,
                spent = daySpent,
                balance = balance,
                baseLimit = dailyBase + extraForLastDay,
                status = status
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    private fun generateFocusModeDays(plan: PlanEntity, expenses: List<ExpenseEntity>, displayYear: Int, displayMonth: Int): List<DailyState> {
        val allPlanDays = generateAllPlanDays(plan, expenses)

        val monthStartCal = Calendar.getInstance().apply {
            set(displayYear, displayMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStartMillis = monthStartCal.timeInMillis
        val monthEndMillis = getEndOfMonth(monthStartMillis)

        val viewStart = max(getStartOfDay(plan.startDate), monthStartMillis)
        val viewEnd = min(getEndOfDay(plan.endDate), monthEndMillis)

        if (viewStart > viewEnd) return emptyList()

        val filteredDays = allPlanDays.filter { it.date in viewStart..viewEnd }
        if (filteredDays.isEmpty()) return emptyList()

        val resultList = mutableListOf<DailyState>()

        val firstDayCal = Calendar.getInstance().apply { timeInMillis = viewStart }
        val startDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)

        repeat(startDayOfWeek - 1) { resultList.add(createEmptyState()) }

        resultList.addAll(filteredDays)

        return resultList
    }

    private fun generateEmptyCalendarDays(year: Int, month: Int): List<DailyState> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<DailyState>()
        repeat(startDayOfWeek - 1) { list.add(createEmptyState()) }
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            list.add(createGrayState(calendar.timeInMillis, day))
        }
        return list
    }

    private fun generateCalendarModeDays(year: Int, month: Int, allPlans: List<PlanEntity>, expenses: List<ExpenseEntity>): List<DailyState> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthStart = calendar.timeInMillis
        val monthEnd = getEndOfMonth(monthStart)

        val overlappingPlans = allPlans.filter { plan ->
            (plan.startDate <= monthEnd) && (plan.endDate >= monthStart)
        }

        val calculatedStatesMap = mutableMapOf<Long, DailyState>()
        overlappingPlans.forEach { plan ->
            val planDays = generateAllPlanDays(plan, expenses)
            planDays.forEach { day -> calculatedStatesMap[day.date] = day }
        }

        val resultList = mutableListOf<DailyState>()
        repeat(startDayOfWeek - 1) { resultList.add(createEmptyState()) }

        for (day in 1..maxDays) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val currentDayMillis = getStartOfDay(calendar.timeInMillis)

            val planState = calculatedStatesMap[currentDayMillis]

            if (planState != null) {
                resultList.add(planState)
            } else {
                resultList.add(DailyState(
                    date = currentDayMillis,
                    dayOfMonth = day,
                    isToday = isSameDay(currentDayMillis, System.currentTimeMillis()),
                    available = 0, spent = 0, balance = 0, baseLimit = 0,
                    status = DayStatus.Neutral
                ))
            }
        }
        return resultList
    }

    private fun createEmptyState() = DailyState(0, 0, false, 0, 0, 0, 0, DayStatus.Empty)

    private fun createGrayState(date: Long, dayOfMonth: Int) = DailyState(
        date = date, dayOfMonth = dayOfMonth, baseLimit = 0, spent = 0, available = 0, balance = 0, status = DayStatus.Future, isToday = isSameDay(date, System.currentTimeMillis())
    )
}