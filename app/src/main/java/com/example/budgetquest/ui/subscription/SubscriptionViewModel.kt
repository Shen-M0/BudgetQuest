package com.example.budgetquest.ui.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.RecurringExpenseEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.SubscriptionTagEntity

class SubscriptionViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _currentPlanId = MutableStateFlow(-1)

    val recurringList: StateFlow<List<RecurringExpenseEntity>> = _currentPlanId
        .flatMapLatest { planId ->
            repository.getAllRecurringStream().map { list ->
                list.filter { it.planId == planId }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var uiState by mutableStateOf(SubscriptionUiState())
        private set


    // [新增] 清除錯誤
    fun clearError() {
        uiState = uiState.copy(errorMessageId = null)
    }


    fun initialize(planId: Int, defaultDate: Long, defaultEndDate: Long) {
        _currentPlanId.value = planId

        // 這裡的 defaultDate/defaultEndDate 就是 Dashboard 傳進來的「當前計畫開始與結束日」
        // 我們將它們視為「邊界限制 (Limit)」
        val limitStart = if (defaultDate != -1L) defaultDate else System.currentTimeMillis()
        // 若沒有結束日(極少見)，給一個很久以後的時間避免錯誤
        val limitEnd = if (defaultEndDate != -1L) defaultEndDate else limitStart + 31536000000L

        uiState = SubscriptionUiState(
            planId = planId,
            startDate = limitStart, // 預設選中計畫開始日
            endDate = limitEnd,     // 預設選中計畫結束日

            // [新增] 記錄邊界，用於存檔時的防呆檢查
            limitStartDate = limitStart,
            limitEndDate = limitEnd,

            amount = "",
            note = "",
            category = "",
            frequency = "MONTH",
            customDays = "30",
            errorMessageId = null // 重置錯誤
        )
    }

    fun updateUiState(
        amount: String? = null,
        note: String? = null,
        category: String? = null,
        startDate: Long? = null,
        frequency: String? = null,
        customDays: String? = null,
        endDate: Long? = null
    ) {
        uiState = uiState.copy(
            amount = amount ?: uiState.amount,
            note = note ?: uiState.note,
            category = category ?: uiState.category,
            startDate = startDate ?: uiState.startDate,
            frequency = frequency ?: uiState.frequency,
            customDays = customDays ?: uiState.customDays,
            endDate = if (endDate != null) endDate else uiState.endDate,
            errorMessageId = null // 輸入時清除錯誤
        )
    }

    // [修改] 新增防呆驗證與錯誤訊息
    fun addSubscription(onSuccess: () -> Unit) {
        val amountInt = uiState.amount.toIntOrNull()
        val customDaysInt = uiState.customDays.toIntOrNull() ?: 0

        // 1. 檢查金額
        if (amountInt == null || amountInt <= 0) {
            uiState = uiState.copy(errorMessageId = R.string.error_msg_amount)
            return
        }

        // 2. 檢查分類
        if (uiState.category.isBlank()) {
            uiState = uiState.copy(errorMessageId = R.string.error_msg_category)
            return
        }

        // 3. 檢查備註/名稱
        if (uiState.note.isBlank()) {
            uiState = uiState.copy(errorMessageId = R.string.error_msg_sub)
            return
        }

        viewModelScope.launch {
            val safeStartDate = if (uiState.startDate < uiState.limitStartDate) uiState.limitStartDate else uiState.startDate
            val safeEndDate = if (uiState.endDate == null || uiState.endDate!! > uiState.limitEndDate) uiState.limitEndDate else uiState.endDate

            if (safeStartDate > safeEndDate!!) return@launch

            val initialLastGenerated = calculateInitialLastGeneratedDate(safeStartDate, uiState.frequency, customDaysInt)

            repository.addRecurring(
                RecurringExpenseEntity(
                    amount = amountInt,
                    note = uiState.note,
                    category = uiState.category,
                    startDate = safeStartDate,
                    endDate = safeEndDate,
                    frequency = uiState.frequency,
                    customDays = customDaysInt,
                    lastGeneratedDate = initialLastGenerated,
                    planId = uiState.planId
                )
            )

            repository.checkAndGenerateRecurringExpenses()
            // 新增成功後清除欄位與錯誤
            uiState = uiState.copy(amount = "", note = "", errorMessageId = null)
            onSuccess()
        }
    }

    fun deleteSubscription(item: RecurringExpenseEntity) {
        viewModelScope.launch { repository.deleteRecurring(item) }
    }

    private fun calculateInitialLastGeneratedDate(startDate: Long, freq: String, customDays: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = startDate }
        when(freq) {
            "MONTH" -> c.add(Calendar.MONTH, -1)
            "WEEK" -> c.add(Calendar.WEEK_OF_YEAR, -1)
            "DAY" -> c.add(Calendar.DAY_OF_YEAR, -1)
            "CUSTOM" -> c.add(Calendar.DAY_OF_YEAR, -customDays)
        }
        return c.timeInMillis
    }

    // ... 分類與標籤 (保持不變) ...
    val visibleCategories = repository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = repository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visibleSubTags = repository.getVisibleSubTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSubTags = repository.getAllSubTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [修改] 接收 名稱、圖示、顏色 三個參數
    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex
                )
            )
        }
    }
    fun toggleCategoryVisibility(category: CategoryEntity) {
        viewModelScope.launch { repository.updateCategory(category.copy(isVisible = !category.isVisible)) }
    }
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }
    fun addSubTag(name: String) {
        viewModelScope.launch { repository.insertSubTag(SubscriptionTagEntity(name = name)) }
    }
    fun toggleSubTagVisibility(tag: SubscriptionTagEntity) {
        viewModelScope.launch { repository.updateSubTag(tag.copy(isVisible = !tag.isVisible)) }
    }
    fun deleteSubTag(tag: SubscriptionTagEntity) {
        viewModelScope.launch { repository.deleteSubTag(tag) }
    }
}

// [修改] 增加 limit 欄位
data class SubscriptionUiState(
    val amount: String = "",
    val note: String = "",
    val category: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val frequency: String = "MONTH",
    val customDays: String = "30",
    val endDate: Long? = null,
    val planId: Int = -1,
    // [新增] 記錄當前計畫的邊界，用於防呆
    val limitStartDate: Long = 0L,
    val limitEndDate: Long = Long.MAX_VALUE,
    val errorMessageId: Int? = null
)

