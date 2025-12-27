package com.example.budgetquest.ui.transaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.TagEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// 1. 定義 UI 狀態
data class TransactionUiState(
    val expenseId: Long = -1L,        // -1 代表新增，其他代表編輯
    val date: Long = System.currentTimeMillis(),
    val amount: String = "",
    val category: String = "飲食",    // 預設分類
    val note: String = ""
)

// 2. 定義分類資料結構 (UI 會用到)
data class Category(val name: String, val icon: ImageVector)

// 定義全域常數，讓 TransactionScreen 可以 import
val categories = listOf(
    Category("飲食", Icons.Default.Face),
    Category("購物", Icons.Default.ShoppingCart),
    Category("交通", Icons.Default.Home),
    Category("娛樂", Icons.Default.Star),
    Category("其他", Icons.Default.Star)
)

val quickTags = listOf("早餐", "午餐", "晚餐", "飲料", "交通", "日用品")


class TransactionViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    // [新增] 從資料庫讀取可見的分類與標籤 (給 UI 選擇用)
    val visibleCategories = budgetRepository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleTags = budgetRepository.getVisibleTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [新增] 讀取所有分類與標籤 (給管理介面用)
    val allCategories = budgetRepository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags = budgetRepository.getAllTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // 更新 UI 狀態 (使用 copy 確保只更新變動的欄位)
    fun updateUiState(
        date: Long? = null,
        amount: String? = null,
        category: String? = null,
        note: String? = null
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                date = date ?: currentState.date,
                amount = amount ?: currentState.amount,
                category = category ?: currentState.category,
                note = note ?: currentState.note
            )
        }
    }

    // 讀取既有的消費紀錄 (編輯模式用)
    fun loadExpense(id: Long) {
        if (id == -1L) return // 如果是新增模式，不用讀取

        viewModelScope.launch {
            val expense = budgetRepository.getExpenseById(id)
            if (expense != null) {
                _uiState.update {
                    it.copy(
                        expenseId = expense.id,
                        date = expense.date,
                        amount = expense.amount.toString(),
                        category = expense.category,
                        note = expense.note
                    )
                }
            }
        }
    }

    // 儲存 (新增或更新)
    fun saveExpense() {
        val currentState = _uiState.value
        val amountInt = currentState.amount.toIntOrNull() ?: 0

        if (amountInt > 0 && currentState.note.isNotBlank()) {
            viewModelScope.launch {
                val expense = ExpenseEntity(
                    id = if (currentState.expenseId == -1L) 0 else currentState.expenseId, // 0=Insert, 其他=Update
                    date = currentState.date,
                    amount = amountInt,
                    category = currentState.category,
                    note = currentState.note
                )

                if (currentState.expenseId == -1L) {
                    budgetRepository.insertExpense(expense)
                } else {
                    budgetRepository.updateExpense(expense)
                }
            }
        }
    }

    // [新增] 管理功能
    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            budgetRepository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex))
        }
    }
    fun toggleCategoryVisibility(category: CategoryEntity) {
        viewModelScope.launch {
            budgetRepository.updateCategory(category.copy(isVisible = !category.isVisible))
        }
    }
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { budgetRepository.deleteCategory(category) }
    }

    fun addTag(name: String) {
        viewModelScope.launch { budgetRepository.insertTag(TagEntity(name = name)) }
    }
    fun toggleTagVisibility(tag: TagEntity) {
        viewModelScope.launch { budgetRepository.updateTag(tag.copy(isVisible = !tag.isVisible)) }
    }
    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch { budgetRepository.deleteTag(tag) }
    }

}