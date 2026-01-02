package com.example.budgetquest.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.RecurringExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class SubscriptionDetailUiState(
    val id: Long = -1,
    val name: String = "",
    val amount: Int = 0,
    val category: String = "",
    val description: String = "",
    val dayOfMonth: Int = 1,
    val startDate: Long = 0L,
    val endDate: Long? = null,
    val isActive: Boolean = true
)

class SubscriptionDetailViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionDetailUiState())
    val uiState: StateFlow<SubscriptionDetailUiState> = _uiState.asStateFlow()

    fun loadSubscription(id: Long) {
        viewModelScope.launch {
            // [關鍵] 這裡呼叫 Repository 的方法，請確保 BudgetRepository 有定義 getRecurringExpenseById
            val sub = repository.getRecurringExpenseById(id)

            if (sub != null) {
                val now = System.currentTimeMillis()
                // [修正] 比較 endDate (Long?) 與 now (Long)
                // 如果 endDate 是 null，代表無限期 (isActive = true)
                // 如果 endDate 不是 null，則檢查是否大於現在
                val isActive = sub.endDate == null || sub.endDate > now

                _uiState.value = SubscriptionDetailUiState(
                    id = sub.id,
                    name = sub.note, // 對應 Entity 的 note
                    amount = sub.amount,
                    category = sub.category,
                    // dayOfMonth 欄位在 Entity 中如果叫 dayOfMonth，這裡就沒問題
                    // 如果您的 Entity 沒這個欄位，請在 RecurringExpenseEntity 補上，或暫時用 1 代替
                    dayOfMonth = sub.dayOfMonth,
                    startDate = sub.startDate,
                    endDate = sub.endDate,
                    isActive = isActive
                )
            }
        }
    }

    fun terminateSubscription(id: Long) {
        viewModelScope.launch {
            val sub = repository.getRecurringExpenseById(id)
            if (sub != null) {
                val now = System.currentTimeMillis()
                // [修正] 使用 copy 建立新物件，並呼叫正確的 update 方法
                val updatedSub = sub.copy(endDate = now)
                repository.updateRecurringExpense(updatedSub)
                loadSubscription(id)
            }
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch {
            val sub = repository.getRecurringExpenseById(id)
            if (sub != null) {
                // [修正] 呼叫正確的 delete 方法
                repository.deleteRecurringExpense(sub)
            }
        }
    }
}