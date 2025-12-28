package com.example.budgetquest.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// 定義 UI 狀態，包含當前顯示的日期
data class DailyDetailUiState(
    val date: Long = System.currentTimeMillis()
)

class DailyDetailViewModel(private val repository: BudgetRepository) : ViewModel() {

    // 內部用來控制日期的 StateFlow
    private val _dateFilter = MutableStateFlow(System.currentTimeMillis())

    // 1. 公開的 uiState，讓 Screen 讀取當前日期
    val uiState: StateFlow<DailyDetailUiState> = _dateFilter
        .map { DailyDetailUiState(date = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DailyDetailUiState()
        )

    // 2. 公開的 expenses 列表，根據日期自動過濾
    val expenses: StateFlow<List<ExpenseEntity>> = combine(
        _dateFilter,
        repository.getAllExpensesStream()
    ) { targetDate, allExpenses ->
        val startOfDay = getStartOfDay(targetDate)
        val endOfDay = getEndOfDay(targetDate)

        // 過濾出該日期的消費，並按時間排序
        allExpenses.filter { it.date in startOfDay..endOfDay }
            .sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // --- Actions ---

    // 設定要顯示哪一天
    fun setDate(date: Long) {
        _dateFilter.value = date
    }

    // 刪除消費
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // --- Helper Functions (計算當天的開始與結束時間) ---

    private fun getStartOfDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}