package com.example.bookkeeping.ui.nav

import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.ui.theme.LocalThemeTokens
import com.example.bookkeeping.ui.theme.HomeActionPlacement
import com.example.bookkeeping.ui.theme.SelectionStyle
import com.example.bookkeeping.ui.theme.HomeLayout
import com.example.bookkeeping.ui.theme.readingSurface
import com.example.bookkeeping.ui.home.LedgerNavigationBar
import com.example.bookkeeping.ui.home.LedgerCreateButton
import com.example.bookkeeping.ui.home.SongCreateAction
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.bookkeeping.ui.common.themedIcon
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
import com.example.bookkeeping.ui.common.AppButton
import com.example.bookkeeping.ui.edit.EditScreen
import com.example.bookkeeping.ui.home.HomeScreen
import com.example.bookkeeping.ui.home.HomeViewModel
import com.example.bookkeeping.ui.settings.SettingsScreen
import com.example.bookkeeping.ui.settings.ReadingSettingsScreen
import com.example.bookkeeping.ui.stats.StatsScreen
import com.example.bookkeeping.ui.stats.CategoryBillsScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_STATS = "stats"
private const val ROUTE_ME = "me"
private const val ROUTE_CATEGORY = "category?type={type}"
private const val ROUTE_EDIT = "edit?billId={billId}&initialType={initialType}&quick={quick}"

