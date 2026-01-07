package com.example.budgetquest.ui.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.CurrencyRepository
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PaymentMethodEntity
import com.example.budgetquest.data.SettingsRepository
import com.example.budgetquest.data.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

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

class TransactionViewModel(
    private val repository: BudgetRepository,
    private val settingsRepository: SettingsRepository, // [新增] 設定 Repository
    private val currencyRepository: CurrencyRepository  // [新增] 匯率 Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private var currentEditingId: Long = -1L

    // [新增] 幣別相關 State (使用 Compose State 以便 UI 即時更新)
    var baseCurrency by mutableStateOf("TWD") // 主幣別 (從設定讀取)
        private set

    var inputCurrency by mutableStateOf("TWD") // 使用者目前選擇的輸入幣別
        private set

    var supportedCurrencies by mutableStateOf<List<String>>(emptyList()) // 幣別選單
        private set

    // [新增] 轉換後的預覽文字 (例如 "≈ 100 TWD")
    var convertedPreview by mutableStateOf("")
        private set

    // 真正的存檔金額 (換算回主幣別後的數值)
    private var finalSaveAmount: Int = 0

    init {
        // [新增] 初始化幣別資料
        viewModelScope.launch {
            // 1. 載入主幣別
            baseCurrency = settingsRepository.baseCurrency
            inputCurrency = baseCurrency // 預設輸入幣別 = 主幣別

            // 2. 載入幣別清單
            val currencies = currencyRepository.getSupportedCurrencies()
            if (currencies.isEmpty()) {
                supportedCurrencies = listOf(baseCurrency) // 至少要有主幣別
            } else {
                supportedCurrencies = currencies
            }
        }
    }

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

    // [修改] 更新金額時，同時觸發匯率計算
    fun updateAmount(newAmount: String) {
        if (newAmount.all { it.isDigit() }) {
            _uiState.update { it.copy(amount = newAmount, errorMessageId = null) }
            calculateConversion() // [新增] 計算匯率
        }
    }

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

    // [新增] 使用者改變輸入幣別
    fun updateInputCurrency(currency: String) {
        inputCurrency = currency
        calculateConversion()
    }

    // [新增] 計算匯率邏輯
    private fun calculateConversion() {
        val inputVal = _uiState.value.amount.toDoubleOrNull() ?: 0.0

        if (inputCurrency == baseCurrency) {
            // 如果幣別相同，不需要轉換
            finalSaveAmount = inputVal.toInt()
            convertedPreview = ""
        } else {
            viewModelScope.launch {
                // 從資料庫取得該幣別的匯率 (相對於主幣別)
                // 假設資料庫存的是 TWD -> JPY = 4.6
                // 輸入 JPY 要換回 TWD，就是 input / 4.6
                val rate = currencyRepository.getRateFor(inputCurrency)

                // 計算：外幣 / 匯率 = 主幣別
                // 避免除以 0
                val converted = if (rate != 0.0) inputVal / rate else inputVal

                finalSaveAmount = converted.toInt()

                // 格式化顯示
                val df = DecimalFormat("#,##0")
                convertedPreview = "≈ ${df.format(finalSaveAmount)} $baseCurrency"
            }
        }
    }

    fun reset() {
        currentEditingId = -1L
        _uiState.value = TransactionUiState()
        // 重置幣別
        inputCurrency = baseCurrency
        convertedPreview = ""
        finalSaveAmount = 0
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
                // 載入時，預設幣別為主幣別，金額不變
                finalSaveAmount = expense.amount
                inputCurrency = baseCurrency
                convertedPreview = ""
            }
        }
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        // [修改] 如果沒有經過轉換(同幣別)，就直接用輸入框的值；如果有轉換，用 finalSaveAmount
        val finalAmount = if (inputCurrency == baseCurrency) {
            state.amount.toIntOrNull() ?: 0
        } else {
            finalSaveAmount
        }

        if (finalAmount <= 0) { _uiState.update { it.copy(errorMessageId = R.string.error_msg_amount) }; return }
        if (state.category.isBlank()) { _uiState.update { it.copy(errorMessageId = R.string.error_msg_category) }; return }
        if (state.note.isBlank()) { _uiState.update { it.copy(errorMessageId = R.string.error_msg_note) }; return }

        // [修改] 處理備註：如果幣別不同，把原始金額加到備註後面
        val finalNote = if (inputCurrency != baseCurrency) {
            "${state.note} (${state.amount} $inputCurrency)"
        } else {
            state.note
        }

        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = if (currentEditingId == -1L) 0 else currentEditingId,
                amount = finalAmount, // 存入轉換後的金額
                note = finalNote, // 存入包含原始金額的備註
                date = state.date,
                category = state.category,
                imageUri = state.imageUri,
                paymentMethod = state.paymentMethod,
                merchant = state.merchant,
                excludeFromBudget = state.excludeFromBudget,
                isNeed = state.isNeed,
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