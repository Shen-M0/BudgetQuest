package com.example.budgetquest.ui.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.PaymentMethodEntity
import com.example.budgetquest.data.RecurringExpenseEntity
import com.example.budgetquest.data.SubscriptionTagEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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
    val errorMessageId: Int? = null,

    // [新增] 進階選項狀態
    val imageUri: String? = null,
    // [修正] 預設為空
    val paymentMethod: String = "",
    // [修正] 預設為 null
    val isNeed: Boolean? = null,
    val excludeFromBudget: Boolean = false,
    val merchant: String = ""

)

class SubscriptionViewModel(private val repository: BudgetRepository) : ViewModel() {

    var uiState by mutableStateOf(SubscriptionUiState())
        private set

    // 用來儲存計畫的邊界日期 (防呆用)
    private var limitStartDate: Long = 0L
    private var limitEndDate: Long = 0L

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

    // [新增] 支付方式列表
    val visiblePaymentMethods = repository.getVisiblePaymentMethodsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPaymentMethods = repository.getAllPaymentMethodsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())





    // [修正 1] 初始化邏輯：確保預設結束日期正確載入
    fun initialize(planId: Int, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            // 只有在新增模式下才需要重設日期，編輯模式不應覆蓋
            if (uiState.id != -1L) {
                // 如果是編輯模式，還是要抓取計畫邊界供 save 時防呆，但不更動 UI
                if (planId != -1) {
                    val plan = repository.getPlanById(planId)
                    if (plan != null) {
                        limitStartDate = plan.startDate
                        limitEndDate = plan.endDate
                    }
                }
                return@launch
            }

            var effectiveStartDate = startDate
            var effectiveEndDate = endDate
            val today = System.currentTimeMillis()

            // 1. 查詢計畫詳情來設定邊界與預設值
            if (planId != -1) {
                val plan = repository.getPlanById(planId)
                if (plan != null) {
                    // 如果傳入的參數無效，就用 Plan 的日期
                    if (effectiveStartDate <= 0) effectiveStartDate = plan.startDate
                    if (effectiveEndDate <= 0) effectiveEndDate = plan.endDate
                }
            }

            // 設定防呆邊界
            limitStartDate = effectiveStartDate
            limitEndDate = effectiveEndDate

            // 2. 決定預設開始日期
            // 判斷是否為過去的計畫
            val isPastPlan = effectiveEndDate > 0 && effectiveEndDate < today

            val defaultStart = if (isPastPlan) {
                // 過去的計畫：預設為計畫第一天
                effectiveStartDate
            } else {
                // 進行中/未來的計畫：預設為今天
                // 防呆：不能早於計畫開始，不能晚於計畫結束
                var d = today
                if (d < effectiveStartDate) d = effectiveStartDate
                if (effectiveEndDate > 0 && d > effectiveEndDate) d = effectiveEndDate
                d
            }

            // 3. 更新 UI State
            uiState = uiState.copy(
                planId = planId,
                startDate = defaultStart,
                // [關鍵修正] 只要計畫有結束日期，就預設帶入，不留空
                endDate = if (effectiveEndDate > 0) effectiveEndDate else null
            )
        }
    }

    // [修正] 增加新欄位的更新函式
    fun updateUiState(
        amount: String? = null,
        category: String? = null,
        note: String? = null,
        frequency: String? = null,
        customDays: String? = null,
        startDate: Long? = null,
        endDate: Long? = -1L,
        // 新增參數
        imageUri: String? = null,
        paymentMethod: String? = null,
        isNeed: Boolean? = null,
        excludeFromBudget: Boolean? = null,
        merchant: String? = null
    ) {
        val newEndDate = if (endDate == -1L) uiState.endDate else endDate

        val newPaymentMethod = if (paymentMethod != null) {
            if (uiState.paymentMethod == paymentMethod) "" else paymentMethod
        } else {
            uiState.paymentMethod
        }


        uiState = uiState.copy(
            amount = amount ?: uiState.amount,
            category = category ?: uiState.category,
            note = note ?: uiState.note,
            frequency = frequency ?: uiState.frequency,
            customDays = customDays ?: uiState.customDays,
            startDate = startDate ?: uiState.startDate,
            endDate = newEndDate,
            // 更新新欄位
            imageUri = imageUri ?: uiState.imageUri, // 注意：如果想設為 null，需另外處理，這裡簡化為更新非空值或維持原值，若 imageUri 傳入特定字串 "null" 代表清除也可以，或者使用獨立函式
            paymentMethod = newPaymentMethod,
            isNeed = isNeed ?: uiState.isNeed,
            excludeFromBudget = excludeFromBudget ?: uiState.excludeFromBudget,
            merchant = merchant ?: uiState.merchant
        )
    }

    fun toggleNeedStatus(targetState: Boolean) {
        if (uiState.isNeed == targetState) {
            // 再次點擊相同狀態，取消選取
            uiState = uiState.copy(isNeed = null)
        } else {
            // 選取新狀態
            uiState = uiState.copy(isNeed = targetState)
        }
    }

    // 專門處理圖片清除 (因為 updateUiState 的 null 被視為不更新)
    fun updateImageUri(uri: String?) {
        uiState = uiState.copy(imageUri = uri)
    }


    fun clearError() {
        uiState = uiState.copy(errorMessageId = null)
    }

    // [修正] 載入時讀取新欄位
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
                    customDays = sub.customDays.toString(),
                    startDate = sub.startDate,
                    endDate = sub.endDate,
                    // 載入進階選項
                    imageUri = sub.imageUri,
                    paymentMethod = sub.paymentMethod,
                    isNeed = sub.isNeed,
                    excludeFromBudget = sub.excludeFromBudget,
                    merchant = sub.merchant
                )
            }
        }
    }

    // [修正] 儲存時寫入新欄位
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
            var days = if (uiState.frequency == "CUSTOM") uiState.customDays.toIntOrNull() ?: 1 else 0
            if (uiState.frequency == "CUSTOM" && days <= 0) days = 1

            val safeStartDate = if (limitStartDate > 0 && uiState.startDate < limitStartDate) limitStartDate else uiState.startDate
            var safeEndDate = uiState.endDate
            if (limitEndDate > 0) {
                if (safeEndDate == null || safeEndDate > limitEndDate) {
                    safeEndDate = limitEndDate
                }
            }
            val finalStartDate = if (safeEndDate != null && safeStartDate > safeEndDate) safeEndDate else safeStartDate

            val expense = RecurringExpenseEntity(
                id = if (uiState.id != -1L) uiState.id else 0,
                planId = uiState.planId,
                category = uiState.category,
                note = uiState.note,
                amount = amountInt,
                frequency = uiState.frequency,
                startDate = finalStartDate,
                endDate = safeEndDate,
                customDays = days,
                dayOfMonth = 1,
                // 儲存進階選項
                imageUri = uiState.imageUri,
                paymentMethod = uiState.paymentMethod,
                isNeed = uiState.isNeed,
                excludeFromBudget = uiState.excludeFromBudget,
                merchant = uiState.merchant
            )

            if (uiState.id != -1L) {
                repository.updateRecurringExpense(expense)
            } else {
                repository.insertRecurringExpense(expense)
            }

            repository.checkAndGenerateRecurringExpenses()

            onSuccess()
            uiState = SubscriptionUiState(planId = uiState.planId)
        }
    }

    fun deleteSubscription(item: RecurringExpenseEntity) {
        viewModelScope.launch { repository.deleteRecurringExpense(item) }
    }

    // Categories & Tags logic
    fun addCategory(name: String, iconKey: String, colorHex: String) { viewModelScope.launch { repository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex)) } }
    fun deleteCategory(cat: CategoryEntity) { viewModelScope.launch { repository.deleteCategory(cat) } }
    fun toggleCategoryVisibility(cat: CategoryEntity) { viewModelScope.launch { repository.updateCategory(cat.copy(isVisible = !cat.isVisible)) } }
    fun addSubTag(name: String) { viewModelScope.launch { repository.insertSubTag(SubscriptionTagEntity(name = name)) } }
    fun deleteSubTag(tag: SubscriptionTagEntity) { viewModelScope.launch { repository.deleteSubTag(tag) } }
    fun toggleSubTagVisibility(tag: SubscriptionTagEntity) { viewModelScope.launch { repository.updateSubTag(tag.copy(isVisible = !tag.isVisible)) } }

    // [新增] 管理函式 (複製 TransactionViewModel 的即可)
    fun addPaymentMethod(name: String) { viewModelScope.launch { repository.insertPaymentMethod(PaymentMethodEntity(name = name)) } }
    fun deletePaymentMethod(pm: PaymentMethodEntity) { viewModelScope.launch { repository.deletePaymentMethod(pm) } }
    fun togglePaymentMethodVisibility(pm: PaymentMethodEntity) { viewModelScope.launch { repository.updatePaymentMethod(pm.copy(isVisible = !pm.isVisible)) } }


}