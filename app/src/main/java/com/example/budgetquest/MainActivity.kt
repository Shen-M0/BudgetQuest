package com.example.budgetquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.data.SettingsRepository
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.dashboard.DashboardScreen
import com.example.budgetquest.ui.history.PlanHistoryScreen
import com.example.budgetquest.ui.onboarding.OnboardingScreen
import com.example.budgetquest.ui.plan.PlanSetupScreen
import com.example.budgetquest.ui.plan.PlanViewModel
import com.example.budgetquest.ui.settings.SettingsScreen
import com.example.budgetquest.ui.subscription.SubscriptionScreen
import com.example.budgetquest.ui.summary.SummaryScreen
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.theme.BudgetQuestTheme
import com.example.budgetquest.ui.transaction.DailyDetailScreen
import com.example.budgetquest.ui.transaction.TransactionScreen
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            var isDarkMode by remember { mutableStateOf(settingsRepo.isDarkModeEnabled) }

            BudgetQuestTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colors.background
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

                            val dashboardRoute = "dashboard?planId={planId}&tutorial={tutorial}"

                            val startDestination = remember {
                                if (settingsRepo.isFirstLaunch) {
                                    "onboarding"
                                } else if (planUiState.currentPlan == null) {
                                    "setup/-1?showBack=false"
                                } else {
                                    "dashboard"
                                }
                            }

                            // [修正] 定義時間常數，讓 tween 自動推斷類型
                            val animDuration = 300

                            NavHost(
                                navController = navController,
                                startDestination = startDestination,
                                // [修正] 分別使用 tween(animDuration)，編譯器會自動推斷 IntOffset 或 Float
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
                                        navArgument("tutorial") { type = NavType.BoolType; defaultValue = false }
                                    )
                                ) { backStackEntry ->
                                    val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                    val isTutorial = backStackEntry.arguments?.getBoolean("tutorial") ?: false

                                    DashboardScreen(
                                        initialPlanId = if (planId != -1) planId else null,
                                        isTutorialMode = isTutorial,
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
                                        }
                                    )
                                }

                                // 2. Transaction
                                composable(
                                    route = "transaction/{expenseId}?date={date}",
                                    arguments = listOf(
                                        navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                                        navArgument("date") { type = NavType.LongType; defaultValue = -1L }
                                    )
                                ) { backStackEntry ->
                                    val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
                                    val date = backStackEntry.arguments?.getLong("date") ?: -1L

                                    TransactionScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onSaveSuccess = { savedDate ->
                                            navController.popBackStack()
                                            navController.navigate("daily_detail/$savedDate") {
                                                launchSingleTop = true
                                            }
                                        },
                                        expenseId = expenseId,
                                        initialDate = date
                                    )
                                }

                                // 3. Daily Detail
                                composable(
                                    route = "daily_detail/{date}",
                                    arguments = listOf(navArgument("date") { type = NavType.LongType })
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
                                            navController.navigate("transaction/$expenseId") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                // 4. Plan Setup
                                composable(
                                    route = "setup/{planId}?startDate={startDate}&endDate={endDate}&showBack={showBack}",
                                    arguments = listOf(
                                        navArgument("planId") { type = NavType.IntType; defaultValue = -1 },
                                        navArgument("startDate") { type = NavType.LongType; defaultValue = -1L },
                                        navArgument("endDate") { type = NavType.LongType; defaultValue = -1L },
                                        navArgument("showBack") { type = NavType.BoolType; defaultValue = true }
                                    )
                                ) { backStackEntry ->
                                    val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                    val startDate = backStackEntry.arguments?.getLong("startDate") ?: -1L
                                    val endDate = backStackEntry.arguments?.getLong("endDate") ?: -1L
                                    val showBack = backStackEntry.arguments?.getBoolean("showBack") ?: true
                                    val validPlanId = if (planId == -1) null else planId

                                    PlanSetupScreen(
                                        planId = validPlanId,
                                        initialStartDate = startDate,
                                        initialEndDate = endDate,
                                        showBackButton = showBack,
                                        onBackClick = { navController.popBackStack() },
                                        onSaveClick = { savedId ->
                                            val tutorialFlag = !showBack
                                            navController.navigate("dashboard?planId=$savedId&tutorial=$tutorialFlag") {
                                                popUpTo(0)
                                            }
                                        },
                                        onDeleteClick = {
                                            navController.navigate("dashboard") {
                                                popUpTo(0)
                                            }
                                        }
                                    )
                                }

                                // 5. Summary
                                composable(
                                    route = "summary?planId={planId}",
                                    arguments = listOf(navArgument("planId") { type = NavType.IntType; defaultValue = -1 })
                                ) { backStackEntry ->
                                    val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                    SummaryScreen(
                                        planId = planId,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }

                                // 6. Settings
                                composable("settings") {
                                    SettingsScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onDarkModeToggle = { isDark -> isDarkMode = isDark },
                                        onReplayOnboarding = {
                                            navController.navigate("onboarding")
                                        }
                                    )
                                }

                                // 7. Subscription
                                composable(
                                    route = "subscription?planId={planId}&start={start}&end={end}",
                                    arguments = listOf(
                                        navArgument("planId") { type = NavType.IntType; defaultValue = -1 },
                                        navArgument("start") { type = NavType.LongType; defaultValue = -1L },
                                        navArgument("end") { type = NavType.LongType; defaultValue = -1L }
                                    )
                                ) { backStackEntry ->
                                    val planId = backStackEntry.arguments?.getInt("planId") ?: -1
                                    val start = backStackEntry.arguments?.getLong("start") ?: -1L
                                    val end = backStackEntry.arguments?.getLong("end") ?: -1L

                                    SubscriptionScreen(
                                        planId = planId,
                                        startDate = start,
                                        endDate = end,
                                        onBackClick = { navController.popBackStack() },
                                        onSaveSuccess = {
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                // 8. History
                                composable("history") {
                                    PlanHistoryScreen(onBackClick = { navController.popBackStack() })
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !isDataReady || planUiState.isLoading,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.Center).background(AppTheme.colors.background)
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