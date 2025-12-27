package com.example.budgetquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.dashboard.DashboardScreen
import com.example.budgetquest.ui.history.PlanHistoryScreen
import com.example.budgetquest.ui.plan.PlanSetupScreen
import com.example.budgetquest.ui.plan.PlanViewModel
import com.example.budgetquest.ui.settings.SettingsScreen
import com.example.budgetquest.ui.subscription.SubscriptionScreen
import com.example.budgetquest.ui.subscription.SubscriptionViewModel
import com.example.budgetquest.ui.summary.SummaryScreen
import com.example.budgetquest.ui.summary.SummaryViewModel
import com.example.budgetquest.ui.theme.BudgetQuestTheme
import com.example.budgetquest.ui.transaction.DailyDetailScreen
import com.example.budgetquest.ui.transaction.TransactionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetQuestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BudgetQuestApp()
                }
            }
        }
    }
}

@Composable
fun BudgetQuestApp(
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as BudgetQuestApplication
    val repository = app.container.budgetRepository

    val uiState by viewModel.uiState.collectAsState()



    // [新增] 載入畫面控制
    if (uiState.isLoading) {
        // 這裡可以放一個漂亮的 Logo 或 Loading 動畫
        // 目前先顯示空白，避免閃爍即可
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    } else {
        // 資料載入完畢，計算起始路徑
        val startRoute = if (uiState.currentPlan != null) "dashboard" else "setup/-1"

        NavHost(
            navController = navController,
            startDestination = startRoute,
            // 全域轉場動畫：像一般 App 一樣左右滑動
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) + fadeOut()
            }
        ) {
            // Dashboard (主頁面)
            composable("dashboard") {
                val dashboardViewModel: com.example.budgetquest.ui.dashboard.DashboardViewModel =
                    viewModel(factory = AppViewModelProvider.Factory)

                val dashboardState by dashboardViewModel.uiState.collectAsState()

                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onAddExpenseClick = {
                        val today = System.currentTimeMillis()
                        navController.navigate("transaction/$today/-1") {
                            launchSingleTop = true
                        }
                    },
                    onDayClick = { dateMillis ->
                        navController.navigate("daily_detail/$dateMillis") {
                            launchSingleTop = true
                        }
                    },
                    onSummaryClick = {
                        val activeId = dashboardState.activePlan?.id ?: -1
                        navController.navigate("summary/$activeId") {
                            launchSingleTop = true
                        }
                    },
                    onSubscriptionClick = {
                        val activePlanStart = dashboardState.activePlan?.startDate ?: -1L
                        val activePlanEnd = dashboardState.activePlan?.endDate ?: -1L
                        navController.navigate("subscription?defaultDate=$activePlanStart&defaultEndDate=$activePlanEnd") {
                            launchSingleTop = true
                        }
                    },
                    onEditPlanClick = { planId ->
                        navController.navigate("setup/${planId ?: -1}") {
                            launchSingleTop = true
                        }
                    },
                    onEmptyDateClick = { dateMillis ->
                        navController.navigate("setup/-1?startDate=$dateMillis") {
                            launchSingleTop = true
                        }
                    },
                    // [新增] 歷史紀錄按鈕點擊
                    onHistoryClick = {
                        navController.navigate("plan_history") {
                            launchSingleTop = true
                        }
                    },
                    // [新增] 設定按鈕點擊
                    onSettingsClick = {
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Setup 路由
            composable(
                route = "setup/{planId}?startDate={startDate}",
                arguments = listOf(
                    navArgument("planId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("startDate") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val planId = backStackEntry.arguments?.getInt("planId")
                val startDate = backStackEntry.arguments?.getLong("startDate") ?: -1L
                val finalId = if (planId == -1) null else planId

                val planViewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)

                PlanSetupScreen(
                    planId = finalId,
                    viewModel = planViewModel,
                    onPlanSaved = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )

                LaunchedEffect(finalId, startDate) {
                    planViewModel.initializeForm(finalId, startDate)
                }
            }

            // [新增] Settings 路由
            composable("settings") {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // [新增] Plan History 路由
            composable("plan_history") {
                PlanHistoryScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 每日詳情
            composable(
                route = "daily_detail/{date}",
                arguments = listOf(navArgument("date") { type = NavType.LongType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                DailyDetailScreen(
                    date = date,
                    onBackClick = { navController.popBackStack() },
                    onAddClick = {
                        navController.navigate("transaction/$date/-1") {
                            launchSingleTop = true
                        }
                    },
                    onItemClick = { expenseId ->
                        navController.navigate("transaction/$date/$expenseId") {
                            launchSingleTop = true
                        }
                    },
                    budgetRepository = repository
                )
            }

            // 記帳/編輯消費
            composable(
                route = "transaction/{date}/{expenseId}",
                arguments = listOf(
                    navArgument("date") { type = NavType.LongType },
                    navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L

                TransactionScreen(
                    initialDate = date,
                    expenseId = expenseId,
                    navigateBack = { navController.popBackStack() }
                )
            }

            // Summary 路由
            composable(
                route = "summary/{planId}",
                arguments = listOf(navArgument("planId") { type = NavType.IntType; defaultValue = -1 })
            ) { backStackEntry ->
                val planId = backStackEntry.arguments?.getInt("planId") ?: -1

                val viewModel: SummaryViewModel = viewModel(factory = AppViewModelProvider.Factory)

                LaunchedEffect(planId) {
                    viewModel.initialize(planId)
                }

                SummaryScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }

            // 訂閱管理路由
            composable(
                route = "subscription?defaultDate={defaultDate}&defaultEndDate={defaultEndDate}",
                arguments = listOf(
                    navArgument("defaultDate") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("defaultEndDate") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val defaultDate = backStackEntry.arguments?.getLong("defaultDate") ?: -1L
                val defaultEndDate = backStackEntry.arguments?.getLong("defaultEndDate") ?: -1L

                val viewModel: SubscriptionViewModel = viewModel(factory = AppViewModelProvider.Factory)

                LaunchedEffect(defaultDate, defaultEndDate) {
                    viewModel.initialize(defaultDate, defaultEndDate)
                }

                SubscriptionScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }

    }


}