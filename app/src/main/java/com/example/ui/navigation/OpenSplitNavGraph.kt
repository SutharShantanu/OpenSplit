package com.example.ui.navigation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.domain.repository.AuthState
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.SignupScreen
import com.example.ui.screens.auth.WelcomeScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ViewModelFactory
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
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            com.example.ui.screens.MainDashboard(appContainer = appContainer, rootNavController = navController)
        }
        composable("group_detail/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupDetailViewModel: com.example.ui.viewmodel.GroupDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.ui.viewmodel.GroupDetailViewModelFactory(groupId, appContainer)
            )
            com.example.ui.screens.GroupDetailScreen(
                viewModel = groupDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddExpense = { navController.navigate("add_expense/$groupId") }
            )
        }
        composable("add_expense/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupDetailViewModel: com.example.ui.viewmodel.GroupDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.ui.viewmodel.GroupDetailViewModelFactory(groupId, appContainer)
            )
            com.example.ui.screens.AddExpenseScreen(
                viewModel = groupDetailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
