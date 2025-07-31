package com.example.nfccardtaptopayv101.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen0Screen
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen1Screen
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen3Screen
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen0ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen1ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen3ViewModel

sealed class PaymentScreen(val route: String) {
    object ProfileGatekeeper : PaymentScreen("profile_gatekeeper")
    object ModeSelection : PaymentScreen("mode_selection")
    object RequestMoney : PaymentScreen("request_money")
    object SendMoney : PaymentScreen("send_money")
}

@Composable
fun ScreensNavGraph(
    userId: String,
    navController: NavHostController = rememberNavController()
) {
    // Validate userId before proceeding
    if (userId.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Error: Invalid user ID")
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = PaymentScreen.ProfileGatekeeper.route
    ) {
        composable(PaymentScreen.ProfileGatekeeper.route) {
            // Create ViewModel and pass userId immediately
            val viewModel: Screen0ViewModel = viewModel()

            // Set userId as soon as the ViewModel is created
            LaunchedEffect(userId) {
                if (userId.isNotEmpty()) {
                    viewModel.setUserId(userId)
                }
            }

            Screen0Screen(
                onNavigateToModeSelection = {
                    navController.navigate(PaymentScreen.ModeSelection.route) {
                        popUpTo(PaymentScreen.ProfileGatekeeper.route) {
                            inclusive = true
                        }
                    }
                },
                viewModel = viewModel
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Request Money Screen - Coming Soon")
            }
        }

        composable(PaymentScreen.SendMoney.route) {
            val viewModel: Screen3ViewModel = viewModel()

            // Set userId - ViewModel will handle fetching user data
            LaunchedEffect(userId) {
                if (userId.isNotEmpty()) {
                    viewModel.setUserId(userId)
                }
            }

            Screen3Screen(
                viewModel = viewModel
            )
        }
    }
}


//package com.example.nfccardtaptopayv101.ui.navigation
//
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.nfccardtaptopayv101.ui.screens.ble.Screen0Screen
//import com.example.nfccardtaptopayv101.ui.screens.ble.Screen1Screen
//import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen0ViewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen1ViewModel
//
//sealed class PaymentScreen(val route: String) {
//    object ProfileGatekeeper : PaymentScreen("profile_gatekeeper")
//    object ModeSelection : PaymentScreen("mode_selection")
//    object RequestMoney : PaymentScreen("request_money")
//    object SendMoney : PaymentScreen("send_money")
//}
//
//@Composable
//fun ScreensNavGraph(
//    userId: String,
//    navController: NavHostController = rememberNavController()
//) {
//    // Validate userId before proceeding
//    if (userId.isEmpty()) {
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            Text("Error: Invalid user ID")
//        }
//        return
//    }
//
//    NavHost(
//        navController = navController,
//        startDestination = PaymentScreen.ProfileGatekeeper.route
//    ) {
//        composable(PaymentScreen.ProfileGatekeeper.route) {
//            // Create ViewModel and pass userId immediately
//            val viewModel: Screen0ViewModel = viewModel()
//
//            // Set userId as soon as the ViewModel is created
//            LaunchedEffect(userId) {
//                if (userId.isNotEmpty()) {
//                    viewModel.setUserId(userId)
//                }
//            }
//
//            Screen0Screen(
//                onNavigateToModeSelection = {
//                    navController.navigate(PaymentScreen.ModeSelection.route) {
//                        popUpTo(PaymentScreen.ProfileGatekeeper.route) {
//                            inclusive = true
//                        }
//                    }
//                },
//                viewModel = viewModel
//            )
//        }
//
//        composable(PaymentScreen.ModeSelection.route) {
//            Screen1Screen(
//                onRequestMoney = {
//                    navController.navigate(PaymentScreen.RequestMoney.route)
//                },
//                onSendMoney = {
//                    navController.navigate(PaymentScreen.SendMoney.route)
//                },
//                viewModel = viewModel<Screen1ViewModel>()
//            )
//        }
//
//        composable(PaymentScreen.RequestMoney.route) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Request Money Screen - Coming Soon")
//            }
//        }
//
//        composable(PaymentScreen.SendMoney.route) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Send Money Screen - Coming Soon")
//            }
//        }
//    }
//}