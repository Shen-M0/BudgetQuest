package com.example.budgetquest.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R // [新增]
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.RecurringExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SubscriptionDetailUiState(
    val id: Long = -1,
    val name: String = "",
    val amount: Int = 0,
    val category: String = "",
    val startDate: Long = 0L,
    val endDate: Long? = null,
    val isActive: Boolean = true,

    // [修改] 改為存 Resource ID 或 Pair，讓 UI 層去解析
    // 為了不讓 UI 邏輯太複雜，這裡還是存字串，但字串內容是空的，
    // 我們新增幾個輔助欄位讓 UI 決定顯示什麼

    val frequencyType: String = "", // MONTH, WEEK...
    val customDays: Int = 0,

    val nextDateMillis: Long = 0L,
    val daysLeft: Long = -1L, // -1 代表已結束

    val startDateMillis: Long = 0L,
    val endDateMillis: Long? = null,

    val paymentMethod: String = "",
    val isNeed: Boolean? = null
)

class SubscriptionDetailViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionDetailUiState())
    val uiState: StateFlow<SubscriptionDetailUiState> = _uiState.asStateFlow()

    private var currentEntity: RecurringExpenseEntity? = null

    fun loadSubscription(id: Long) {
        viewModelScope.launch {
            val sub = repository.getRecurringExpenseById(id)
            if (sub != null) {
                currentEntity = sub
                val now = System.currentTimeMillis()
                val isActive = sub.endDate == null || sub.endDate > now

                // 計算下次日期
                val nextDateMillis = calculateNextDueDate(sub)
                val daysLeft = if (!isActive) -1L else getDaysDifference(now, nextDateMillis)

                _uiState.value = SubscriptionDetailUiState(
                    id = sub.id,
                    name = sub.note,
                    amount = sub.amount,
                    category = sub.category,
                    isActive = isActive,

                    // 傳遞原始數據給 UI
                    frequencyType = sub.frequency,
                    customDays = sub.customDays,

                    nextDateMillis = nextDateMillis,
                    daysLeft = daysLeft,

                    startDateMillis = sub.startDate,
                    endDateMillis = sub.endDate,

                    paymentMethod = sub.paymentMethod,
                    isNeed = sub.isNeed
                )
            }
        }
    }

    // ... (getDaysDifference, calculateNextDueDate, terminateSubscription, deleteSubscription 保持不變) ...
    private fun getDaysDifference(startMillis: Long, endMillis: Long): Long {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = endCal.timeInMillis - startCal.timeInMillis
        return diffMillis / (1000 * 60 * 60 * 24)
    }

    private fun calculateNextDueDate(recurring: RecurringExpenseEntity): Long {
        if (recurring.endDate != null && System.currentTimeMillis() > recurring.endDate) return 0L
        val calendar = Calendar.getInstance()
        val baseDate = if (recurring.lastGeneratedDate == 0L) recurring.startDate else recurring.lastGeneratedDate
        calendar.timeInMillis = baseDate
        when (recurring.frequency) {
            "MONTH" -> calendar.add(Calendar.MONTH, 1)
            "WEEK" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "DAY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "CUSTOM" -> {
                val days = if (recurring.customDays <= 0) 1 else recurring.customDays
                calendar.add(Calendar.DAY_OF_YEAR, days)
            }
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun terminateSubscription(id: Long) {
        viewModelScope.launch {
            val sub = repository.getRecurringExpenseById(id)
            if (sub != null) {
                val now = System.currentTimeMillis()
                repository.updateRecurringExpense(sub.copy(endDate = now))
                loadSubscription(id)
            }
        }
    }

    fun deleteSubscription(id: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            val sub = repository.getRecurringExpenseById(id)
            if (sub != null) {
                repository.deleteRecurringRuleAndHistory(sub)
            }
            onComplete()
        }
    }
}