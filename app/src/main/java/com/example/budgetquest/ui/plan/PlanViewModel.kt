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

// 用來管理 UI 輸入框的狀態
// [修改] PlanFormState 移到這裡並增加 planName
data class PlanFormState(
    val id: Int = 0,
    val totalBudget: String = "",
    val targetSavings: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val planName: String = "" // [新增]
)

// 原本的 UiState (用於顯示目前計畫狀態)
data class PlanUiState(
    val currentPlan: PlanEntity? = null,
    val dailyLimit: Int = 0,
    val isLoading: Boolean = true // [新增] 預設為載入中
)

class PlanViewModel(private val budgetRepository: BudgetRepository) : ViewModel() {

    // 我們需要修改 uiState 的來源，確保它能反映 "載入中"
    val uiState: StateFlow<PlanUiState> = budgetRepository.getCurrentPlanStream()
        .map { plan ->
            if (plan != null) {
                val diff = plan.endDate - plan.startDate
                val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                val safeDays = if (days > 0) days else 1
                val daily = (plan.totalBudget - plan.targetSavings) / safeDays
                PlanUiState(currentPlan = plan, dailyLimit = daily, isLoading = false) // 載入完成
            } else {
                // 這裡要注意：如果讀取完發現真的沒計畫，isLoading 也要變成 false
                // 但 Flow 第一次發射 null 可能是因為還沒連上 DB。
                // 為了簡單起見，我們假設 Room 回傳第一次資料就是準確的
                PlanUiState(currentPlan = null, dailyLimit = 0, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanUiState(isLoading = true) // [關鍵] 初始值設為 true
        )

    var formState by mutableStateOf(PlanFormState())
        private set

    fun initializeForm(planId: Int?, defaultStartDate: Long = -1L) {
        if (planId != null && planId != -1) {
            viewModelScope.launch {
                val plan = budgetRepository.getPlanById(planId)
                if (plan != null) {
                    formState = PlanFormState(
                        id = plan.id,
                        totalBudget = plan.totalBudget.toString(),
                        targetSavings = plan.targetSavings.toString(),
                        startDate = plan.startDate,
                        endDate = plan.endDate,
                        planName = plan.planName // [新增]
                    )
                }
            }
        } else {
            viewModelScope.launch {
                val calendar = Calendar.getInstance()

                val rawStart = if (defaultStartDate != -1L) defaultStartDate else calendar.timeInMillis
                calendar.timeInMillis = rawStart
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val startMillis = calendar.timeInMillis

                val currentMonth = calendar.get(Calendar.MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                var endMillis = calendar.timeInMillis

                val allPlans = budgetRepository.getAllPlans()
                val nextPlan = allPlans
                    .filter { it.startDate > startMillis }
                    .minByOrNull { it.startDate }

                if (nextPlan != null) {
                    val nextStartCal = Calendar.getInstance().apply { timeInMillis = nextPlan.startDate }
                    nextStartCal.add(Calendar.DAY_OF_YEAR, -1)
                    nextStartCal.set(Calendar.HOUR_OF_DAY, 23); nextStartCal.set(Calendar.MINUTE, 59); nextStartCal.set(Calendar.SECOND, 59); nextStartCal.set(Calendar.MILLISECOND, 999)

                    if (nextStartCal.timeInMillis >= startMillis) {
                        endMillis = nextStartCal.timeInMillis
                    }
                }

                formState = PlanFormState(
                    id = 0,
                    totalBudget = "",
                    targetSavings = "",
                    startDate = startMillis,
                    endDate = endMillis,
                    planName = "" // [新增]
                )
            }
        }
    }

    fun updateFormState(
        totalBudget: String? = null,
        targetSavings: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        planName: String? = null // [新增]
    ) {
        formState = formState.copy(
            totalBudget = totalBudget ?: formState.totalBudget,
            targetSavings = targetSavings ?: formState.targetSavings,
            startDate = startDate ?: formState.startDate,
            endDate = endDate ?: formState.endDate,
            planName = planName ?: formState.planName // [新增]
        )
    }

    // [新增] 錯誤訊息狀態
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun savePlanWithCallback(onResult: (Boolean) -> Unit) {
        val budget = formState.totalBudget.toIntOrNull() ?: 0
        val savings = formState.targetSavings.toIntOrNull() ?: 0
        val finalName = if (formState.planName.isBlank()) "我的存錢計畫" else formState.planName

        if (budget > 0) {
            viewModelScope.launch {
                // 1. 檢查重疊 (Overlap Check)
                // 抓取所有計畫
                val allPlans = budgetRepository.getAllPlans()

                // 判斷是否有重疊 (排除自己，如果是編輯模式)
                val isOverlap = allPlans.any { existingPlan ->
                    if (existingPlan.id == formState.id) return@any false // 排除自己

                    // 重疊邏輯：(新開始 <= 舊結束) AND (新結束 >= 舊開始)
                    // 使用當天 00:00 和 23:59 進行寬鬆比對
                    val newStart = getStartOfDay(formState.startDate)
                    val newEnd = getEndOfDay(formState.endDate)
                    val oldStart = getStartOfDay(existingPlan.startDate)
                    val oldEnd = getEndOfDay(existingPlan.endDate)

                    newStart <= oldEnd && newEnd >= oldStart
                }

                if (isOverlap) {
                    errorMessage = "此日期範圍與現有計畫重疊！"
                    onResult(false) // [回傳失敗]
                    return@launch
                }

                // 2. 沒有重疊，繼續存檔
                errorMessage = null // 清除錯誤
                val plan = PlanEntity(
                    id = formState.id,
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    totalBudget = budget,
                    targetSavings = savings,
                    isActive = true,
                    planName = finalName
                )

                if (formState.id == 0) budgetRepository.insertPlan(plan)
                else budgetRepository.updatePlan(plan)

                onResult(true) // [回傳成功]
            }
        }else {
            onResult(false)
        }


    }


    // 輔助函式
    private fun getStartOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        return c.timeInMillis
    }
    private fun getEndOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
        return c.timeInMillis
    }



}