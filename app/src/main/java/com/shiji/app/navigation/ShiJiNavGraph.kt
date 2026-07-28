package com.shiji.app.navigation

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shiji.app.ui.MainViewModel
import com.shiji.app.ui.about.PrivacyPolicyScreen
import com.shiji.app.ui.about.AboutScreen
import com.shiji.app.ui.aisettings.AiSettingsScreen
import com.shiji.app.ui.camera.CameraScreen
import com.shiji.app.ui.chat.AiChatScreen
import com.shiji.app.ui.data.DataScreen
import com.shiji.app.ui.diet.DietLogScreen
import com.shiji.app.ui.export.DataExportScreen
import com.shiji.app.ui.foodlibrary.FoodLibraryScreen
import com.shiji.app.ui.goal.GoalSettingScreen
import com.shiji.app.ui.home.HomeScreen
import com.shiji.app.ui.manual.ManualEntryScreen
import com.shiji.app.ui.onboarding.OnboardingScreen
import com.shiji.app.ui.profile.ProfileScreen
import com.shiji.app.ui.theme.ShiJiTheme
import com.shiji.app.ui.textrecord.TextRecordScreen
import com.shiji.app.ui.water.WaterScreen
import com.shiji.app.ui.weight.WeightScreen
import com.shiji.core.data.entity.UserGoalEntity
import androidx.compose.ui.unit.dp

