package com.example.budgetquest.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PaymentMethodEntity
import com.example.budgetquest.data.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 定義 UI 狀態
data class TransactionUiState(
    val id: Long = 0,
    val amount: String = "",
    val note: String = "",
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val paymentMethod: String = "",
    val merchant: String = "",
    val excludeFromBudget: Boolean = false,
    val isNeed: Boolean? = null,
    val errorMessageId: Int? = null
)

class TransactionViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private var currentEditingId: Long = -1L

    val visibleCategories = repository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visibleTags = repository.getVisibleTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = repository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTags = repository.getAllTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // [新增] 讀取支付方式列表
    val visiblePaymentMethods = repository.getVisiblePaymentMethodsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaymentMethods = repository.getAllPaymentMethodsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- 表單操作 ---
    fun clearError() { _uiState.update { it.copy(errorMessageId = null) } }
    fun updateAmount(newAmount: String) { if (newAmount.all { it.isDigit() }) { _uiState.update { it.copy(amount = newAmount, errorMessageId = null) } } }
    fun updateNote(newNote: String) { _uiState.update { it.copy(note = newNote) } }
    fun updateCategory(newCategory: String) { _uiState.update { it.copy(category = newCategory) } }
    fun updateImageUri(uri: String?) { _uiState.update { it.copy(imageUri = uri) } }
    // [修正] 更新支付方式 (支援 Toggle：點第二次取消)
    fun updatePaymentMethod(method: String) {
        _uiState.update { currentState ->
            if (currentState.paymentMethod == method) {
                currentState.copy(paymentMethod = "") // 點擊相同 -> 清空
            } else {
                currentState.copy(paymentMethod = method)
            }
        }
    }
    fun updateMerchant(merchant: String) { _uiState.update { it.copy(merchant = merchant) } }
    fun updateExcludeFromBudget(exclude: Boolean) { _uiState.update { it.copy(excludeFromBudget = exclude) } }

    fun toggleNeedStatus(targetState: Boolean) {
        _uiState.update { currentState ->
            if (currentState.isNeed == targetState) currentState.copy(isNeed = null) else currentState.copy(isNeed = targetState)
        }
    }

    fun reset() {
        currentEditingId = -1L
        _uiState.value = TransactionUiState()
    }

    fun setDate(timestamp: Long) { _uiState.update { it.copy(date = timestamp) } }

    fun loadExpense(id: Long) {
        currentEditingId = id
        viewModelScope.launch {
            val expense = repository.getExpenseById(id)
            if (expense != null) {
                _uiState.value = TransactionUiState(
                    id = expense.id,
                    amount = expense.amount.toString(),
                    note = expense.note,
                    category = expense.category,
                    date = expense.date,
                    imageUri = expense.imageUri,
                    paymentMethod = expense.paymentMethod,
                    merchant = expense.merchant,
                    excludeFromBudget = expense.excludeFromBudget,
                    isNeed = expense.isNeed
                )
            }
        }
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amountInt = state.amount.toIntOrNull() ?: 0

        if (amountInt <= 0) { _uiState.update { it.copy(errorMessageId = R.string.error_msg_amount) }; return }
        if (state.category.isBlank()) { _uiState.update { it.copy(errorMessageId = R.string.error_msg_category) }; return }
        if (state.note.isBlank()) { _uiState.update { it.copy(errorMessageId = R.string.error_msg_note) }; return }

        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = if (currentEditingId == -1L) 0 else currentEditingId,
                amount = amountInt,
                note = state.note,
                date = state.date,
                category = state.category,
                imageUri = state.imageUri,
                paymentMethod = state.paymentMethod,
                merchant = state.merchant,
                excludeFromBudget = state.excludeFromBudget,
                isNeed = state.isNeed,
                // [修正] 加入 planId，設為 -1 (預設)
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

    // --- 分類與標籤管理 ---
    fun addCategory(name: String, iconKey: String, colorHex: String) { viewModelScope.launch { repository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex)) } }
    fun deleteCategory(cat: CategoryEntity) { viewModelScope.launch { repository.deleteCategory(cat) } }
    fun toggleCategoryVisibility(cat: CategoryEntity) { viewModelScope.launch { repository.updateCategory(cat.copy(isVisible = !cat.isVisible)) } }
    fun addTag(name: String) { viewModelScope.launch { repository.insertTag(TagEntity(name = name)) } }
    fun deleteTag(tag: TagEntity) { viewModelScope.launch { repository.deleteTag(tag) } }
    fun toggleTagVisibility(tag: TagEntity) { viewModelScope.launch { repository.updateTag(tag.copy(isVisible = !tag.isVisible)) } }
    fun deleteExpense(id: Long) { viewModelScope.launch { val expense = repository.getExpenseById(id); if (expense != null) repository.deleteExpense(expense) } }

    // [新增] 支付方式管理功能
    fun addPaymentMethod(name: String) {
        viewModelScope.launch { repository.insertPaymentMethod(PaymentMethodEntity(name = name)) }
    }
    fun deletePaymentMethod(pm: PaymentMethodEntity) {
        viewModelScope.launch { repository.deletePaymentMethod(pm) }
    }
    fun togglePaymentMethodVisibility(pm: PaymentMethodEntity) {
        viewModelScope.launch { repository.updatePaymentMethod(pm.copy(isVisible = !pm.isVisible)) }
    }

}