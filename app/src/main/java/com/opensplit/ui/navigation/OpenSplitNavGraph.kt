package com.opensplit.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.opensplit.di.AppContainer
import com.opensplit.domain.repository.AuthState
import com.opensplit.ui.screens.SplashScreen
import com.opensplit.ui.screens.auth.ForgotPasswordScreen
import com.opensplit.ui.screens.auth.LoginScreen
import com.opensplit.ui.screens.auth.SignupScreen
import com.opensplit.ui.screens.auth.WelcomeScreen
import com.opensplit.ui.viewmodel.AuthViewModel
import com.opensplit.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun OpenSplitNavGraph(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(appContainer)
    )

    val authState by appContainer.authRepository.getAuthState().collectAsState(initial = AuthState.Loading)
    var splashFinished by remember { mutableStateOf(false) }

    if (!splashFinished) {
        SplashScreen(onTimeout = { splashFinished = true })
    } else {
        when (authState) {
            is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.LoggedOut -> {
                AuthNavHost(navController = navController, authViewModel = authViewModel)
            }
            is AuthState.LoggedIn -> {
                MainNavHost(navController = navController, appContainer = appContainer, authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun AuthNavHost(navController: NavHostController, authViewModel: AuthViewModel) {
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToSignup = { navController.navigate("signup") }
            )
        }
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignup = {
                    navController.navigate("signup") {
                        popUpTo("welcome")
                    }
                },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        composable("signup") {
            SignupScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("welcome")
                    }
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


@Composable
fun MainNavHost(navController: NavHostController, appContainer: AppContainer, authViewModel: AuthViewModel) {
    val hasCompletedPrimer by appContainer.userPreferencesRepository.hasCompletedPermissionPrimer.collectAsState(initial = true)
    val startDest = if (!hasCompletedPrimer) "permission_primer" else "home"

    NavHost(navController = navController, startDestination = startDest) {
        composable("permission_primer") {
            val vm = remember { com.opensplit.ui.viewmodel.PermissionPrimerViewModel(appContainer.userPreferencesRepository) }
            com.opensplit.ui.screens.PermissionPrimerScreen(
                viewModel = vm,
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("permission_primer") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            com.opensplit.ui.screens.MainDashboard(appContainer = appContainer, rootNavController = navController)
        }

        composable("activity") {
            val activityViewModel: com.opensplit.ui.viewmodel.ActivityViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ViewModelFactory(appContainer)
            )
            com.opensplit.ui.screens.ActivityScreen(
                viewModel = activityViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("account") {
            val accountViewModel: com.opensplit.ui.viewmodel.AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ViewModelFactory(appContainer)
            )
            com.opensplit.ui.screens.AccountScreen(
                appContainer = appContainer,
                rootNavController = navController,
                viewModel = accountViewModel
            )
        }
        composable("group_detail/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupDetailViewModel: com.opensplit.ui.viewmodel.GroupDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.opensplit.ui.viewmodel.GroupDetailViewModelFactory(groupId, appContainer)
            )
            com.opensplit.ui.screens.GroupDetailScreen(
                viewModel = groupDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExpense = { navController.navigate("add_expense/$groupId") },
                onNavigateToExpenseDetail = { gId, eId -> navController.navigate("expense_detail/$gId/$eId") }
            )
        }
        composable("expense_detail/{groupId}/{expenseId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
            val vm: com.opensplit.ui.viewmodel.ExpenseDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.opensplit.ui.viewmodel.ExpenseDetailViewModelFactory(groupId, expenseId, appContainer)
            )
            com.opensplit.ui.screens.ExpenseDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("person_balance/{friendId}") { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: return@composable
            val vm: com.opensplit.ui.viewmodel.PersonBalanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.opensplit.ui.viewmodel.PersonBalanceViewModelFactory(friendId, appContainer)
            )
            com.opensplit.ui.screens.PersonBalanceScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("add_expense/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupDetailViewModel: com.opensplit.ui.viewmodel.GroupDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.opensplit.ui.viewmodel.GroupDetailViewModelFactory(groupId, appContainer)
            )
            com.opensplit.ui.screens.AddExpenseScreen(
                viewModel = groupDetailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settle_up/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val vm: com.opensplit.ui.viewmodel.SettleUpViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.opensplit.ui.viewmodel.SettleUpViewModelFactory(
                    groupId = groupId,
                    suggestedToUid = null,
                    suggestedAmount = null,
                    container = appContainer
                )
            )
            com.opensplit.ui.screens.SettleUpScreen(
                viewModel = vm,
                suggestedToUid = null,
                suggestedAmount = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
