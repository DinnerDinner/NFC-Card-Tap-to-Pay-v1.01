package com.example.nfccardtaptopayv101.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen0Screen
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen1Screen
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen0ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen1ViewModel

sealed class PaymentScreen(val route: String) {
    object ProfileGatekeeper : PaymentScreen("profile_gatekeeper")
    object ModeSelection : PaymentScreen("mode_selection")
    object RequestMoney : PaymentScreen("request_money")
    object SendMoney : PaymentScreen("send_money")
    // Add Screen 2-7 routes here as needed
    // object Screen2 : PaymentScreen("screen2")
    // object Screen3 : PaymentScreen("screen3")
    // etc...
}

@Composable
fun ScreensNavGraph(
    userId: String,  // Accept userId parameter
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PaymentScreen.ProfileGatekeeper.route
    ) {
        composable(PaymentScreen.ProfileGatekeeper.route) {
            Screen0Screen(
                onNavigateToModeSelection = {
                    navController.navigate(PaymentScreen.ModeSelection.route) {
                        popUpTo(PaymentScreen.ProfileGatekeeper.route) {
                            inclusive = true
                        }
                    }
                },
                viewModel = viewModel<Screen0ViewModel>().apply {
                    // Initialize ViewModel with userId if needed
                    setUserId(userId)
                }
            )
        }

        composable(PaymentScreen.ModeSelection.route) {
            Screen1Screen(
                onRequestMoney = {
                    navController.navigate(PaymentScreen.RequestMoney.route)
                },
                onSendMoney = {
                    navController.navigate(PaymentScreen.SendMoney.route)
                },
                viewModel = viewModel<Screen1ViewModel>()
            )
        }

        composable(PaymentScreen.RequestMoney.route) {
            // Placeholder for Screen 2 - Request Money Flow
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Request Money Screen - Coming Soon")
            }
        }

        composable(PaymentScreen.SendMoney.route) {
            // Placeholder for Screen 3 - Send Money Flow
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Send Money Screen - Coming Soon")
            }
        }

        // Add more screens here as you develop them:
        // composable(PaymentScreen.Screen2.route) {
        //     Screen2Screen(navController = navController)
        // }
        // etc...
    }
}