private fun navigateTab(route: String, navController: NavHostController) {
    navController.navigate(route) {
        popUpTo(ROUTE_HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainNav(quickType: String? = null, onQuickConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val message by homeViewModel.message.collectAsStateWithLifecycle()
    val deleting by homeViewModel.deleting.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var quickConfirm by remember { mutableStateOf(false) }
    fun openQuick() {
        if (currentRoute == ROUTE_EDIT) navController.popBackStack()
        navController.navigate("edit?billId=-1&initialType=$quickType&quick=true") { launchSingleTop = true }
        onQuickConsumed()
    }
    LaunchedEffect(quickType, currentRoute) {
        if (quickType != null && currentRoute != null) {
            if (currentRoute == ROUTE_EDIT) {
                val editing = androidx.lifecycle.ViewModelProvider(navController.currentBackStackEntry!!, AppViewModelProvider.Factory)[com.example.bookkeeping.ui.edit.EditViewModel::class.java]
                if (editing.saving.value || deleting) onQuickConsumed()
                else if (editing.hasUnsavedChanges()) quickConfirm = true else openQuick()
            } else openQuick()
        }
    }
    if (quickConfirm && quickType != null) AlertDialog(
        onDismissRequest = { quickConfirm = false; onQuickConsumed() },
        title = { Text("打开快捷记账？") }, text = { Text("当前记账页尚未关闭，继续会放弃该页未保存的内容。") },
        confirmButton = { TextButton({ quickConfirm = false; openQuick() }) { Text("继续") } },
        dismissButton = { TextButton({ quickConfirm = false; onQuickConsumed() }) { Text("留在当前页") } }
    )
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
                            onOpenBudget = { navController.navigate("budget?month=$it") }, viewModel = homeViewModel
                        )
                    }
                }
                composable(ROUTE_STATS) {
                    MainPageScaffold(ROUTE_STATS, navController, snackbar) {
                        StatsScreen(onOpenSummary = { navController.navigate("monthly-summary?month=$it") }, onOpenCategory = { id, type, year, month ->
                            navController.navigate("category-bills?categoryId=$id&type=$type&year=$year&month=${month ?: -1}")
                        })
                    }
                }
                composable(ROUTE_ME) {
                    MainPageScaffold(ROUTE_ME, navController, snackbar) {
                        SettingsScreen(onOpenCategories = { navController.navigate("category?type=expense") },
                            onOpenReading = { navController.navigate("reading") { launchSingleTop = true } },
                            onOpenBudget = { navController.navigate("budget?month=${java.time.YearMonth.now()}") })
                    }
                }
                composable("budget?month={month}", arguments = listOf(navArgument("month") { defaultValue = java.time.YearMonth.now().toString() })) { entry ->
                    com.example.bookkeeping.ui.budget.BudgetScreen(java.time.YearMonth.parse(entry.arguments?.getString("month"))) { navController.popBackStack() }
                }
                composable("monthly-summary?month={month}", arguments = listOf(navArgument("month") { defaultValue = java.time.YearMonth.now().toString() })) { entry ->
                    com.example.bookkeeping.ui.summary.MonthlySummaryScreen(java.time.YearMonth.parse(entry.arguments?.getString("month"))) { navController.popBackStack() }
                }
                composable("reading") {
                    ReadingSettingsScreen(onBack = { navController.popBackStack() })
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
                    arguments = listOf(navArgument("billId") { type = NavType.LongType; defaultValue = -1L }, navArgument("initialType") { defaultValue = "expense" }, navArgument("quick") { type = NavType.BoolType; defaultValue = false }),
                    // 与主标签一样直接切换，避免淡出时两层页面重叠。
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) { editEntry ->
                    EditScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { if (editEntry.arguments?.getBoolean("quick") == true) navController.popBackStack(ROUTE_HOME, false) else navController.popBackStack() },
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
    val tokens = LocalThemeTokens.current
    val dockedAction = route == ROUTE_HOME && tokens.homeActionPlacement == HomeActionPlacement.DOCKED
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (route == ROUTE_HOME && !dockedAction) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("edit?billId=-1") { launchSingleTop = true } },
                    icon = { Icon(themedIcon(Icons.Filled.Add, Icons.Outlined.Edit), contentDescription = null) },
                    text = { Text("记一笔") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = if (LocalThemeTokens.current.ledgerRows) RoundedCornerShape(LocalThemeTokens.current.actionCorner) else FloatingActionButtonDefaults.extendedFabShape
                )
            }
        },
        bottomBar = {
            Column {
                if (dockedAction && tokens.homeLayout == HomeLayout.SONG) {
                    SongCreateAction(onClick = { navController.navigate("edit?billId=-1") { launchSingleTop = true } })
                } else if (dockedAction && tokens.homeLayout != HomeLayout.STANDARD) Surface(color = Color.Transparent) {
                    LedgerCreateButton(onClick = { navController.navigate("edit?billId=-1") { launchSingleTop = true } },
                        wide = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                } else if (dockedAction) Surface(color = MaterialTheme.colorScheme.background) {
                    AppButton(
                        onClick = { navController.navigate("edit?billId=-1") { launchSingleTop = true } },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, null)
                        Spacer(Modifier.width(10.dp))
                        Text("记一笔", style = MaterialTheme.typography.titleMedium)
                    }
                }
            if (tokens.homeLayout != HomeLayout.STANDARD) {
                LedgerNavigationBar(selected = when (route) { ROUTE_HOME -> 0; ROUTE_STATS -> 1; else -> 2 }) { index ->
                    navigateTab(listOf(ROUTE_HOME, ROUTE_STATS, ROUTE_ME)[index], navController)
                }
            } else NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = route == ROUTE_HOME,
                    onClick = { navigateTab(ROUTE_HOME, navController) },
                    icon = { ThemeNavigationIcon(themedIcon(Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List), route == ROUTE_HOME) },
                    label = { Text("明细") },
                    colors = themeNavigationColors()
                )
                NavigationBarItem(
                    selected = route == ROUTE_STATS,
                    onClick = { navigateTab(ROUTE_STATS, navController) },
                    icon = { ThemeNavigationIcon(themedIcon(Icons.Filled.BarChart, Icons.Outlined.BarChart), route == ROUTE_STATS) },
                    label = { Text("统计") },
                    colors = themeNavigationColors()
                )
                NavigationBarItem(
                    selected = route == ROUTE_ME,
                    onClick = { navigateTab(ROUTE_ME, navController) },
                    icon = { ThemeNavigationIcon(themedIcon(Icons.Filled.Settings, Icons.Outlined.Settings), route == ROUTE_ME) },
                    label = { Text("设置") },
                    colors = themeNavigationColors()
                )
            }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).readingSurface()) { content() }
    }
}

@Composable
private fun themeNavigationColors() = if (LocalThemeTokens.current.navigationStyle != SelectionStyle.CLASSIC)
    NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = if (LocalThemeTokens.current.navigationStyle == SelectionStyle.PILL)
            MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) else NavigationBarItemDefaults.colors()

@Composable
private fun ThemeNavigationIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean) {
    if (LocalThemeTokens.current.navigationStyle != SelectionStyle.BOOKMARK) Icon(icon, contentDescription = null)
    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(Modifier.width(16.dp), thickness = 2.dp, color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
    }
}
