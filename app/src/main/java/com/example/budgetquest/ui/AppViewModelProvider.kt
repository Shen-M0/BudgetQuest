package com.example.budgetquest.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetquest.BudgetQuestApplication
import com.example.budgetquest.data.SettingsRepository // [新增]
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
            val app = budgetQuestApplication()
            PlanViewModel(
                repository = app.container.budgetRepository,
                settingsRepository = com.example.budgetquest.data.SettingsRepository(app.applicationContext), // [新增]
                currencyRepository = app.currencyRepository // [新增]
            )
        }

        // 2. DashboardViewModel
        // [修改] 注入 SettingsRepository
        initializer {
            val app = budgetQuestApplication()
            DashboardViewModel(
                budgetRepository = app.container.budgetRepository,
                settingsRepository = com.example.budgetquest.data.SettingsRepository(app.applicationContext) // [新增]
            )
        }

        // 3. TransactionViewModel
        initializer {
            val app = budgetQuestApplication()
            TransactionViewModel(
                repository = app.container.budgetRepository,
                settingsRepository = SettingsRepository(app.applicationContext), // [新增]
                currencyRepository = app.currencyRepository // [新增]
            )
        }

        // 4. SummaryViewModel
        // 需要: BudgetRepository, SettingsRepository
        initializer {
            val app = budgetQuestApplication()
            SummaryViewModel(
                repository = app.container.budgetRepository,
                settingsRepository = SettingsRepository(app.applicationContext)
            )
        }

        // 5. SubscriptionViewModel
        initializer {
            val app = budgetQuestApplication()
            SubscriptionViewModel(
                repository = app.container.budgetRepository,
                settingsRepository = com.example.budgetquest.data.SettingsRepository(app.applicationContext), // [新增]
                currencyRepository = app.currencyRepository // [新增]
            )
        }

        // 6. PlanHistoryViewModel
        initializer {
            PlanHistoryViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // 7. DailyDetailViewModel
        initializer {
            DailyDetailViewModel(budgetQuestApplication().container.budgetRepository)
        }

        // 8. SubscriptionDetailViewModel
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