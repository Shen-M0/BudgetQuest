package com.example.budgetquest.ui.plan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.R // [確保 import]
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// 1. 定義 Dashboard 需要的狀態 (唯讀，給首頁用)
data class PlanUiState(
    val currentPlan: PlanEntity? = null,
    val dailyLimit: Int = 0,
    val isLoading: Boolean = true
)

// PlanSetupState (表單用)
// [維持上一階段優化] 使用 Int? 儲存錯誤訊息的 Resource ID
data class PlanSetupState(
    val id: Int = 0,
    val planName: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 86400000L * 30,
    val totalBudget: String = "",
    val targetSavings: String = "",
    val errorMessageId: Int? = null
)

class PlanViewModel(private val repository: BudgetRepository) : ViewModel() {

    // [Bug 修復] 新增：指定要查看的計畫 ID (null 代表預設查看進行中計畫)
    private val _viewingPlanId = MutableStateFlow<Int?>(null)

    // [Bug 修復] DashboardScreen 進入時必須呼叫此函式
    fun setViewingPlanId(id: Int) {
        _viewingPlanId.value = if (id == -1) null else id
    }

    // [Bug 修復] 修改資料流來源
    // 使用 combine 結合 "指定ID" 與 "所有計畫"，動態決定要顯示哪一個
    val uiState: StateFlow<PlanUiState> = combine(
        _viewingPlanId,
        repository.getAllPlansStream()
    ) { targetId, plans ->
        // 1. 決定要顯示哪個計畫
        val plan = if (targetId != null) {
            // 如果有指定 ID (例如從歷史紀錄進來)，就找那個計畫
            plans.find { it.id == targetId }
        } else {
            // 如果沒指定 (Dashboard 預設)，就找當前進行中的計畫
            val today = System.currentTimeMillis()
            plans.find { it.isActive && today >= it.startDate && today <= it.endDate }
        }

        // 2. 計算邏輯 (保持不變)
        if (plan != null) {
            val diff = plan.endDate - plan.startDate
            val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            val safeDays = if (days > 0) days else 1
            val daily = (plan.totalBudget - plan.targetSavings) / safeDays
            PlanUiState(currentPlan = plan, dailyLimit = daily, isLoading = false)
        } else {
            PlanUiState(currentPlan = null, dailyLimit = 0, isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanUiState(isLoading = true)
    )

    // --- Plan Setup Logic (表單邏輯) ---

    var planUiState by mutableStateOf(PlanSetupState())
        private set

    fun initDates(start: Long, end: Long) {
        if (start != -1L) {
            val finalEnd = if (end != -1L) end else start + 86400000L * 30
            planUiState = planUiState.copy(startDate = start, endDate = finalEnd)
        } else {
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endOfMonth = calendar.timeInMillis
            planUiState = planUiState.copy(startDate = today, endDate = endOfMonth)
        }
    }

    fun updatePlanState(
        planName: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        totalBudget: String? = null,
        targetSavings: String? = null
    ) {
        planUiState = planUiState.copy(
            planName = planName ?: planUiState.planName,
            startDate = startDate ?: planUiState.startDate,
            endDate = endDate ?: planUiState.endDate,
            totalBudget = totalBudget ?: planUiState.totalBudget,
            targetSavings = targetSavings ?: planUiState.targetSavings,
            errorMessageId = null // 清除錯誤
        )
    }

    fun loadPlan(id: Int) {
        viewModelScope.launch {
            val plan = repository.getPlanById(id)
            if (plan != null) {
                planUiState = PlanSetupState(
                    id = plan.id,
                    planName = plan.planName,
                    startDate = plan.startDate,
                    endDate = plan.endDate,
                    totalBudget = plan.totalBudget.toString(),
                    targetSavings = plan.targetSavings.toString()
                )
            }
        }
    }

    fun savePlan(onSuccess: (Int) -> Unit) {
        val currentState = planUiState
        if (currentState.planName.isBlank() || currentState.totalBudget.isBlank()) {
            // [提取] 使用 Resource ID
            planUiState = planUiState.copy(errorMessageId = R.string.error_empty_plan_fields)
            return
        }

        viewModelScope.launch {
            val allPlans = repository.getAllPlans()
            val hasOverlap = allPlans.any { existing ->
                if (existing.id == currentState.id) return@any false
                (currentState.startDate <= existing.endDate) && (currentState.endDate >= existing.startDate)
            }

            if (hasOverlap) {
                // [提取] 使用 Resource ID
                planUiState = planUiState.copy(errorMessageId = R.string.error_plan_overlap)
            } else {
                val plan = PlanEntity(
                    id = currentState.id,
                    planName = currentState.planName,
                    startDate = currentState.startDate,
                    endDate = currentState.endDate,
                    totalBudget = currentState.totalBudget.toIntOrNull() ?: 0,
                    targetSavings = currentState.targetSavings.toIntOrNull() ?: 0,
                    isActive = true
                )

                val savedId = if (currentState.id == 0) {
                    repository.insertPlan(plan).toInt()
                } else {
                    repository.updatePlan(plan)
                    plan.id
                }
                onSuccess(savedId)
            }
        }
    }

    fun deletePlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val plan = repository.getPlanById(planUiState.id)
            if (plan != null) {
                repository.deletePlan(plan)
                onSuccess()
            }
        }
    }

    // [新增] 清空目前計畫範圍內的所有消費紀錄
    fun clearPlanExpenses() {
        val start = planUiState.startDate
        val end = planUiState.endDate

        viewModelScope.launch {
            repository.deleteExpensesByRange(start, end)
            // 這裡不需要額外的 onSuccess callback，因為刪除後 UI 不會跳轉
            // 且因為 Repository 使用 Flow，相關的觀察者 (如月曆頁面) 會自動更新
        }
    }



}