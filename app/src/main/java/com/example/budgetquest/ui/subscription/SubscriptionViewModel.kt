package com.example.budgetquest.ui.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.RecurringExpenseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.SubscriptionTagEntity
import kotlinx.coroutines.flow.stateIn


class SubscriptionViewModel(private val repository: BudgetRepository) : ViewModel() {

    val recurringList: StateFlow<List<RecurringExpenseEntity>> = repository.getAllRecurringStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State for Adding
    var uiState by mutableStateOf(SubscriptionUiState())
        private set

    // [新增] 支援 endDate 更新
    fun updateUiState(
        amount: String? = null,
        note: String? = null,
        category: String? = null,
        startDate: Long? = null,
        frequency: String? = null,
        customDays: String? = null,
        endDate: Long? = null // 新增
    ) {
        uiState = uiState.copy(
            amount = amount ?: uiState.amount,
            note = note ?: uiState.note,
            category = category ?: uiState.category,
            startDate = startDate ?: uiState.startDate,
            frequency = frequency ?: uiState.frequency,
            customDays = customDays ?: uiState.customDays,
            endDate = if (endDate != null) endDate else uiState.endDate
        )
    }

    // [修改] initialize 接收兩個參數：開始日, 結束日
    // initialize 保持上次給你的版本 (接收 start, end)
    fun initialize(defaultDate: Long, defaultEndDate: Long) {
        if (defaultDate != -1L) {
            val finalEndDate = if (defaultEndDate != -1L) defaultEndDate else null
            uiState = uiState.copy(startDate = defaultDate, endDate = finalEndDate)
        }
    }
    fun addSubscription() {
        val amountInt = uiState.amount.toIntOrNull()
        val customDaysInt = uiState.customDays.toIntOrNull() ?: 0

        if (amountInt != null && amountInt > 0 && uiState.note.isNotBlank()) {
            viewModelScope.launch {
                val initialLastGenerated = calculateInitialLastGeneratedDate(uiState.startDate, uiState.frequency, customDaysInt)

                repository.addRecurring(
                    RecurringExpenseEntity(
                        amount = amountInt,
                        note = uiState.note,
                        category = uiState.category,
                        startDate = uiState.startDate,
                        endDate = uiState.endDate,
                        frequency = uiState.frequency,
                        customDays = customDaysInt,
                        lastGeneratedDate = initialLastGenerated
                    )
                )

                // [關鍵修正] 新增完訂閱後，立刻執行一次檢查！
                // 這樣如果開始日期是昨天，Dashboard 才會立刻扣款
                repository.checkAndGenerateRecurringExpenses()

                uiState = SubscriptionUiState()
            }
        }
    }

    fun deleteSubscription(item: RecurringExpenseEntity) {
        viewModelScope.launch { repository.deleteRecurring(item) }
    }

    // 計算初始的 "上一次時間"，好讓第一次扣款發生在 startDate
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

    // [新增] 共用的分類
    val visibleCategories = repository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = repository.getAllCategoriesStream() // 管理用
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [新增] 訂閱專用的備註 (SubTags)
    val visibleSubTags = repository.getVisibleSubTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSubTags = repository.getAllSubTagsStream() // 管理用
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [新增] CRUD
    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch { repository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex)) }
    }
    fun toggleCategoryVisibility(category: CategoryEntity) {
        viewModelScope.launch { repository.updateCategory(category.copy(isVisible = !category.isVisible)) }
    }
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    // 訂閱標籤 CRUD
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

data class SubscriptionUiState(
    val amount: String = "",
    val note: String = "",
    val category: String = "娛樂",
    val startDate: Long = System.currentTimeMillis(),
    val frequency: String = "MONTH",
    val customDays: String = "30",
    val endDate: Long? = null // 確保有這個欄位
)