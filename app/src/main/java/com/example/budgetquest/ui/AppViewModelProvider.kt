package com.example.budgetquest.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetquest.BudgetQuestApplication
import com.example.budgetquest.ui.dashboard.DashboardViewModel
import com.example.budgetquest.ui.plan.PlanViewModel
import com.example.budgetquest.ui.subscription.SubscriptionViewModel
import com.example.budgetquest.ui.summary.SummaryViewModel
import com.example.budgetquest.ui.transaction.TransactionViewModel
import com.example.budgetquest.ui.history.PlanHistoryViewModel
import com.example.budgetquest.ui.subscription.SubscriptionDetailViewModel
import com.example.budgetquest.ui.transaction.DailyDetailViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        // 1. PlanViewModel
        initializer {
            PlanViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // 2. DashboardViewModel
        initializer {
            DashboardViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // 3. TransactionViewModel
        initializer {
            TransactionViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // 4. SummaryViewModel
        initializer {
            SummaryViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // 5. SubscriptionViewModel (新加入的)
        initializer {
            SubscriptionViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // [關鍵修正] 加入這一段！
        initializer {
            PlanHistoryViewModel(budgetQuestApplication().container.budgetRepository)
        }
        // [關鍵修復] 加入 DailyDetailViewModel 的初始化邏輯
        initializer {
            DailyDetailViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // [新增這一段] 修復閃退的關鍵
        initializer {
            SubscriptionDetailViewModel(budgetQuestApplication().container.budgetRepository)
        }

    }
}

/**
 * 擴充函式：讓 CreationExtras 可以直接存取到我們的 BudgetQuestApplication
 */
fun CreationExtras.budgetQuestApplication(): BudgetQuestApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as BudgetQuestApplication)