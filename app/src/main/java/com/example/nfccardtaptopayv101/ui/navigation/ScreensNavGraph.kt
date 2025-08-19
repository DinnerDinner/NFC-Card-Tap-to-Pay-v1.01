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
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen2Screen
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen3Screen
import com.example.nfccardtaptopayv101.ui.screens.ble.Screen4Screen
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen0ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen1ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen2ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen3ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen4ViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.NearbyUser

sealed class PaymentScreen(val route: String) {
    object ProfileGatekeeper : PaymentScreen("profile_gatekeeper")
    object ModeSelection : PaymentScreen("mode_selection")
    object RequestMoney : PaymentScreen("request_money")
    object SendMoney : PaymentScreen("send_money")
    object AmountInput : PaymentScreen("amount_input/{userId}/{userName}/{userImageUrl}")
    object WaitingScreen : PaymentScreen("waiting_screen")
    object TransactionDetails : PaymentScreen("transaction_details/{userId}/{userName}")
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
            val viewModel: Screen2ViewModel = viewModel()

            Screen2Screen(
                viewModel = viewModel,
                onUserSelected = { selectedUser ->
                    // Navigate to amount input screen (Screen 4) with selected user info
                    val encodedImageUrl = java.net.URLEncoder.encode(selectedUser.userProfileImageUrl, "UTF-8")
                    val encodedUserName = java.net.URLEncoder.encode(selectedUser.userName.ifEmpty { "User ${selectedUser.userId}" }, "UTF-8")

                    navController.navigate(
                        PaymentScreen.AmountInput.route
                            .replace("{userId}", selectedUser.userId)
                            .replace("{userName}", encodedUserName)
                            .replace("{userImageUrl}", encodedImageUrl)
                    )
                }
            )
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

        composable(PaymentScreen.AmountInput.route) { backStackEntry ->
            val selectedUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val selectedUserName = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("userName") ?: "", "UTF-8")
            val selectedUserImageUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("userImageUrl") ?: "", "UTF-8")

            val viewModel: Screen4ViewModel = viewModel()

            // Create NearbyUser object from navigation arguments
            val selectedUser = NearbyUser(
                userId = selectedUserId,
                userName = selectedUserName,
                userProfileImageUrl = selectedUserImageUrl,
                isLoadingData = false
            )

            Screen4Screen(
                viewModel = viewModel,
                selectedUser = selectedUser,
                merchantUserId = userId,
                onNavigateToWaitingScreen = {
                    navController.navigate(PaymentScreen.WaitingScreen.route) {
                        popUpTo(PaymentScreen.RequestMoney.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(PaymentScreen.WaitingScreen.route) {
            // Placeholder for Screen aaa6 (Waiting/Success Screen)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting Screen (Screen 6) - Coming Soon\nWaiting for payment confirmation...")
            }
        }

        composable(PaymentScreen.TransactionDetails.route) { backStackEntry ->
            val selectedUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val selectedUserName = backStackEntry.arguments?.getString("userName") ?: ""

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Transaction Details Screen - Coming Soon\nSelected User: $selectedUserName (ID: $selectedUserId)")
            }
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
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.nfccardtaptopayv101.ui.screens.ble.Screen0Screen
//import com.example.nfccardtaptopayv101.ui.screens.ble.Screen1Screen
//import com.example.nfccardtaptopayv101.ui.screens.ble.Screen2Screen
//import com.example.nfccardtaptopayv101.ui.screens.ble.Screen3Screen
//import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen0ViewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen1ViewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen2ViewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen3ViewModel
//
//sealed class PaymentScreen(val route: String) {
//    object ProfileGatekeeper : PaymentScreen("profile_gatekeeper")
//    object ModeSelection : PaymentScreen("mode_selection")
//    object RequestMoney : PaymentScreen("request_money")
//    object SendMoney : PaymentScreen("send_money")
//    object TransactionDetails : PaymentScreen("transaction_details/{userId}/{userName}")
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
//            val viewModel: Screen2ViewModel = viewModel()
//
//            Screen2Screen(
//                viewModel = viewModel,
//                onUserSelected = { selectedUser ->
//                    // Navigate to transaction details screen with selected user info
//                    navController.navigate(
//                        PaymentScreen.TransactionDetails.route
//                            .replace("{userId}", selectedUser.userId)
//                            .replace("{userName}", selectedUser.userName.ifEmpty { "User ${selectedUser.userId}" })
//                    )
//                }
//            )
//        }
//
//        composable(PaymentScreen.SendMoney.route) {
//            val viewModel: Screen3ViewModel = viewModel()
//
//            // Set userId - ViewModel will handle fetching user data
//            LaunchedEffect(userId) {
//                if (userId.isNotEmpty()) {
//                    viewModel.setUserId(userId)
//                }
//            }
//
//            Screen3Screen(
//                viewModel = viewModel
//            )
//        }
//
//        composable(PaymentScreen.TransactionDetails.route) { backStackEntry ->
//            val selectedUserId = backStackEntry.arguments?.getString("userId") ?: ""
//            val selectedUserName = backStackEntry.arguments?.getString("userName") ?: ""
//
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Transaction Details Screen - Coming Soon\nSelected User: $selectedUserName (ID: $selectedUserId)")
//            }
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
