package com.example.budgetquest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.budgetquest.data.SettingsRepository
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraBackground
import com.example.budgetquest.ui.dashboard.DashboardScreen
import com.example.budgetquest.ui.history.PlanHistoryScreen
import com.example.budgetquest.ui.onboarding.OnboardingScreen
import com.example.budgetquest.ui.plan.PlanSetupScreen
import com.example.budgetquest.ui.plan.PlanViewModel
import com.example.budgetquest.ui.settings.SettingsScreen
import com.example.budgetquest.ui.subscription.SubscriptionDetailScreen
import com.example.budgetquest.ui.subscription.SubscriptionScreen
import com.example.budgetquest.ui.summary.SummaryScreen
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.theme.BudgetQuestTheme
import com.example.budgetquest.ui.transaction.DailyDetailScreen
import com.example.budgetquest.ui.transaction.TransactionDetailScreen
import com.example.budgetquest.ui.transaction.TransactionScreen

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            var isDarkMode by remember { mutableStateOf(settingsRepo.isDarkModeEnabled) }

            BudgetQuestTheme(darkTheme = isDarkMode) {
                AuroraBackground {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        val navController = rememberNavController()
                        val planViewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
                        val planUiState by planViewModel.uiState.collectAsState()

                        var isDataReady by remember { mutableStateOf(false) }

                        LaunchedEffect(planUiState.isLoading) {
                            if (!planUiState.isLoading) {
                                isDataReady = true
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {

                            if (isDataReady) {

                                val dashboardRoute = "dashboard?planId={planId}&date={date}&trigger={trigger}&tutorial={tutorial}"

                                val startDestination = remember {
                                    if (settingsRepo.isFirstLaunch) {
                                        "onboarding"
                                    } else {
                                        "dashboard"
                                    }
                                }

                                val animDuration = 300

                                SharedTransitionLayout {
                                    NavHost(
                                        navController = navController,
                                        startDestination = startDestination,
                                        // 預設全域動畫：左右滑動 (Dashboard, DailyDetail 等層級頁面使用)
                                        enterTransition = {
                                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) +
                                                    fadeIn(animationSpec = tween(animDuration))
                                        },
                                        exitTransition = {
                                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) +
                                                    fadeOut(animationSpec = tween(animDuration))
                                        },
                                        popEnterTransition = {
                                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) +
                                                    fadeIn(animationSpec = tween(animDuration))
                                        },
                                        popExitTransition = {
                                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) +
                                                    fadeOut(animationSpec = tween(animDuration))
                                        }
                                    ) {
                                        // 0. Onboarding
                                        composable("onboarding") {
                                            OnboardingScreen(
                                                onFinish = {
                                                    settingsRepo.isFirstLaunch = false
                                                    if (navController.previousBackStackEntry == null) {
                                                        navController.navigate("setup/-1?showBack=false") {
                                                            popUpTo("onboarding") { inclusive = true }
                                                        }
                                                    } else {
                                                        navController.navigate("dashboard?tutorial=true") {
                                                            popUpTo("onboarding") { inclusive = true }
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        // 1. Dashboard
                                        composable(
                                            route = dashboardRoute,
                                            arguments = listOf(
                                                navArgument("planId") { type = NavType.IntType; defaultValue = -1 },
                                                navArgument("date") { type = NavType.LongType; defaultValue = -1L },
                                                navArgument("trigger") { type = NavType.LongType; defaultValue = 0L },
                                                navArgument("tutorial") { type = NavType.BoolType; defaultValue = false }
                                            ),
                                            // [關鍵修改] 針對 Dashboard 的進入動畫做特殊處理
                                            // 如果是從 History 頁面回來 (planId != -1)，我們希望使用 SharedElement 的縮放效果
                                            // 所以這裡改用 fadeIn()，不要 slide，讓 SharedElement 唱主角
                                            enterTransition = {
                                                val fromHistory = initialState.destination.route?.startsWith("history") == true
                                                if (fromHistory) {
                                                    fadeIn(animationSpec = tween(animDuration))
                                                } else {
                                                    // 其他情況維持預設的滑動
                                                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) +
                                                            fadeIn(animationSpec = tween(animDuration))
                                                }
                                            }
                                        ) { backStackEntry ->
                                            val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                            val date = backStackEntry.arguments?.getLong("date") ?: -1L
                                            val trigger = backStackEntry.arguments?.getLong("trigger") ?: 0L
                                            val isTutorial = backStackEntry.arguments?.getBoolean("tutorial") ?: false

                                            DashboardScreen(
                                                initialPlanId = if (planId != -1) planId else null,
                                                planId = planId,
                                                targetDate = date,
                                                trigger = trigger,
                                                isTutorialMode = isTutorial,
                                                onConsumeNavigationArgs = {
                                                    backStackEntry.arguments?.putInt("planId", -1)
                                                    backStackEntry.arguments?.putLong("date", -1L)
                                                    backStackEntry.arguments?.putLong("trigger", 0L)
                                                },
                                                onTutorialFinished = {
                                                    navController.navigate("dashboard") {
                                                        popUpTo(0)
                                                    }
                                                },
                                                onAddExpenseClick = { targetDate ->
                                                    navController.navigate("transaction/-1?date=$targetDate") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onDayClick = { date ->
                                                    navController.navigate("daily_detail/$date") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onSummaryClick = { id ->
                                                    val route = if (id != null) "summary?planId=$id" else "summary"
                                                    navController.navigate(route) {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onSubscriptionClick = { pId, start, end ->
                                                    navController.navigate("subscription?planId=$pId&start=$start&end=$end") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onEditPlanClick = { id ->
                                                    val validId = id ?: -1
                                                    navController.navigate("setup/$validId") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onEmptyDateClick = { start, end ->
                                                    navController.navigate("setup/-1?startDate=$start&endDate=$end") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onHistoryClick = {
                                                    navController.navigate("history") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onSettingsClick = {
                                                    navController.navigate("settings") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                // [傳遞 Scope]
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }

                                        // 2. Transaction (填寫消費頁面) - Slide Up
                                        composable(
                                            route = "transaction/{expenseId}?date={date}",
                                            arguments = listOf(
                                                navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                                                navArgument("date") { type = NavType.LongType; defaultValue = -1L }
                                            ),
                                            enterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            exitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            },
                                            popEnterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            popExitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            }
                                        ) { backStackEntry ->
                                            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
                                            val date = backStackEntry.arguments?.getLong("date") ?: -1L

                                            TransactionScreen(
                                                onBackClick = { navController.popBackStack() },
                                                onSaveSuccess = { savedDate ->
                                                    navController.popBackStack()
                                                },
                                                expenseId = expenseId,
                                                initialDate = date
                                            )
                                        }

                                        // 3. Daily Detail
                                        composable(
                                            route = "daily_detail/{date}",
                                            arguments = listOf(navArgument("date") { type = NavType.LongType }),
                                            // 當從 Transaction 返回時只淡入，不滑動
                                            popEnterTransition = {
                                                if (initialState.destination.route?.startsWith("transaction") == true) {
                                                    fadeIn(animationSpec = tween(animDuration))
                                                } else {
                                                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(animDuration)) +
                                                            fadeIn(animationSpec = tween(animDuration))
                                                }
                                            },
                                            // 當進入 Transaction 時只淡出，不滑動
                                            exitTransition = {
                                                if (targetState.destination.route?.startsWith("transaction") == true) {
                                                    fadeOut(animationSpec = tween(animDuration))
                                                } else {
                                                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(animDuration)) +
                                                            fadeOut(animationSpec = tween(animDuration))
                                                }
                                            }
                                        ) { backStackEntry ->
                                            val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                                            DailyDetailScreen(
                                                date = date,
                                                onBackClick = { navController.popBackStack() },
                                                onAddExpenseClick = { selectedDate ->
                                                    navController.navigate("transaction/-1?date=$selectedDate") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onItemClick = { expenseId ->
                                                    navController.navigate("transaction_detail/$expenseId") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }

                                        // 4. Plan Setup - Slide Up
                                        composable(
                                            route = "setup/{planId}?startDate={startDate}&endDate={endDate}&showBack={showBack}",
                                            arguments = listOf(
                                                navArgument("planId") { type = NavType.IntType; defaultValue = -1 },
                                                navArgument("startDate") { type = NavType.LongType; defaultValue = -1L },
                                                navArgument("endDate") { type = NavType.LongType; defaultValue = -1L },
                                                navArgument("showBack") { type = NavType.BoolType; defaultValue = true }
                                            ),
                                            enterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            exitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            },
                                            popEnterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            popExitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            }
                                        ) { backStackEntry ->
                                            val planIdArg = backStackEntry.arguments?.getInt("planId") ?: -1
                                            val startDate = backStackEntry.arguments?.getLong("startDate") ?: -1L
                                            val endDate = backStackEntry.arguments?.getLong("endDate") ?: -1L
                                            val showBack = backStackEntry.arguments?.getBoolean("showBack") ?: true

                                            val isCreatingNewPlan = planIdArg == -1
                                            val validPlanId = if (planIdArg == -1) null else planIdArg

                                            PlanSetupScreen(
                                                planId = validPlanId,
                                                initialStartDate = startDate,
                                                initialEndDate = endDate,
                                                showBackButton = showBack,
                                                onBackClick = { navController.popBackStack() },
                                                onSaveClick = { savedId ->
                                                    if (!showBack) {
                                                        navController.navigate("dashboard?planId=$savedId&tutorial=true") {
                                                            popUpTo(0)
                                                        }
                                                    } else if (isCreatingNewPlan) {
                                                        val timestamp = System.currentTimeMillis()
                                                        navController.navigate("dashboard?planId=$savedId&trigger=$timestamp") {
                                                            popUpTo("dashboard") { inclusive = true }
                                                        }
                                                    } else {
                                                        navController.popBackStack()
                                                    }
                                                },
                                                onDeleteClick = { deletedPlanDate ->
                                                    val timestamp = System.currentTimeMillis()
                                                    navController.navigate("dashboard?planId=-1&date=$deletedPlanDate&trigger=$timestamp") {
                                                        popUpTo(0)
                                                    }
                                                }
                                            )
                                        }

                                        // 5. Summary - Slide Up
                                        composable(
                                            route = "summary?planId={planId}",
                                            arguments = listOf(navArgument("planId") { type = NavType.IntType; defaultValue = -1 }),
                                            enterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            exitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            },
                                            popEnterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            popExitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            }
                                        ) { backStackEntry ->
                                            val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                            SummaryScreen(
                                                planId = planId,
                                                onBackClick = { navController.popBackStack() },
                                                onItemClick = { expenseId ->
                                                    navController.navigate("transaction_detail/$expenseId") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }

                                        // 6. Settings - Slide Up
                                        composable(
                                            "settings",
                                            enterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            exitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            },
                                            popEnterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            popExitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            }
                                        ) {
                                            SettingsScreen(
                                                onBackClick = { navController.popBackStack() },
                                                onDarkModeToggle = { isDark -> isDarkMode = isDark },
                                                onReplayOnboarding = {
                                                    navController.navigate("onboarding")
                                                }
                                            )
                                        }

                                        // 7. Subscription - Slide Up
                                        composable(
                                            route = "subscription?planId={planId}&start={start}&end={end}&editId={editId}",
                                            arguments = listOf(
                                                navArgument("planId") { type = NavType.IntType; defaultValue = -1 },
                                                navArgument("start") { type = NavType.LongType; defaultValue = -1L },
                                                navArgument("end") { type = NavType.LongType; defaultValue = -1L },
                                                navArgument("editId") { type = NavType.LongType; defaultValue = -1L }
                                            ),
                                            enterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            exitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            },
                                            popEnterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(animDuration)) +
                                                        fadeIn(animationSpec = tween(animDuration))
                                            },
                                            popExitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(animDuration)) +
                                                        fadeOut(animationSpec = tween(animDuration))
                                            }
                                        ) { backStackEntry ->
                                            val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                            val start = backStackEntry.arguments?.getLong("start") ?: -1L
                                            val end = backStackEntry.arguments?.getLong("end") ?: -1L
                                            val editId = backStackEntry.arguments?.getLong("editId") ?: -1L

                                            SubscriptionScreen(
                                                planId = planId,
                                                startDate = start,
                                                endDate = end,
                                                editId = editId,
                                                onBackClick = { navController.popBackStack() },
                                                onSaveSuccess = {
                                                    navController.popBackStack()
                                                },
                                                onItemClick = { subId ->
                                                    navController.navigate("subscription_detail/$subId") {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }

                                        // 8. History (計畫歷史紀錄)
                                        composable(
                                            "history",
                                            // [修正 1] 進入動畫：改為由下往上滑入 (與設定頁面相同)
                                            enterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300))
                                            },
                                            // [修正 2] 離開動畫：智慧判斷
                                            exitTransition = {
                                                // 如果是前往 Dashboard (點擊卡片)，則背景淡出，讓 SharedElement 負責縮放動畫
                                                if (targetState.destination.route?.startsWith("dashboard") == true) {
                                                    fadeOut(animationSpec = tween(300))
                                                } else {
                                                    // 如果是其他情況 (例如被覆蓋)，則向下滑出
                                                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300))
                                                }
                                            },
                                            // [修正 3] 返回進入：維持由下往上 (雖然通常 History 是 Top level，不太會發生)
                                            popEnterTransition = {
                                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300))
                                            },
                                            // [修正 4] 返回離開 (按下 Back 鍵)：由上往下滑出 (與設定頁面相同)
                                            popExitTransition = {
                                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300))
                                            }
                                        ) {
                                            PlanHistoryScreen(
                                                onBackClick = { navController.popBackStack() },
                                                onPlanClick = { planId, startDate ->
                                                    val timestamp = System.currentTimeMillis()
                                                    navController.navigate("dashboard?planId=$planId&date=$startDate&trigger=$timestamp") {
                                                        // 這裡移除 popUpTo，讓 dashboard 疊在 history 上面，
                                                        // 這樣返回時才能看到 history 頁面滑下來
                                                        launchSingleTop = true
                                                    }
                                                },
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }

                                        // 9. Transaction Detail
                                        composable(
                                            route = "transaction_detail/{expenseId}",
                                            arguments = listOf(navArgument("expenseId") { type = NavType.LongType }),
                                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                                            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                                            popExitTransition = { fadeOut(animationSpec = tween(300)) }
                                        ) { backStackEntry ->
                                            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
                                            val viewModel: com.example.budgetquest.ui.transaction.TransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)

                                            TransactionDetailScreen(
                                                expenseId = expenseId,
                                                onBackClick = { navController.popBackStack() },
                                                onEditClick = { id ->
                                                    navController.navigate("transaction/$id")
                                                },
                                                onDeleteClick = {
                                                    viewModel.deleteExpense(expenseId)
                                                    navController.popBackStack()
                                                },
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }

                                        // 10. Subscription Detail
                                        composable(
                                            route = "subscription_detail/{subId}",
                                            arguments = listOf(navArgument("subId") { type = NavType.LongType }),
                                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                                            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                                            popExitTransition = { fadeOut(animationSpec = tween(300)) }
                                        ) { backStackEntry ->
                                            val subId = backStackEntry.arguments?.getLong("subId") ?: -1L

                                            SubscriptionDetailScreen(
                                                subscriptionId = subId,
                                                onBackClick = { navController.popBackStack() },
                                                onEditClick = { id ->
                                                    navController.navigate("subscription?planId=-1&editId=$id")
                                                },
                                                onDeleteSuccess = {
                                                    navController.popBackStack()
                                                },
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@composable
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = !isDataReady || planUiState.isLoading,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.align(Alignment.Center).background(Color.Transparent)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = AppTheme.colors.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}