data class BottomNavTab(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
val bottomNavTabs = listOf(
    BottomNavTab("home", "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab("data", "数据", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavTab("profile", "我的", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun ShiJiNavGraph() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()

    val currentDate by mainViewModel.currentDate.collectAsStateWithLifecycle()
    val currentDateRecords by mainViewModel.currentDateRecords.collectAsStateWithLifecycle()
    val weekRecords by mainViewModel.weekRecords.collectAsStateWithLifecycle()
    val cachedFoods by mainViewModel.cachedFoods.collectAsStateWithLifecycle()
    val userGoal by mainViewModel.userGoal.collectAsStateWithLifecycle()
    val onboardingDone by mainViewModel.onboardingDone.collectAsStateWithLifecycle()
    val userName by mainViewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by mainViewModel.userAvatar.collectAsStateWithLifecycle()
    val waterMl by mainViewModel.waterMl.collectAsStateWithLifecycle()
    val waterGoal by mainViewModel.waterGoal.collectAsStateWithLifecycle()
    val aiUsage by mainViewModel.aiUsageSummary.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val darkPref by mainViewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isDarkTheme = darkPref ?: systemDark

    val startDestination = when (onboardingDone) {
        null -> null
        false -> "onboarding"
        else -> "home"
    }

    ShiJiTheme(darkTheme = isDarkTheme) {
        if (startDestination == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ShiJiTheme
        }

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = bottomNavTabs.any { it.route == currentDestination?.route }
                if (showBottomBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        bottomNavTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true; restoreState = true
                                    }
                                },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                // === Main Tabs ===
                composable("home") {
                    HomeScreen(
                        todayRecords = currentDateRecords,
                        calorieTarget = userGoal?.dailyCalories?.toFloat() ?: 2000f,
                        proteinTarget = userGoal?.proteinTargetGrams?.toFloat() ?: 60f,
                        carbsTarget = userGoal?.carbsTargetGrams?.toFloat() ?: 250f,
                        fatTarget = userGoal?.fatTargetGrams?.toFloat() ?: 65f,
                        waterMl = waterMl,
                        waterGoalMl = waterGoal,
                        onAddWater = { mainViewModel.addWater(it) },
                        onSetWaterGoal = { mainViewModel.setWaterGoal(it) },
                        onNavigateToCamera = { navController.navigate("camera") },
                        onNavigateToTextRecord = { navController.navigate("textrecord") },
                        onNavigateToManual = { navController.navigate("manual") },
                        onNavigateToSettings = { navController.navigate("profile") },
                        onNavigateToDietLog = { navController.navigate("diet_log") }
                    )
                }
                composable("data") {
                    DataScreen(
                        weekRecords = weekRecords,
                        todayRecords = currentDateRecords,
                        calorieTarget = userGoal?.dailyCalories ?: 2000.0,
                        proteinTarget = userGoal?.proteinTargetGrams ?: 60.0,
                        onNavigateToDietLog = { navController.navigate("diet_log") },
                        onNavigateToWeight = { navController.navigate("weight") },
                        onNavigateToAIChat = { prompt ->
                            navController.navigate("ai_chat/${Uri.encode(prompt)}")
                        }
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        userName = userName, userAvatar = userAvatar,
                        isDarkTheme = isDarkTheme,
                        aiUsage = aiUsage,
                        onEditProfile = { name, avatar -> mainViewModel.updateProfile(name, avatar) },
                        onToggleDarkTheme = { mainViewModel.setDarkTheme(it) },
                        onNavigateToGoal = { navController.navigate("goal") },
                        onNavigateToAiSettings = { navController.navigate("ai_settings") },
                        onNavigateToAIChat = { navController.navigate("ai_chat") },
                        onNavigateToFoodLibrary = { navController.navigate("food_library") },
                        onNavigateToDataExport = { navController.navigate("data_export") },
                        onNavigateToWeight = { navController.navigate("weight") },
                        onNavigateToWater = { navController.navigate("water") },
                        onNavigateToPrivacy = { navController.navigate("privacy") },
                        onNavigateToAbout = { navController.navigate("about") }
                    )
                }

                // === AI Chat ===
                composable("ai_chat") {
                    AiChatScreen(
                        initialPrompt = "",
                        userName = userName,
                        onBack = { navController.popBackStack() },
                        onNavigateToAiSettings = { navController.navigate("ai_settings") }
                    )
                }
                composable(
                    route = "ai_chat/{prompt}",
                    arguments = listOf(navArgument("prompt") { type = NavType.StringType; defaultValue = "" })
                ) { backStackEntry ->
                    AiChatScreen(
                        initialPrompt = Uri.decode(backStackEntry.arguments?.getString("prompt") ?: ""),
                        userName = userName,
                        onBack = { navController.popBackStack() },
                        onNavigateToAiSettings = { navController.navigate("ai_settings") }
                    )
                }

                // === AI Settings ===
                composable("ai_settings") { AiSettingsScreen(onBack = { navController.popBackStack() }) }

                // === Food entry ===
                composable("camera") {
                    CameraScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        onNavigateToAiSettings = { navController.navigate("ai_settings") },
                        onNavigateToTextRecord = {
                            navController.navigate("textrecord") { popUpTo("camera") { inclusive = true } }
                        }
                    )
                }
                composable("textrecord") {
                    TextRecordScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        onNavigateToAiSettings = { navController.navigate("ai_settings") }
                    )
                }
                composable("manual") {
                    ManualEntryScreen(
                        cachedFoods = cachedFoods,
                        onSearchFoods = { q -> cachedFoods.filter { it.name.contains(q, ignoreCase = true) } },
                        onSaved = { record ->
                            mainViewModel.saveRecords(listOf(record))
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // === Logs ===
                composable("diet_log") {
                    DietLogScreen(
                        records = currentDateRecords,
                        date = currentDate,
                        onDateChange = { mainViewModel.setDate(it) },
                        onDeleteRecord = { id -> mainViewModel.deleteRecord(id) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("food_library") {
                    FoodLibraryScreen(
                        foods = cachedFoods,
                        onAddFood = { food -> mainViewModel.addCachedFood(food) },
                        onDeleteFood = { id -> mainViewModel.deleteCachedFood(id) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // === Health ===
                composable("weight") {
                    WeightScreen(
                        onBack = { navController.popBackStack() },
                        onSaveWeight = { mainViewModel.saveWeight(it) },
                        weightHistory = mainViewModel.weightHistoryFlow(
                            java.time.LocalDate.now().minusDays(90).toString(),
                            java.time.LocalDate.now().toString()
                        )
                    )
                }
                composable("water") {
                    WaterScreen(
                        onBack = { navController.popBackStack() },
                        waterMl = waterMl,
                        waterGoalMl = waterGoal,
                        onAddWater = { mainViewModel.addWater(it) },
                        onSetWaterGoal = { mainViewModel.setWaterGoal(it) }
                    )
                }
                composable("goal") {
                    GoalSettingScreen(
                        onBack = { navController.popBackStack() },
                        onSave = { goal -> mainViewModel.saveGoal(goal); navController.popBackStack() }
                    )
                }

                // === Onboarding ===
                composable("onboarding") {
                    var step by remember { mutableIntStateOf(1) }
                    OnboardingScreen(
                        step = step,
                        onStepChange = { step = it },
                        onComplete = { heightCm, weightKg, goalType ->
                            mainViewModel.saveGoal(UserGoalEntity(
                                heightCm = heightCm, currentWeightKg = weightKg, goalType = goalType
                            ))
                            mainViewModel.completeOnboarding()
                            navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                        },
                        onSkip = {
                            mainViewModel.completeOnboarding()
                            navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                        },
                        onNavigateToAiSettings = { navController.navigate("ai_settings") }
                    )
                }

                // === System ===
                composable("data_export") { DataExportScreen(onBack = { navController.popBackStack() }) }
                composable("privacy") { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
                composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}
