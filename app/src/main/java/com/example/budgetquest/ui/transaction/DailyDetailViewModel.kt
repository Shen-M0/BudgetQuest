package com.example.budgetquest.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// 定義 UI 狀態，包含當前顯示的日期
data class DailyDetailUiState(
    val date: Long = System.currentTimeMillis()
)

class DailyDetailViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _currentDate = MutableStateFlow(0L)
    val currentDate: StateFlow<Long> = _currentDate

    private val _currentPlan = MutableStateFlow<PlanEntity?>(null)

    // [新增] 解決問題 4：防止從編輯頁返回時，日期被重置為初始進入的日期
    private var isInitialized = false

    // [修正] 解決問題 3：使用嚴格的日期比較 (忽略時間)
    val canGoPrevious: StateFlow<Boolean> = combine(_currentDate, _currentPlan) { date, plan ->
        if (plan == null) false else isDateAfter(date, plan.startDate) // 當前日期 > 開始日期，才能往左
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canGoNext: StateFlow<Boolean> = combine(_currentDate, _currentPlan) { date, plan ->
        if (plan == null) false else isDateBefore(date, plan.endDate) // 當前日期 < 結束日期，才能往右
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val expenses: StateFlow<List<ExpenseEntity>> = combine(_currentDate, repository.getAllExpensesStream()) { date, all ->
        val start = getStartOfDay(date)
        val end = getEndOfDay(date)
        all.filter { it.date in start..end }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDate(date: Long) {
        // [關鍵修正] 如果已經初始化過 (代表 ViewModel 存活，可能是從編輯頁返回)，則忽略傳入的舊 date
        if (isInitialized) return

        viewModelScope.launch {
            _currentDate.value = date
            val plans = repository.getAllPlansStream().first()
            // 找到包含此日期的計畫
            val plan = plans.find {
                val d = getStartOfDay(date)
                val s = getStartOfDay(it.startDate)
                val e = getStartOfDay(it.endDate) // 寬容判斷
                d in s..e
            }
            _currentPlan.value = plan
            isInitialized = true
        }
    }

    fun goToPreviousDay() {
        if (canGoPrevious.value) {
            _currentDate.value -= ONE_DAY_MILLIS
        }
    }

    fun goToNextDay() {
        if (canGoNext.value) {
            _currentDate.value += ONE_DAY_MILLIS
        }
    }

    companion object {
        const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    private fun getStartOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }
    private fun getEndOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
        return c.timeInMillis
    }

    // 判斷 date1 是否在 date2 之後 (date1 > date2)
    private fun isDateAfter(date1: Long, date2: Long): Boolean {
        return getStartOfDay(date1) > getStartOfDay(date2)
    }

    // 判斷 date1 是否在 date2 之前 (date1 < date2)
    private fun isDateBefore(date1: Long, date2: Long): Boolean {
        return getStartOfDay(date1) < getStartOfDay(date2)
    }
}