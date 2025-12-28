package com.example.budgetquest.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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

class DashboardViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.Focus)
    private val _currentMonthOffset = MutableStateFlow(0)
    private val _selectedPlanId = MutableStateFlow<Int?>(null)

    // 1. 決定當前要顯示哪個計畫 (專注模式用)
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
        _currentMonthOffset
    ) { activePlan, allPlans, expenses, mode, monthOffset ->

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        // 1. 生成格子
        val dailyStates = if (mode == ViewMode.Focus) {
            if (activePlan != null) generateFocusModeDays(activePlan, expenses) else emptyList()
        } else {
            // [關鍵修正] 這裡會呼叫新的邏輯，計算正確的顏色
            generateCalendarModeDays(year, month, allPlans, expenses)
        }

        // 2. 計算今日可用額度 (保持不變)
        var displayAmount = 0
        var isExpired = false
        if (activePlan != null) {
            val todayMillis = System.currentTimeMillis()
            isExpired = todayMillis > getEndOfDay(activePlan.endDate)
            if (isExpired) {
                val planExpenses = expenses.filter { it.date >= activePlan.startDate && it.date <= activePlan.endDate }
                displayAmount = activePlan.totalBudget - planExpenses.sumOf { it.amount } - activePlan.targetSavings
            } else {
                val todayState = dailyStates.find { it.isToday }
                displayAmount = todayState?.balance ?: 0
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch { budgetRepository.checkAndGenerateRecurringExpenses() }
    }

    fun toggleViewMode() { _viewMode.value = if (_viewMode.value == ViewMode.Focus) ViewMode.Calendar else ViewMode.Focus }
    fun nextMonth() { _currentMonthOffset.value += 1 }
    fun prevMonth() { _currentMonthOffset.value -= 1 }

    fun selectPlanByDate(dateMillis: Long) {
        viewModelScope.launch {
            val allPlans = budgetRepository.getAllPlans()
            val targetPlan = allPlans.find { plan -> dateMillis >= getStartOfDay(plan.startDate) && dateMillis <= getEndOfDay(plan.endDate) }
            if (targetPlan != null) {
                _selectedPlanId.value = targetPlan.id
                _viewMode.value = ViewMode.Focus
            }
        }
    }

    fun selectPlanById(planId: Int) {
        _selectedPlanId.value = planId
        _viewMode.value = ViewMode.Focus
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

    // --- Helpers (保持不變) ---
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

    // 專注模式產生器 (保持不變，這是計算核心)
    private fun generateFocusModeDays(plan: PlanEntity, expenses: List<ExpenseEntity>): List<DailyState> {
        val dailyStates = mutableListOf<DailyState>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = plan.startDate
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)

        val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        repeat(startDayOfWeek - 1) { dailyStates.add(createEmptyState()) }

        val diff = plan.endDate - plan.startDate
        val totalDays = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
        val safeTotalDays = if (totalDays > 0) totalDays else 1
        val dailyBase = (plan.totalBudget - plan.targetSavings) / safeTotalDays
        val todayMillis = System.currentTimeMillis()
        var accumulatedCarryOver = 0

        for (i in 0 until safeTotalDays) {
            val dayStart = calendar.timeInMillis
            val dayEnd = getEndOfDay(dayStart)
            val daySpent = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }

            val currentAvailable = dailyBase + accumulatedCarryOver
            val balance = currentAvailable - daySpent
            accumulatedCarryOver = balance

            val isFutureDay = dayStart > todayMillis
            val status = if (isFutureDay) DayStatus.Future else if (balance >= 0) DayStatus.Success else DayStatus.Fail

            dailyStates.add(DailyState(
                date = dayStart,
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                isToday = isSameDay(dayStart, todayMillis),
                available = currentAvailable,
                spent = daySpent,
                balance = balance,
                baseLimit = dailyBase,
                status = status
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dailyStates
    }

    // [關鍵修正] 月曆模式產生器
    private fun generateCalendarModeDays(year: Int, month: Int, allPlans: List<PlanEntity>, expenses: List<ExpenseEntity>): List<DailyState> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val monthStart = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = getEndOfDay(calendar.timeInMillis)
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Reset 回月初

        // 1. 找出與當月重疊的所有計畫
        val overlappingPlans = allPlans.filter { plan ->
            (plan.startDate <= monthEnd) && (plan.endDate >= monthStart)
        }

        // 2. 預先計算這些計畫中，每一天的詳細狀態 (包含紅綠顏色)，並存入 Map
        val calculatedStatesMap = mutableMapOf<Long, DailyState>()
        overlappingPlans.forEach { plan ->
            // 重用專注模式的計算邏輯
            val planDays = generateFocusModeDays(plan, expenses)
            planDays.forEach { dayState ->
                if (dayState.status != DayStatus.Empty) {
                    // 使用日期起始時間作為 Key
                    calculatedStatesMap[getStartOfDay(dayState.date)] = dayState
                }
            }
        }

        val dailyStates = mutableListOf<DailyState>()

        // 3. 計算前面的空白格
        val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        repeat(startDayOfWeek - 1) { dailyStates.add(createEmptyState()) }

        // 4. 生成當月格子
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDays) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = getStartOfDay(calendar.timeInMillis)

            // 5. [核心] 優先從計算好的 Map 中取值
            val preCalculatedState = calculatedStatesMap[dayStart]

            if (preCalculatedState != null) {
                // 如果這一天有計算好的狀態，直接使用 (顏色就會是對的)
                // 需要重新設定 isToday，因為 Map 中的 isToday 可能是舊的
                dailyStates.add(preCalculatedState.copy(isToday = isSameDay(dayStart, System.currentTimeMillis())))
            } else {
                // 如果沒有計畫，顯示灰色
                dailyStates.add(createGrayState(dayStart, day))
            }
        }
        return dailyStates
    }

    private fun createEmptyState() = DailyState(0, 0, false, 0, 0, 0, 0, DayStatus.Empty)

    private fun createGrayState(date: Long, dayOfMonth: Int) = DailyState(
        date = date, dayOfMonth = dayOfMonth, baseLimit = 0, spent = 0, available = 0, balance = 0, status = DayStatus.Future, isToday = isSameDay(date, System.currentTimeMillis())
    )
}