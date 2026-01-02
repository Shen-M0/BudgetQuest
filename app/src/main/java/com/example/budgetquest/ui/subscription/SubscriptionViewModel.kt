package com.example.budgetquest.ui.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.RecurringExpenseEntity
import com.example.budgetquest.data.SubscriptionTagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val id: Long = -1L,
    val planId: Int = -1,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val amount: String = "",
    val category: String = "",
    val note: String = "",
    val frequency: String = "MONTH",
    val customDays: String = "1",
    val errorMessageId: Int? = null
)

class SubscriptionViewModel(private val repository: BudgetRepository) : ViewModel() {

    var uiState by mutableStateOf(SubscriptionUiState())
        private set

    // 用來儲存計畫的邊界日期 (防呆用)
    private var limitStartDate: Long = 0L
    private var limitEndDate: Long = 0L


    // [修正 1] 移除重複定義，只保留這一個 recurringList
    // 直接從 Repository 取得資料流
    val recurringList = repository.getAllRecurringExpensesStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val visibleCategories = repository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleSubTags = repository.getVisibleSubTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = repository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubTags = repository.getAllSubTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun initialize(planId: Int, startDate: Long, endDate: Long) {
        // 記錄計畫的範圍邊界
        this.limitStartDate = startDate
        this.limitEndDate = endDate

        if (uiState.id == -1L) {
            // [修改] 新增模式：預設開始與結束日期皆為計畫的範圍
            uiState = uiState.copy(
                planId = planId,
                startDate = if (startDate > 0) startDate else System.currentTimeMillis(),
                // [修改] 預設結束日期為計畫結束日，而非 null (無限期)
                endDate = if (endDate > 0) endDate else null
            )
        }
    }

    fun updateUiState(
        amount: String? = null,
        category: String? = null,
        note: String? = null,
        frequency: String? = null,
        customDays: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ) {
        uiState = uiState.copy(
            amount = amount ?: uiState.amount,
            category = category ?: uiState.category,
            note = note ?: uiState.note,
            frequency = frequency ?: uiState.frequency,
            customDays = customDays ?: uiState.customDays,
            startDate = startDate ?: uiState.startDate,
            endDate = endDate
        )
    }

    fun clearError() {
        uiState = uiState.copy(errorMessageId = null)
    }

    fun loadForEditing(id: Long) {
        viewModelScope.launch {
            val sub = repository.getRecurringExpenseById(id)
            if (sub != null) {
                uiState = uiState.copy(
                    id = sub.id,
                    planId = sub.planId,
                    amount = sub.amount.toString(),
                    note = sub.note,
                    category = sub.category,
                    frequency = sub.frequency,
                    // [注意] 這裡對應 Entity 的 customDays
                    customDays = sub.customDays.toString(),
                    startDate = sub.startDate,
                    endDate = sub.endDate
                )
            }
        }
    }

    fun saveSubscription(onSuccess: () -> Unit) {
        val amountInt = uiState.amount.toIntOrNull()
        if (amountInt == null || amountInt <= 0) {
            uiState = uiState.copy(errorMessageId = R.string.error_msg_amount)
            return
        }
        if (uiState.category.isBlank()) {
            uiState = uiState.copy(errorMessageId = R.string.error_msg_category)
            return
        }
        if (uiState.note.isBlank()) {
            uiState = uiState.copy(errorMessageId = R.string.error_msg_note)
            return
        }

        viewModelScope.launch {
            val days = if (uiState.frequency == "CUSTOM") uiState.customDays.toIntOrNull() ?: 1 else 0

            // [新增] 防呆機制：確保日期在計畫範圍內
            // 1. 開始日期不能早於計畫開始日
            val safeStartDate = if (uiState.startDate < limitStartDate) limitStartDate else uiState.startDate

            // 2. 結束日期處理
            var safeEndDate = uiState.endDate
            if (limitEndDate > 0) { // 如果計畫有結束日
                if (safeEndDate == null || safeEndDate > limitEndDate) {
                    // 如果使用者沒設結束日，或結束日超過計畫範圍，強制設為計畫結束日
                    safeEndDate = limitEndDate
                }
            }

            // 3. 再次確認開始日期不能晚於結束日期
            val finalStartDate = if (safeEndDate != null && safeStartDate > safeEndDate) safeEndDate else safeStartDate

            val expense = RecurringExpenseEntity(
                id = if (uiState.id != -1L) uiState.id else 0,
                planId = uiState.planId,
                category = uiState.category,
                note = uiState.note,
                amount = amountInt,
                frequency = uiState.frequency,
                startDate = finalStartDate, // 使用校正後的日期
                endDate = safeEndDate,      // 使用校正後的日期
                customDays = days,
                dayOfMonth = 1
            )

            if (uiState.id != -1L) {
                repository.updateRecurringExpense(expense)
            } else {
                repository.insertRecurringExpense(expense)
            }

            // [新增] 儲存後立即觸發檢查，生成今日消費
            repository.checkAndGenerateRecurringExpenses()

            onSuccess()
            uiState = SubscriptionUiState(planId = uiState.planId)
        }
    }

    fun deleteSubscription(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(item)
            // 刪除後不需要特別生成，除非您想把已生成的未來消費也刪除(通常不建議)
        }
    }

    // Categories & Tags logic
    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch { repository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex)) }
    }
    fun deleteCategory(cat: CategoryEntity) { viewModelScope.launch { repository.deleteCategory(cat) } }
    fun toggleCategoryVisibility(cat: CategoryEntity) { viewModelScope.launch { repository.updateCategory(cat.copy(isVisible = !cat.isVisible)) } }

    fun addSubTag(name: String) { viewModelScope.launch { repository.insertSubTag(SubscriptionTagEntity(name = name)) } }
    fun deleteSubTag(tag: SubscriptionTagEntity) { viewModelScope.launch { repository.deleteSubTag(tag) } }
    fun toggleSubTagVisibility(tag: SubscriptionTagEntity) { viewModelScope.launch { repository.updateSubTag(tag.copy(isVisible = !tag.isVisible)) } }
}