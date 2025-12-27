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
// [修改] 狀態現在只需要基本定義，顏色由 UI 決定
enum class DayStatus { Future, Success, Neutral, Fail }

data class DailyState(
    val date: Long,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val available: Int, // 當日起始可用 (含累計)
    val spent: Int,     // 當日花費
    val balance: Int,   // [新增] 當日結餘 (available - spent)，用於顯示與顏色判斷
    val baseLimit: Int, // 每日基礎額度 (用於計算顏色深淺比例)
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

    // 1. 決定當前要顯示哪個計畫
    private val _targetPlan = combine(
        budgetRepository.getCurrentPlanStream(), // 這個可以不用了，我們直接用 allPlans 算
        _selectedPlanId,
        budgetRepository.getAllPlansStream()
    ) { _, selectedId, allPlans -> // 第一個參數 _ 忽略
        val today = System.currentTimeMillis()

        if (selectedId != null) {
            // Case A: 使用者手動選擇
            allPlans.find { it.id == selectedId }
        } else {
            // Case B: 自動判斷
            // 1. 優先找：包含 "今天" 的計畫
            val currentActivePlan = allPlans.find { plan ->
                today >= getStartOfDay(plan.startDate) && today <= getEndOfDay(plan.endDate)
            }

            // 2. 找不到則找：最新的計畫 (結束日期最晚的) -> 模擬 "上次最後開啟"
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

        // 1. 生成格子 (所有數值計算都在這裡完成)
        val dailyStates = if (mode == ViewMode.Focus) {
            if (activePlan != null) generateFocusModeDays(activePlan, expenses) else emptyList()
        } else {
            generateCalendarModeDays(year, month, allPlans, expenses)
        }

        // 2. [關鍵修正] 從生成好的 dailyStates 中抓取 "今日可用額度"
        // 這樣就能保證上方卡片與下方格子的數字 100% 同步
        var displayAmount = 0
        var isExpired = false

        if (activePlan != null) {
            val todayMillis = System.currentTimeMillis()
            val planEndCalendar = Calendar.getInstance().apply { timeInMillis = activePlan.endDate }
            planEndCalendar.set(Calendar.HOUR_OF_DAY, 23); planEndCalendar.set(Calendar.MINUTE, 59)
            isExpired = todayMillis > planEndCalendar.timeInMillis

            if (isExpired) {
                // 如果過期，顯示總結餘 (總預算 - 總花費)
                // 這裡我們可以直接計算總花費，比較簡單直觀
                val planExpenses = expenses.filter { it.date >= activePlan.startDate && it.date <= activePlan.endDate }
                displayAmount = activePlan.totalBudget - planExpenses.sumOf { it.amount }
            } else {
                // 如果進行中，找出 "今天" 的格子，顯示它的 balance (結餘)
                // 如果找不到今天 (例如切換月份了)，就顯示 0 或保持原樣
                val todayState = dailyStates.find { it.isToday }
                // 如果沒找到今天(可能在看別的月份)，但還在計畫內，我們可以試著找 "計畫最後一天" 的狀態?
                // 為了簡單且準確，我們只在 "顯示當前月份" 且 "包含今天" 時顯示今日額度
                displayAmount = todayState?.balance ?: 0

                // [Edge Case] 如果今天還沒到計畫開始日，或者剛好跨月，顯示 0 或是第一天的預算
                if (todayState == null && mode == ViewMode.Focus && dailyStates.isNotEmpty()) {
                    // 這種情況比較少見，暫時顯示 0
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

    // --- 計算邏輯 (包含累計 CarryOver) ---
    private fun generateFocusModeDays(plan: PlanEntity, expenses: List<ExpenseEntity>): List<DailyState> {
        val dailyStates = mutableListOf<DailyState>()
        val diff = plan.endDate - plan.startDate
        val totalDays = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
        val safeTotalDays = if (totalDays > 0) totalDays else 1
        val dailyBase = (plan.totalBudget - plan.targetSavings) / safeTotalDays

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = plan.startDate
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val todayMillis = System.currentTimeMillis()
        var accumulatedCarryOver = 0 // 累計變數

        for (i in 0 until safeTotalDays) {
            val dayStart = calendar.timeInMillis
            val dayEnd = getEndOfDay(dayStart)
            val daySpent = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }

            // [計算核心]
            val currentAvailable = dailyBase + accumulatedCarryOver
            val balance = currentAvailable - daySpent

            // 累計到下一天
            accumulatedCarryOver = balance

            val isFutureDay = dayStart > todayMillis

            val status = if (isFutureDay) {
                DayStatus.Future
            } else {
                when {
                    balance > 0 -> DayStatus.Success
                    balance == 0 -> DayStatus.Neutral
                    else -> DayStatus.Fail
                }
            }

            dailyStates.add(DailyState(
                date = dayStart,
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                isToday = isSameDay(dayStart, todayMillis),
                available = currentAvailable,
                spent = daySpent,
                balance = balance, // [新增]
                baseLimit = dailyBase,
                status = status
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dailyStates
    }

    private fun generateCalendarModeDays(year: Int, month: Int, allPlans: List<PlanEntity>, expenses: List<ExpenseEntity>): List<DailyState> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0); calendar.set(Calendar.MILLISECOND, 0)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyStates = mutableListOf<DailyState>()
        val todayMillis = System.currentTimeMillis()

        for (day in 1..maxDays) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = calendar.timeInMillis
            val dayEnd = getEndOfDay(dayStart)

            val targetPlan = allPlans.find { plan -> dayStart >= getStartOfDay(plan.startDate) && dayStart <= getEndOfDay(plan.endDate) }

            if (targetPlan == null) {
                dailyStates.add(createGrayState(dayStart, day))
            } else {
                // 為了準確顯示顏色深淺，我們需要那個計畫當天的 "balance"
                // 這裡會跑一個小迴圈去算那個計畫的所有天數 (確保 CarryOver 正確)
                val planStates = generateFocusModeDays(targetPlan, expenses)
                val dayState = planStates.find { isSameDay(it.date, dayStart) }

                if (dayState != null) {
                    dailyStates.add(dayState)
                } else {
                    dailyStates.add(createGrayState(dayStart, day))
                }
            }
        }
        return dailyStates
    }

    private fun createGrayState(date: Long, dayOfMonth: Int) = DailyState(
        date = date, dayOfMonth = dayOfMonth, baseLimit = 0, spent = 0, available = 0, balance = 0, status = DayStatus.Future, isToday = false
    )
    private fun getStartOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }
    private fun getEndOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
        return c.timeInMillis
    }
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }; val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}