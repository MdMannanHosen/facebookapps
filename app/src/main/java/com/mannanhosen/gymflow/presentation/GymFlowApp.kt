package com.mannanhosen.gymflow.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mannanhosen.gymflow.core.session.UserSession
import com.mannanhosen.gymflow.domain.model.UserRole
import com.mannanhosen.gymflow.presentation.auth.LoginScreen
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.BannerType
import com.mannanhosen.gymflow.presentation.components.InfoBanner
import com.mannanhosen.gymflow.presentation.dashboard.OwnerDashboardScreen
import com.mannanhosen.gymflow.presentation.dues.DuesScreen
import com.mannanhosen.gymflow.presentation.member.MemberDashboardScreen
import com.mannanhosen.gymflow.presentation.member.MyMembershipScreen
import com.mannanhosen.gymflow.presentation.membership.MembershipScreen
import com.mannanhosen.gymflow.presentation.members.MemberDetailScreen
import com.mannanhosen.gymflow.presentation.members.MemberEditScreen
import com.mannanhosen.gymflow.presentation.members.MembersScreen
import com.mannanhosen.gymflow.presentation.navigation.Routes
import com.mannanhosen.gymflow.presentation.notifications.NotificationsScreen
import com.mannanhosen.gymflow.presentation.payment.PaymentHistoryScreen
import com.mannanhosen.gymflow.presentation.payment.ReceivePaymentScreen
import com.mannanhosen.gymflow.presentation.plans.PlansScreen
import com.mannanhosen.gymflow.presentation.reports.DailyReportScreen
import com.mannanhosen.gymflow.presentation.settings.SettingsScreen
import com.mannanhosen.gymflow.ui.theme.Spacing

/** Root composable that shows a splash until the session/start destination resolves. */
@Composable
fun GymFlowApp(viewModel: MainViewModel) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    if (startDestination == null) {
        SplashScreen()
    } else {
        GymFlowNavHost(startDestination = startDestination!!, viewModel = viewModel)
    }
}

@Composable
private fun SplashScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GymFlow", style = MaterialTheme.typography.displaySmall)
        }
    }
}

