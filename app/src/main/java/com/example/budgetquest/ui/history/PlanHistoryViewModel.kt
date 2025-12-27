package com.example.budgetquest.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

data class PlanHistoryItem(val plan: PlanEntity, val totalSpent: Int)

class PlanHistoryViewModel(private val repository: BudgetRepository) : ViewModel() {

    val allPlans: StateFlow<List<PlanHistoryItem>> = flow {
        try {
            Log.d("PlanHistoryVM", "開始讀取計畫列表...") // [Debug]

            // 1. 嘗試讀取所有計畫
            val plans = repository.getAllPlans()
            Log.d("PlanHistoryVM", "讀取到 ${plans.size} 個計畫") // [Debug] 確認是否有讀到資料

            // 2. 計算每個計畫的花費
            val items = plans.map { plan ->
                Log.d("PlanHistoryVM", "正在處理計畫: ${plan.planName}, ID: ${plan.id}") // [Debug]

                val expenses = repository.getExpensesByRangeList(plan.startDate, plan.endDate)
                val spent = expenses.sumOf { it.amount }

                Log.d("PlanHistoryVM", "計畫 ${plan.planName} 總花費: $spent") // [Debug]

                PlanHistoryItem(plan, spent)
            }

            // 3. 發送結果
            emit(items.sortedByDescending { it.plan.startDate })

        } catch (e: Exception) {
            // [關鍵] 這裡會抓到閃退的原因，並印在 Logcat
            Log.e("PlanHistoryVM", "讀取歷史紀錄失敗", e)
            emit(emptyList()) // 發生錯誤時發送空清單，防止 APP 崩潰
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}