package com.example.nfccardtaptopayv101.ui.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfccardtaptopayv101.ui.screens.mpos.AddProductScreen
import com.example.nfccardtaptopayv101.ui.screens.mpos.CheckoutScreen
import com.example.nfccardtaptopayv101.ui.screens.mpos.EditDeleteProductScreen
import com.example.nfccardtaptopayv101.ui.screens.mpos.ProductManagerScreen
import com.example.nfccardtaptopayv101.ui.screens.mpos.ScanQRScreen
import com.example.nfccardtaptopayv101.ui.screens.mpos.ScannedProductScreen // You'll need to create this
import com.example.nfccardtaptopayv101.ui.screens.mpos.SalesPageScreen
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScannedProductViewModel

sealed class MposScreens(val route: String) {
    object ProductManager : MposScreens("product_manager")
    object AddProduct : MposScreens("add_product")
    object EditDeleteProduct : MposScreens("edit_delete_product/{productId}") {
        fun createRoute(productId: Int) = "edit_delete_product/$productId"
    }
    object SalesPage : MposScreens("sales_page")
    object Checkout : MposScreens("checkout")
    object ScanQr : MposScreens("scan_qr")
    object ScannedProduct : MposScreens("scanned_product")
}

@Composable
fun MposNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = MposScreens.ProductManager.route,
    onBackToDashboard: () -> Unit
) {
    // Get context for ViewModels
    val context = LocalContext.current

    // Instantiate ViewModels ONCE here, scoped to NavHost
    val salesPageViewModel: SalesPageViewModel = viewModel()
    val scanQrViewModel: ScanQrViewModel = viewModel { ScanQrViewModel(context.applicationContext) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(MposScreens.ProductManager.route) {
            ProductManagerScreen(
                onBack = onBackToDashboard,
                onAddProduct = { navController.navigate(MposScreens.AddProduct.route) },
                onEditProduct = { productId: Int ->
                    navController.navigate(MposScreens.EditDeleteProduct.createRoute(productId))
                }
            )
        }

        composable(MposScreens.AddProduct.route) {
            AddProductScreen(onBack = { navController.popBackStack() })
        }

        composable(MposScreens.EditDeleteProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: -1
            EditDeleteProductScreen(productId = productId, onBack = { navController.popBackStack() })
        }

        composable(MposScreens.SalesPage.route) {
            SalesPageScreen(
                vm = salesPageViewModel,
                onBack = onBackToDashboard,
                onCheckoutClicked = { navController.navigate(MposScreens.Checkout.route) },
                onScanClicked = { navController.navigate(MposScreens.ScanQr.route) }
            )
        }

        composable(MposScreens.Checkout.route) {
            CheckoutScreen(
                vm = salesPageViewModel,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(MposScreens.ScanQr.route) {
            ScanQRScreen(
                vm = scanQrViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToScannedProduct = {
                    navController.navigate(MposScreens.ScannedProduct.route)
                }
            )
        }

        composable(MposScreens.ScannedProduct.route) {
            // Get the scanned data from the scanQrViewModel
            val scannedProduct = scanQrViewModel.scannedProduct
            val scannedCode = scanQrViewModel.scannedCode

            if (scannedProduct != null && scannedCode != null) {
                // Create ScannedProductViewModel with the data
                val scannedProductViewModel: ScannedProductViewModel = viewModel {
                    ScannedProductViewModel(context.applicationContext, scannedProduct, scannedCode)
                }

                ScannedProductScreen(
                    vm = scannedProductViewModel,
                    onBack = {
                        // Clear the scanned data when going back
                        scanQrViewModel.clearScannedData()
                        navController.popBackStack()
                    },
                    onConfirmProduct = { isCorrect ->
                        if (isCorrect) {
                            // Product confirmed - you can add the product to cart here
                            // For now, navigate back to sales page
                            scanQrViewModel.clearScannedData()
                            navController.popBackStack(MposScreens.SalesPage.route, false)
                        } else {
                            // Product not correct - go back to scan again
                            scanQrViewModel.clearScannedData()
                            navController.popBackStack()
                        }
                    }
                )
            } else {
                // If no data available, go back to scan screen
                navController.popBackStack()
            }
        }
    }
}




//package com.example.nfccardtaptopayv101.ui.navigation
//import androidx.navigation.NavType
//import androidx.navigation.navArgument
//
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.nfccardtaptopayv101.ui.screens.mpos.AddProductScreen
//import com.example.nfccardtaptopayv101.ui.screens.mpos.CheckoutScreen
//import com.example.nfccardtaptopayv101.ui.screens.mpos.EditDeleteProductScreen
//import com.example.nfccardtaptopayv101.ui.screens.mpos.ProductManagerScreen
//import com.example.nfccardtaptopayv101.ui.screens.mpos.ScanQRScreen
//import com.example.nfccardtaptopayv101.ui.screens.mpos.SalesPageScreen
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrViewModel
//
//sealed class MposScreens(val route: String) {
//    object ProductManager : MposScreens("product_manager")
//    object AddProduct : MposScreens("add_product")
//    object EditDeleteProduct : MposScreens("edit_delete_product/{productId}") { fun createRoute(productId: Int) = "edit_delete_product/$productId"}
//    object SalesPage : MposScreens("sales_page")
//    object Checkout : MposScreens("checkout")
//    object ScanQr : MposScreens("scan_qr")
//}
//
//@Composable
//fun MposNavGraph(
//    navController: NavHostController = rememberNavController(),
//    startDestination: String = MposScreens.ProductManager.route,
//    onBackToDashboard: () -> Unit
//) {
//    // Get context for ViewModels
//    val context = LocalContext.current
//
//    // Instantiate ViewModels ONCE here, scoped to NavHost
//    val salesPageViewModel: SalesPageViewModel = viewModel()
//    val scanQrViewModel: ScanQrViewModel = viewModel { ScanQrViewModel(context.applicationContext) }
//
//    NavHost(
//        navController = navController,
//        startDestination = startDestination
//    ) {
//        composable(MposScreens.ProductManager.route) {
//            ProductManagerScreen(
//                onBack = onBackToDashboard,
//                onAddProduct = { navController.navigate(MposScreens.AddProduct.route) },
//                onEditProduct = { productId: Int ->
//                    navController.navigate(MposScreens.EditDeleteProduct.createRoute(productId))
//                }
//            )
//        }
//
//        composable(MposScreens.AddProduct.route) {
//            AddProductScreen(onBack = { navController.popBackStack() })
//        }
//
//        composable(MposScreens.EditDeleteProduct.route,
//            arguments = listOf(navArgument("productId") { type = NavType.IntType })
//        ) { backStackEntry ->
//            val productId = backStackEntry.arguments?.getInt("productId") ?: -1
//            EditDeleteProductScreen(productId = productId, onBack = { navController.popBackStack() })
//        }
//
//        composable(MposScreens.SalesPage.route) {
//            SalesPageScreen(
//                vm = salesPageViewModel,
//                onBack = onBackToDashboard,
//                onCheckoutClicked = { navController.navigate(MposScreens.Checkout.route) },
//                onScanClicked = { navController.navigate(MposScreens.ScanQr.route) }
//            )
//        }
//
//        composable(MposScreens.Checkout.route) {
//            CheckoutScreen(
//                vm = salesPageViewModel,
//                navController = navController,
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        composable(MposScreens.ScanQr.route) {
//            ScanQRScreen(
//                vm = scanQrViewModel,
//                onBack = { navController.popBackStack() },
//                onSuccessScan = { message ->
//                    // Pop back to SalesPage and the screen will handle the toast
//                    navController.popBackStack()
//                }
//            )
//        }
//    }
//}
//
//