@Composable
private fun GymFlowNavHost(
    startDestination: String,
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val role = UserRole.from(session?.role)
    val userId = session?.userId.orEmpty()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = isTopLevel(role, currentRoute)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomBar(role, currentRoute, navController, userId)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                if (role == UserRole.MEMBER) {
                    MemberDashboardScreen(
                        onRenew = { navController.navigate(Routes.membership(userId, renew = true)) },
                        onPayOnline = { navController.navigate(Routes.PAY_ONLINE) },
                        onViewHistory = { navController.navigate(Routes.paymentHistory(userId)) }
                    )
                } else {
                    OwnerDashboardScreen(
                        onAddMember = { navController.navigate(Routes.memberEdit()) },
                        onReceivePayment = { navController.navigate(Routes.receivePayment()) },
                        onViewMembers = { navController.navigate(Routes.MEMBERS) },
                        onViewDues = { navController.navigate(Routes.DUES) },
                        onViewExpiring = { navController.navigate(Routes.MEMBERS) },
                        onViewReports = { navController.navigate(Routes.REPORTS) },
                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                        onOpenMember = { navController.navigate(Routes.memberDetail(it)) }
                    )
                }
            }

            composable(Routes.MEMBERS) {
                MembersScreen(
                    onAddMember = { navController.navigate(Routes.memberEdit()) },
                    onOpenMember = { navController.navigate(Routes.memberDetail(it)) }
                )
            }

            composable(Routes.DUES) {
                DuesScreen(
                    onBack = {},
                    onCollect = { navController.navigate(Routes.receivePayment(it)) }
                )
            }

            composable(Routes.REPORTS) {
                DailyReportScreen(onBack = {})
            }

            composable(Routes.PLANS) {
                PlansScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PAY_ONLINE) {
                PayOnlineScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.MEMBER_MEMBERSHIP) {
                MyMembershipScreen(
                    onRenew = { navController.navigate(Routes.membership(userId, renew = true)) }
                )
            }

            composable(Routes.MEMBER_PROFILE) {
                SettingsContent(session, viewModel) {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            composable(Routes.SETTINGS) {
                SettingsContent(session, viewModel) {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            composable(
                route = Routes.MEMBER_DETAIL,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) { entry ->
                MemberDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.memberEdit(entry.arguments?.getString("memberId"))) },
                    onReceivePayment = {
                        navController.navigate(Routes.receivePayment(entry.arguments?.getString("memberId")))
                    },
                    onRenew = {
                        navController.navigate(
                            Routes.membership(entry.arguments?.getString("memberId"), renew = true)
                        )
                    }
                )
            }

            composable(
                route = Routes.MEMBER_EDIT,
                arguments = listOf(
                    navArgument("memberId") {
                        type = NavType.StringType
                        defaultValue = null
                    }
                )
            ) {
                MemberEditScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.MEMBERSHIP,
                arguments = listOf(
                    navArgument("memberId") { type = NavType.StringType; defaultValue = null },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "create" }
                )
            ) { entry ->
                val isRenewal = entry.arguments?.getString("mode") == "renew"
                MembershipScreen(
                    memberId = entry.arguments?.getString("memberId"),
                    isRenewal = isRenewal,
                    onBack = { navController.popBackStack() },
                    onPickedMember = { id ->
                        navController.navigate(Routes.membership(id, isRenewal)) {
                            popUpTo(Routes.MEMBERSHIP) { inclusive = true }
                        }
                    },
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.RECEIVE_PAYMENT,
                arguments = listOf(
                    navArgument("memberId") {
                        type = NavType.StringType
                        defaultValue = null
                    }
                )
            ) { entry ->
                ReceivePaymentScreen(
                    memberId = entry.arguments?.getString("memberId"),
                    onBack = { navController.popBackStack() },
                    onPickedMember = { id ->
                        navController.navigate(Routes.receivePayment(id)) {
                            popUpTo(Routes.RECEIVE_PAYMENT) { inclusive = true }
                        }
                    },
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.PAYMENT_HISTORY,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) {
                PaymentHistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun SettingsContent(
    session: UserSession?,
    viewModel: MainViewModel,
    onSignOut: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val role = UserRole.from(session?.role)
    SettingsScreen(
        profileName = session?.fullName.orEmpty(),
        email = session?.email.orEmpty(),
        roleLabel = role.name.lowercase().replaceFirstChar { it.uppercase() },
        themeMode = themeMode,
        onThemeChange = viewModel::setThemeMode,
        onSignOut = onSignOut,
        onBack = {},
        showBack = false,
        onOpenStaffPermissions = if (role == UserRole.OWNER) {
            { /* Staff permission management is a future module. */ }
        } else null
    )
}

@Composable
private fun PayOnlineScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Pay online", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg)
        ) {
            Icon(
                Icons.Filled.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Online payment",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = Spacing.md)
            )
            InfoBanner(
                message = "Online payment will open a secure checkout powered by your payment gateway. Please pay at the front desk for now.",
                type = BannerType.INFO
            )
        }
    }
}

private fun isTopLevel(role: UserRole, route: String?): Boolean {
    if (route == null) return false
    return when (role) {
        UserRole.MEMBER ->
            route == Routes.DASHBOARD ||
                route == Routes.MEMBER_MEMBERSHIP ||
                route == Routes.MEMBER_PROFILE ||
                route.startsWith("payment_history")
        else ->
            route == Routes.DASHBOARD ||
                route == Routes.MEMBERS ||
                route == Routes.DUES ||
                route == Routes.REPORTS ||
                route == Routes.SETTINGS
    }
}

@Composable
private fun BottomBar(
    role: UserRole,
    currentRoute: String?,
    navController: NavHostController,
    userId: String
) {
    val items = if (role == UserRole.MEMBER) {
        listOf(
            NavItem(Routes.DASHBOARD, "Home", Icons.Filled.Home),
            NavItem(Routes.MEMBER_MEMBERSHIP, "Membership", Icons.Filled.Badge),
            NavItem(Routes.paymentHistory(userId), "Payments", Icons.Filled.CreditCard),
            NavItem(Routes.MEMBER_PROFILE, "Profile", Icons.Filled.Person)
        )
    } else {
        listOf(
            NavItem(Routes.DASHBOARD, "Home", Icons.Filled.Home),
            NavItem(Routes.MEMBERS, "Members", Icons.Filled.Groups),
            NavItem(Routes.DUES, "Dues", Icons.Filled.CreditCardOff),
            NavItem(Routes.REPORTS, "Reports", Icons.Filled.Summarize),
            NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
        )
    }

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            val selected = if (item.route == Routes.paymentHistory(userId)) {
                currentRoute?.startsWith("payment_history") == true
            } else {
                currentRoute == item.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)