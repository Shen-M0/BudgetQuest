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
import java.util.Calendar
import com.example.budgetquest.R // [新增] Import R

// 1. 定義 Dashboard 需要的狀態
data class PlanUiState(
    val currentPlan: PlanEntity? = null,
    val dailyLimit: Int = 0,
    val isLoading: Boolean = true
)

// [修改] errorMessage 改為儲存 Resource ID (Int?)
data class PlanSetupState(
    val id: Int = 0,
    val planName: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 86400000L * 30,
    val totalBudget: String = "",
    val targetSavings: String = "",
    val errorMessageId: Int? = null // [修改] 改名為 Id 以示區別
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

    // --- Plan Setup Logic ---

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
            errorMessageId = null // [修改] 清除錯誤 ID
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
            // [提取] 設定錯誤訊息 ID
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
                // [提取] 設定錯誤訊息 ID
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
}