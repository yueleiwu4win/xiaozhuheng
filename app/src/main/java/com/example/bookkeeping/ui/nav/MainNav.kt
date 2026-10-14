package com.example.bookkeeping.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.bookkeeping.AppViewModelProvider
import com.example.bookkeeping.ui.category.CategoryScreen
import com.example.bookkeeping.ui.common.AppBackground
import com.example.bookkeeping.ui.edit.EditScreen
import com.example.bookkeeping.ui.home.HomeScreen
import com.example.bookkeeping.ui.home.HomeViewModel
import com.example.bookkeeping.ui.settings.SettingsScreen
import com.example.bookkeeping.ui.stats.StatsScreen
import com.example.bookkeeping.ui.stats.CategoryBillsScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_STATS = "stats"
private const val ROUTE_ME = "me"
private const val ROUTE_CATEGORY = "category?type={type}"
private const val ROUTE_EDIT = "edit?billId={billId}"

private fun navigateTab(route: String, navController: NavHostController) {
    navController.navigate(route) {
        popUpTo(ROUTE_HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val message by homeViewModel.message.collectAsStateWithLifecycle()
    val deleting by homeViewModel.deleting.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val mainPage = currentRoute in listOf(ROUTE_HOME, ROUTE_STATS, ROUTE_ME)

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            homeViewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground(Modifier.fillMaxSize())
        // 根导航始终全屏，主页面的导航栏不会改变退出中的编辑页尺寸。
        NavHost(
            navController, startDestination = ROUTE_HOME, modifier = Modifier.fillMaxSize(),
            enterTransition = { if (targetState.destination.route in listOf(ROUTE_HOME, ROUTE_STATS, ROUTE_ME)) EnterTransition.None else fadeIn(tween(180)) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { fadeOut(tween(180)) }
        ) {
                composable(ROUTE_HOME) {
                    MainPageScaffold(ROUTE_HOME, navController, snackbar) {
                        HomeScreen(
                            onEditBill = { navController.navigate("edit?billId=${it ?: -1L}") },
                            viewModel = homeViewModel
                        )
                    }
                }
                composable(ROUTE_STATS) {
                    MainPageScaffold(ROUTE_STATS, navController, snackbar) {
                        StatsScreen(onOpenCategory = { id, type, year, month ->
                            navController.navigate("category-bills?categoryId=$id&type=$type&year=$year&month=${month ?: -1}")
                        })
                    }
                }
                composable(ROUTE_ME) {
                    MainPageScaffold(ROUTE_ME, navController, snackbar) {
                        SettingsScreen(onOpenCategories = { navController.navigate("category?type=expense") })
                    }
                }
                composable(ROUTE_CATEGORY, arguments = listOf(navArgument("type") { defaultValue = "expense" })) { entry ->
                    CategoryScreen(onBack = { navController.popBackStack() }, initialType = entry.arguments?.getString("type") ?: "expense")
                }
                composable("category-bills?categoryId={categoryId}&type={type}&year={year}&month={month}", arguments = listOf(
                    navArgument("categoryId") { type = NavType.LongType }, navArgument("type") { defaultValue = "expense" },
                    navArgument("year") { type = NavType.IntType }, navArgument("month") { type = NavType.IntType; defaultValue = -1 }
                )) {
                    CategoryBillsScreen(onBack = { navController.popBackStack() }, onEdit = { navController.navigate("edit?billId=$it") })
                }
                composable(
                    route = ROUTE_EDIT,
                    arguments = listOf(navArgument("billId") { type = NavType.LongType; defaultValue = -1L }),
                    // 与主标签一样直接切换，避免淡出时两层页面重叠。
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    EditScreen(
                        onBack = { navController.popBackStack() },
                        onManageCategories = { type -> navController.navigate("category?type=$type") },
                        onSaved = { navController.popBackStack() },
                        onDeleteBill = { id ->
                            homeViewModel.deleteBill(id) {
                                val entry = navController.currentBackStackEntry
                                if (entry?.destination?.route == ROUTE_EDIT && entry.arguments?.getLong("billId") == id) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        deleting = deleting
                    )
                }
        }
        if (!mainPage) SnackbarHost(
            snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun MainPageScaffold(
    route: String,
    navController: NavHostController,
    snackbar: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (route == ROUTE_HOME) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("edit?billId=-1") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("记一笔") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = route == ROUTE_HOME,
                    onClick = { navigateTab(ROUTE_HOME, navController) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("明细") }
                )
                NavigationBarItem(
                    selected = route == ROUTE_STATS,
                    onClick = { navigateTab(ROUTE_STATS, navController) },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text("统计") }
                )
                NavigationBarItem(
                    selected = route == ROUTE_ME,
                    onClick = { navigateTab(ROUTE_ME, navController) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() }
    }
}
