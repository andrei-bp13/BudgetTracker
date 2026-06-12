package com.budgettracker.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch
import com.budgettracker.app.ui.screens.*
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.viewmodel.TransactionViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home         : Screen("home",       "Home",     Icons.Rounded.Home)
    object Reports      : Screen("reports",    "Reports",  Icons.Rounded.BarChart)
    object Calendar     : Screen("calendar",   "Calendar", Icons.Rounded.CalendarMonth)
    object Categories   : Screen("categories", "Categories", Icons.Rounded.Category)
    object CategoryDetail : Screen("category/{name}", "Category", Icons.Rounded.BarChart) {
        fun route(name: String) = "category/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerNavGraph(viewModel: TransactionViewModel) {
    val navController  = rememberNavController()
    val drawerState    = rememberDrawerState(DrawerValue.Closed)
    var showAddSheet   by remember { mutableStateOf(false) }
    val scope          = rememberCoroutineScope()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SettingsDrawer(
                    viewModel              = viewModel,
                    onClose                = { scope.launch { drawerState.close() } },
                    onNavigateToCategories = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Categories.route)
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            val currentEntry by navController.currentBackStackEntryAsState()
                            val route = currentEntry?.destination?.route
                            Text(
                                when (route) {
                                    "home"     -> "Dashboard"
                                    "reports"  -> "Reports"
                                    "calendar" -> "Calendar"
                                    else       -> "Dashboard"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        tonalElevation = 4.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        val navBackStack by navController.currentBackStackEntryAsState()
                        val current = navBackStack?.destination

                        // 1 — Home
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == Screen.Home.route } == true,
                            onClick  = { navController.navigate(Screen.Home.route) { launchSingleTop = true; restoreState = true } },
                            icon     = { Icon(Screen.Home.icon, contentDescription = Screen.Home.label) },
                            label    = { Text(Screen.Home.label) }
                        )

                        // 2 — Reports
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == Screen.Reports.route } == true,
                            onClick  = { navController.navigate(Screen.Reports.route) { launchSingleTop = true; restoreState = true } },
                            icon     = { Icon(Screen.Reports.icon, contentDescription = Screen.Reports.label) },
                            label    = { Text(Screen.Reports.label) }
                        )

                        // 3 — Add (circle FAB)
                        NavigationBarItem(
                            selected = false,
                            onClick  = { showAddSheet = true },
                            icon = {
                                Surface(
                                    shape    = CircleShape,
                                    color    = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            },
                            label = { Text("Add") }
                        )

                        // 4 — Calendar
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == Screen.Calendar.route } == true,
                            onClick  = { navController.navigate(Screen.Calendar.route) { launchSingleTop = true; restoreState = true } },
                            icon     = { Icon(Screen.Calendar.icon, contentDescription = Screen.Calendar.label) },
                            label    = { Text(Screen.Calendar.label) }
                        )

                        // 5 — Account avatar
                        NavigationBarItem(
                            selected = false,
                            onClick  = { /* TODO: account screen */ },
                            icon = {
                                Surface(
                                    shape    = CircleShape,
                                    color    = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "A",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            },
                            label = { Text("Account") }
                        )
                    }
                }
            ) { padding ->
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Home.route,
                    modifier         = Modifier.padding(padding),
                    enterTransition  = { slideInHorizontally { it } + fadeIn() },
                    exitTransition   = { slideOutHorizontally { -it } + fadeOut() },
                    popEnterTransition  = { slideInHorizontally { -it } + fadeIn() },
                    popExitTransition   = { slideOutHorizontally { it } + fadeOut() }
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel        = viewModel,
                            onAddTransaction = { showAddSheet = true },
                            onCategoryClick  = { name -> navController.navigate(Screen.CategoryDetail.route(name)) }
                        )
                    }
                    composable(Screen.CategoryDetail.route) { backStack ->
                        val raw  = backStack.arguments?.getString("name") ?: ""
                        val name = java.net.URLDecoder.decode(raw, "UTF-8")
                        CategoryTransactionsScreen(
                            categoryName = name,
                            viewModel    = viewModel,
                            onBack       = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Reports.route) {
                        ReportsScreen(viewModel = viewModel)
                    }
                    composable(Screen.Calendar.route) {
                        CalendarScreen(viewModel = viewModel)
                    }
                    composable(Screen.Categories.route) {
                        CategoriesScreen(
                            viewModel = viewModel,
                            onBack    = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        if (showAddSheet) {
            AddTransactionSheet(
                viewModel = viewModel,
                onDismiss = { showAddSheet = false }
            )
        }
    } // end CompositionLocalProvider
}
