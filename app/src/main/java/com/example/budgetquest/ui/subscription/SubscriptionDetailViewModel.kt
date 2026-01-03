package com.example.budgetquest.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val cycleText: String = "",
    val nextDateText: String = "",
    val periodText: String = "",

    // [新增] 補上這兩個欄位，解決 Screen 的 Unresolved reference 錯誤
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

                // 1. 週期
                val cycleStr = when(sub.frequency) {
                    "MONTH" -> "每月"
                    "WEEK" -> "每週"
                    "DAY" -> "每日"
                    "CUSTOM" -> "每 ${sub.customDays} 天"
                    else -> "自訂"
                }

                // 2. 下次支出
                val nextDateMillis = calculateNextDueDate(sub)
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

                val nextDateStr = if (!isActive) {
                    "已結束"
                } else {
                    val dateStr = dateFormat.format(java.util.Date(nextDateMillis))
                    val daysLeft = getDaysDifference(now, nextDateMillis)
                    if (daysLeft == 0L) "$dateStr (今天)" else "$dateStr ($daysLeft 天後)"
                }

                // 3. 期間
                val startStr = dateFormat.format(java.util.Date(sub.startDate))
                val endStr = sub.endDate?.let { dateFormat.format(java.util.Date(it)) } ?: "無限期"
                val periodStr = "$startStr ~ $endStr"

                _uiState.value = SubscriptionDetailUiState(
                    id = sub.id,
                    name = sub.note,
                    amount = sub.amount,
                    category = sub.category,
                    startDate = sub.startDate,
                    endDate = sub.endDate,
                    isActive = isActive,
                    cycleText = cycleStr,
                    nextDateText = nextDateStr,
                    periodText = periodStr,
                    // [新增] 載入資料庫中的值
                    paymentMethod = sub.paymentMethod,
                    isNeed = sub.isNeed
                )
            }
        }
    }

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