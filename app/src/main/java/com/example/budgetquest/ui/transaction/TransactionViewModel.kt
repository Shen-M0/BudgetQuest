package com.example.budgetquest.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 定義 UI 狀態
data class TransactionUiState(
    val id: Long = 0,
    val amount: String = "",
    val note: String = "",
    val category: String = "", // 預設分類
    val date: Long = System.currentTimeMillis(),

    // [新增] 進階欄位狀態
    val imageUri: String? = null,
    val paymentMethod: String = "Cash", // 預設現金
    val merchant: String = "",
    val excludeFromBudget: Boolean = false,
    val isNeed: Boolean = true, // 預設為「需要」

    // 錯誤訊息 ID
    val errorMessageId: Int? = null
)

class TransactionViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // 用來記錄當前編輯的 ID (-1 代表新增)
    private var currentEditingId: Long = -1L

    // 讀取分類與標籤 (用於下拉或列表)
    val visibleCategories = repository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleTags = repository.getVisibleTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 讀取所有分類與標籤 (用於管理頁面)
    val allCategories = repository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags = repository.getAllTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 表單操作 ---

    // 清除錯誤訊息的輔助函式
    fun clearError() {
        _uiState.update { it.copy(errorMessageId = null) }
    }

    fun updateAmount(newAmount: String) {
        if (newAmount.all { it.isDigit() }) {
            _uiState.update { it.copy(amount = newAmount, errorMessageId = null) }
        }
    }

    fun updateNote(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
    }

    fun updateCategory(newCategory: String) {
        _uiState.update { it.copy(category = newCategory) }
    }

    // [新增] 更新圖片 URI
    fun updateImageUri(uri: String?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    // [新增] 更新支付方式
    fun updatePaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    // [新增] 更新店家/地點
    fun updateMerchant(merchant: String) {
        _uiState.update { it.copy(merchant = merchant) }
    }

    // [新增] 更新是否計入預算
    fun updateExcludeFromBudget(exclude: Boolean) {
        _uiState.update { it.copy(excludeFromBudget = exclude) }
    }

    // [新增] 更新消費性質 (Need/Want)
    fun updateIsNeed(isNeed: Boolean) {
        _uiState.update { it.copy(isNeed = isNeed) }
    }

    fun reset() {
        currentEditingId = -1L
        // 重置為預設狀態，包含新欄位的預設值
        _uiState.value = TransactionUiState()
    }

    // 設定初始日期
    fun setDate(timestamp: Long) {
        _uiState.update { it.copy(date = timestamp) }
    }

    fun loadExpense(id: Long) {
        currentEditingId = id // 必須記錄當前編輯的 ID
        viewModelScope.launch {
            val expense = repository.getExpenseById(id)
            if (expense != null) {
                _uiState.value = TransactionUiState(
                    id = expense.id,
                    amount = expense.amount.toString(),
                    note = expense.note,
                    category = expense.category,
                    date = expense.date,
                    // [新增] 載入進階欄位
                    imageUri = expense.imageUri,
                    paymentMethod = expense.paymentMethod,
                    merchant = expense.merchant ?: "", // DB 若為 null 轉為空字串給 UI
                    excludeFromBudget = expense.excludeFromBudget,
                    isNeed = expense.isNeed
                )
            }
        }
    }

    // 儲存邏輯
    fun saveExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amountInt = state.amount.toIntOrNull() ?: 0

        // 1. 檢查金額
        if (amountInt <= 0) {
            _uiState.update { it.copy(errorMessageId = R.string.error_msg_amount) }
            return
        }

        // 2. 檢查分類
        if (state.category.isBlank()) {
            _uiState.update { it.copy(errorMessageId = R.string.error_msg_category) }
            return
        }

        // 3. 檢查備註
        if (state.note.isBlank()) {
            _uiState.update { it.copy(errorMessageId = R.string.error_msg_note) }
            return
        }

        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = if (currentEditingId == -1L) 0 else currentEditingId,
                amount = amountInt,
                note = state.note,
                date = state.date,
                category = state.category,
                // [新增] 儲存進階欄位
                imageUri = state.imageUri,
                paymentMethod = state.paymentMethod,
                merchant = if (state.merchant.isBlank()) null else state.merchant,
                excludeFromBudget = state.excludeFromBudget,
                isNeed = state.isNeed,
                // planId 維持預設 (目前邏輯是 -1 或依賴 DB 預設值，如果需要關聯 Plan 可在此擴充)
                planId = -1
            )

            if (currentEditingId == -1L) {
                repository.insertExpense(expense)
            } else {
                repository.updateExpense(expense)
            }

            onSuccess()
        }
    }

    // --- 分類與標籤管理 (供 Dialog 使用) ---

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
    fun deleteCategory(cat: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(cat) }
    }
    fun toggleCategoryVisibility(cat: CategoryEntity) {
        viewModelScope.launch { repository.updateCategory(cat.copy(isVisible = !cat.isVisible)) }
    }

    fun addTag(name: String) {
        viewModelScope.launch { repository.insertTag(TagEntity(name = name)) }
    }
    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch { repository.deleteTag(tag) }
    }
    fun toggleTagVisibility(tag: TagEntity) {
        viewModelScope.launch { repository.updateTag(tag.copy(isVisible = !tag.isVisible)) }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            val expense = repository.getExpenseById(id)
            if (expense != null) {
                repository.deleteExpense(expense)
            }
        }
    }



}