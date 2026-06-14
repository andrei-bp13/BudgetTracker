package com.pardonmyspending.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.ui.screens.*
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.ChartBar
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home         : Screen("home",       "Home",     Icons.Rounded.Home)
    object Reports      : Screen("reports",    "Reports",  Icons.Rounded.BarChart)
    object Calendar     : Screen("calendar",   "Calendar", Icons.Rounded.CalendarMonth)
    object Categories   : Screen("categories", "Categories", Icons.Rounded.Category)
    object CategoryDetail : Screen("category/{name}", "Category", Icons.Rounded.BarChart) {
        fun route(name: String) = "category/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    object DrillDown : Screen("drilldown/{title}/{startDate}/{endDate}/{transactionType}", "Detail", Icons.Rounded.BarChart) {
        fun route(title: String, startDate: LocalDate, endDate: LocalDate, transactionType: TransactionType = TransactionType.EXPENSE) =
            "drilldown/${java.net.URLEncoder.encode(title, "UTF-8")}/$startDate/$endDate/${transactionType.name}"
    }
    object PaymentTypeDetail : Screen("payment/{typeName}", "Payment", Icons.Rounded.Home) {
        fun route(type: PaymentType) = "payment/${type.name}"
    }
    object Budget     : Screen("budget",    "Budget",    Icons.Rounded.Savings)
    object Recurring  : Screen("recurring", "Recurring", Icons.Rounded.Repeat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerNavGraph(viewModel: TransactionViewModel) {
    val navController       = rememberNavController()
    val drawerState         = rememberDrawerState(DrawerValue.Closed)
    var showAddSheet          by remember { mutableStateOf(false) }
    var showAddRecurringSheet by remember { mutableStateOf(false) }
    var editingTransaction    by remember { mutableStateOf<com.pardonmyspending.app.data.model.Transaction?>(null) }
    var editingRecurring      by remember { mutableStateOf<com.pardonmyspending.app.data.model.RecurringTransaction?>(null) }
    var fabExpanded           by remember { mutableStateOf(false) }

    BackHandler(enabled = fabExpanded) { fabExpanded = false }
    val scope               = rememberCoroutineScope()
    val currencySymbol      by viewModel.currencySymbol.collectAsState()
    val snackbarHostState   = remember { SnackbarHostState() }
    var reportsEditMode     by remember { mutableStateOf(false) }
    var homeEditMode        by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.importErrorEvent.collect {
            snackbarHostState.showSnackbar(
                message  = "Some rows couldn't be imported — error log saved to Downloads",
                duration = SnackbarDuration.Long
            )
        }
    }

    // Track current route at the top level — used for both topBar visibility and bottomBar selection
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route
    val isTopLevel = route == null || route in setOf(Screen.Home.route, Screen.Reports.route, Screen.Calendar.route, Screen.Budget.route, Screen.Recurring.route)

    // Reset edit modes and FAB when navigating away from their screens
    LaunchedEffect(route) {
        fabExpanded = false
        if (route != Screen.Reports.route) reportsEditMode = false
        if (route != Screen.Home.route)    homeEditMode    = false
    }

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
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    val fabRotation by animateFloatAsState(
                        targetValue    = if (fabExpanded) 45f else 0f,
                        animationSpec  = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "fabRotation"
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                    slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it / 2 },
                            exit  = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                    slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { it / 2 }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick         = { fabExpanded = false; showAddRecurringSheet = true },
                                    shape           = RoundedCornerShape(percent = 50),
                                    color           = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor    = androidx.compose.ui.graphics.Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier              = Modifier.padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text("Recurring Transaction", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                Surface(
                                    onClick         = { fabExpanded = false; showAddSheet = true },
                                    shape           = RoundedCornerShape(percent = 50),
                                    color           = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor    = androidx.compose.ui.graphics.Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier              = Modifier.padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text("Transaction", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick        = { fabExpanded = !fabExpanded },
                            shape          = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.Add,
                                contentDescription = if (fabExpanded) "Close" else "Add",
                                modifier           = Modifier.rotate(fabRotation)
                            )
                        }
                    }
                },
                topBar = {
                    // Only show the global top bar for the three top-level tabs;
                    // sub-screens (DrillDown, CategoryDetail, etc.) carry their own TopAppBar.
                    if (isTopLevel) {
                        TopAppBar(
                            title = {
                                Text(
                                    when (route) {
                                        Screen.Home.route      -> "Dashboard"
                                        Screen.Reports.route   -> "Reports"
                                        Screen.Calendar.route  -> "Calendar"
                                        Screen.Budget.route    -> "Budget"
                                        Screen.Recurring.route -> "Recurring"
                                        else                   -> ""
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                if (route == Screen.Home.route || route == Screen.Reports.route) {
                                    val isEditMode = (route == Screen.Home.route && homeEditMode) ||
                                                     (route == Screen.Reports.route && reportsEditMode)
                                    IconButton(onClick = {
                                        when (route) {
                                            Screen.Home.route    -> homeEditMode    = !homeEditMode
                                            Screen.Reports.route -> reportsEditMode = !reportsEditMode
                                        }
                                    }) {
                                        Icon(
                                            imageVector        = if (isEditMode) Icons.Rounded.Done else Icons.Rounded.Edit,
                                            contentDescription = if (isEditMode) "Done editing" else "Edit layout",
                                            tint               = if (isEditMode) MaterialTheme.colorScheme.primary
                                                                  else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(onClick = { /* TODO: account screen */ }) {
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
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        tonalElevation = 4.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        val current = currentEntry?.destination

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

                        // 3 — Budget
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == Screen.Budget.route } == true,
                            onClick  = { navController.navigate(Screen.Budget.route) { launchSingleTop = true; restoreState = true } },
                            icon     = { Icon(Screen.Budget.icon, contentDescription = Screen.Budget.label) },
                            label    = { Text(Screen.Budget.label) }
                        )

                        // 4 — Calendar
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == Screen.Calendar.route } == true,
                            onClick  = { navController.navigate(Screen.Calendar.route) { launchSingleTop = true; restoreState = true } },
                            icon     = { Icon(Screen.Calendar.icon, contentDescription = Screen.Calendar.label) },
                            label    = { Text(Screen.Calendar.label) }
                        )

                        // 5 — Recurring payments
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == Screen.Recurring.route } == true,
                            onClick  = { navController.navigate(Screen.Recurring.route) { launchSingleTop = true; restoreState = true } },
                            icon     = { Icon(Screen.Recurring.icon, contentDescription = Screen.Recurring.label) },
                            label    = { Text(Screen.Recurring.label) }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Home.route,
                    modifier         = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel           = viewModel,
                            editMode            = homeEditMode,
                            onAddTransaction    = { showAddSheet = true },
                            onCategoryClick     = { name -> navController.navigate(Screen.CategoryDetail.route(name)) },
                            onPaymentTypeClick  = { pt -> navController.navigate(Screen.PaymentTypeDetail.route(pt)) },
                            onEditTransaction   = { t -> editingTransaction = t },
                            onDeleteTransaction = { t -> viewModel.deleteTransaction(t.id) }
                        )
                    }
                    composable(
                        Screen.CategoryDetail.route,
                        enterTransition    = { slideInHorizontally { it } + fadeIn() },
                        popExitTransition  = { slideOutHorizontally { it } + fadeOut() }
                    ) { backStack ->
                        val raw  = backStack.arguments?.getString("name") ?: ""
                        val name = java.net.URLDecoder.decode(raw, "UTF-8")
                        CategoryTransactionsScreen(
                            categoryName = name,
                            viewModel    = viewModel,
                            onBack       = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Reports.route) {
                        ReportsScreen(
                            viewModel        = viewModel,
                            editMode         = reportsEditMode,
                            onBarClick       = { bar ->
                                navController.navigate(
                                    Screen.DrillDown.route(bar.title, bar.startDate, bar.endDate)
                                )
                            },
                            onIncomeBarClick = { bar ->
                                navController.navigate(
                                    Screen.DrillDown.route(bar.title, bar.startDate, bar.endDate, TransactionType.INCOME)
                                )
                            }
                        )
                    }
                    composable(
                        Screen.DrillDown.route,
                        enterTransition    = { slideInHorizontally { it } + fadeIn() },
                        popExitTransition  = { slideOutHorizontally { it } + fadeOut() }
                    ) { backStack ->
                        val rawTitle = backStack.arguments?.getString("title") ?: ""
                        val title    = java.net.URLDecoder.decode(rawTitle, "UTF-8")
                        val start    = backStack.arguments?.getString("startDate")
                            ?.let { LocalDate.parse(it) } ?: LocalDate.now()
                        val end      = backStack.arguments?.getString("endDate")
                            ?.let { LocalDate.parse(it) } ?: LocalDate.now()
                        val typeName = backStack.arguments?.getString("transactionType") ?: "EXPENSE"
                        val txType   = runCatching { TransactionType.valueOf(typeName) }.getOrDefault(TransactionType.EXPENSE)
                        DrillDownScreen(
                            title               = title,
                            startDate           = start,
                            endDate             = end,
                            transactionType     = txType,
                            viewModel           = viewModel,
                            onBack              = { navController.popBackStack() },
                            onEditTransaction   = { t -> editingTransaction = t },
                            onDeleteTransaction = { t -> viewModel.deleteTransaction(t.id) }
                        )
                    }
                    composable(Screen.Calendar.route) {
                        CalendarScreen(
                            viewModel           = viewModel,
                            onEditTransaction   = { t -> editingTransaction = t },
                            onDeleteTransaction = { t -> viewModel.deleteTransaction(t.id) }
                        )
                    }
                    composable(
                        Screen.PaymentTypeDetail.route,
                        enterTransition    = { slideInHorizontally { it } + fadeIn() },
                        popExitTransition  = { slideOutHorizontally { it } + fadeOut() }
                    ) { backStack ->
                        val typeName    = backStack.arguments?.getString("typeName") ?: "DEBIT"
                        val paymentType = runCatching { PaymentType.valueOf(typeName) }.getOrDefault(PaymentType.DEBIT)
                        PaymentTypeTransactionsScreen(
                            paymentType = paymentType,
                            viewModel   = viewModel,
                            onBack      = { navController.popBackStack() }
                        )
                    }
                    composable(
                        Screen.Categories.route,
                        enterTransition    = { slideInHorizontally { it } + fadeIn() },
                        popExitTransition  = { slideOutHorizontally { it } + fadeOut() }
                    ) {
                        CategoriesScreen(
                            viewModel = viewModel,
                            onBack    = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Budget.route) {
                        BudgetScreen()
                    }
                    composable(Screen.Recurring.route) {
                        RecurringScreen(
                            viewModel       = viewModel,
                            onEditRecurring = { rec -> editingRecurring = rec }
                        )
                    }
                }

                // Dismiss speed-dial when tapping outside the FAB
                if (fabExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) { detectTapGestures { fabExpanded = false } }
                    )
                }
                } // end Box
            }
        }

        if (showAddSheet || editingTransaction != null) {
            AddTransactionSheet(
                viewModel          = viewModel,
                initialTransaction = editingTransaction,
                onDismiss          = {
                    showAddSheet       = false
                    editingTransaction = null
                }
            )
        }

        if (showAddRecurringSheet || editingRecurring != null) {
            AddRecurringTransactionSheet(
                viewModel        = viewModel,
                initialRecurring = editingRecurring,
                onDismiss        = {
                    showAddRecurringSheet = false
                    editingRecurring      = null
                }
            )
        }
    } // end CompositionLocalProvider
}
