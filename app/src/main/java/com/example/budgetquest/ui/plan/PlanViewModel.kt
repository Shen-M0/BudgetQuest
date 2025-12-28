package com.example.budgetquest.ui.plan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar // [新增] 記得 import Calendar
// 1. 定義 Dashboard 需要的狀態 (唯讀，給首頁用)
data class PlanUiState(
    val currentPlan: PlanEntity? = null,
    val dailyLimit: Int = 0,
    val isLoading: Boolean = true
)

// [修改] PlanSetupState 增加錯誤訊息欄位
data class PlanSetupState(
    val id: Int = 0,
    val planName: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 86400000L * 30,
    val totalBudget: String = "",
    val targetSavings: String = "",
    val errorMessage: String? = null // [新增]
)

class PlanViewModel(private val repository: BudgetRepository) : ViewModel() {

    // --- Dashboard Logic (保持不變) ---
    val uiState: StateFlow<PlanUiState> = repository.getCurrentPlanStream()
        .map { plan ->
            if (plan != null) {
                val diff = plan.endDate - plan.startDate
                val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                val safeDays = if (days > 0) days else 1
                val daily = (plan.totalBudget - plan.targetSavings) / safeDays
                PlanUiState(currentPlan = plan, dailyLimit = daily, isLoading = false)
            } else {
                PlanUiState(currentPlan = null, dailyLimit = 0, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanUiState(isLoading = true)
        )

    // --- [新增] Plan Setup Logic (表單邏輯) ---

    // 使用 Compose State 來管理表單輸入，因為它是暫時的 UI 狀態
    var planUiState by mutableStateOf(PlanSetupState())
        private set

    // [修改] 預設日期邏輯
    fun initDates(start: Long, end: Long) {
        if (start != -1L) {
            // 情境 A: 從月曆點擊空白處進入 (有指定日期)
            val finalEnd = if (end != -1L) end else start + 86400000L * 30
            planUiState = planUiState.copy(startDate = start, endDate = finalEnd)
        } else {
            // 情境 B: 第一次開啟 APP 或點擊 Dashboard 的 "+" (無指定日期)
            // 自動設定為：今天 ~ 本月最後一天
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis

            // 設定為當月最後一天
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endOfMonth = calendar.timeInMillis

            planUiState = planUiState.copy(startDate = today, endDate = endOfMonth)
        }
    }

    // 更新表單內容 (支援部分更新)
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
            errorMessage = null // 清除錯誤訊息
        )
    }

    // 載入既有計畫 (編輯模式)
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

    // 儲存計畫 (新增或更新)
    fun savePlan(onSuccess: (Int) -> Unit) {
        val currentState = planUiState
        if (currentState.planName.isBlank() || currentState.totalBudget.isBlank()) {
            planUiState = planUiState.copy(errorMessage = "請輸入計畫名稱與預算")
            return
        }

        viewModelScope.launch {
            // 防呆：日期重疊檢查
            val allPlans = repository.getAllPlans()
            val hasOverlap = allPlans.any { existing ->
                if (existing.id == currentState.id) return@any false
                (currentState.startDate <= existing.endDate) && (currentState.endDate >= existing.startDate)
            }

            if (hasOverlap) {
                planUiState = planUiState.copy(errorMessage = "日期範圍與現有計畫重疊，請重新選擇")
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
                    repository.insertPlan(plan).toInt() // 新增並取得 ID
                } else {
                    repository.updatePlan(plan)
                    plan.id // 更新，回傳原 ID
                }

                // 呼叫 callback 並傳回 ID
                onSuccess(savedId)
            }
        }
    }

    // [新增] 刪除計畫
    fun deletePlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val plan = repository.getPlanById(planUiState.id)
            if (plan != null) {
                repository.deletePlan(plan)
                onSuccess()
            }
        }
    }


}