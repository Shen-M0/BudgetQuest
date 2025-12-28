package com.example.budgetquest.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.flow.*

data class PlanHistoryItem(val plan: PlanEntity, val totalSpent: Int)

class PlanHistoryViewModel(private val repository: BudgetRepository) : ViewModel() {

    // [關鍵修正] 使用 combine 結合兩個資料流，實現 Real-time 更新
    val allPlans: StateFlow<List<PlanHistoryItem>> = combine(
        repository.getAllPlansStream(),    // 需確認 Repository 有這個 Flow
        repository.getAllExpensesStream()  // 這是之前用過的，應該有
    ) { plans, allExpenses ->

        Log.d("PlanHistoryVM", "資料變動，重新計算歷史紀錄...")

        plans.map { plan ->
            // 針對每個計畫，篩選出該時間範圍內的消費並加總
            val spent = allExpenses
                .filter { it.date >= plan.startDate && it.date <= plan.endDate }
                .sumOf { it.amount }

            PlanHistoryItem(plan, spent)
        }
            .sortedByDescending { it.plan.startDate } // 按日期新到舊排序

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}