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
import com.example.nfccardtaptopayv101.ui.screens.mpos.SalesPageScreen
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrViewModel

sealed class MposScreens(val route: String) {
    object ProductManager : MposScreens("product_manager")
    object AddProduct : MposScreens("add_product")
    object EditDeleteProduct : MposScreens("edit_delete_product/{productId}") { fun createRoute(productId: Int) = "edit_delete_product/$productId"}
    object SalesPage : MposScreens("sales_page")
    object Checkout : MposScreens("checkout")
    object ScanQr : MposScreens("scan_qr")
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
                onSuccessScan = { message ->
                    // Pop back to SalesPage and the screen will handle the toast
                    navController.popBackStack()
                }
            )
        }
    }
}



//package com.example.nfccardtaptopayv101.ui.navigation
//import androidx.navigation.NavType
//import androidx.navigation.navArgument
//
//import androidx.compose.runtime.Composable
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
//
//sealed class MposScreens(val route: String) {
//    object ProductManager : MposScreens("product_manager")
//    object AddProduct : MposScreens("add_product")
//    object EditDeleteProduct : MposScreens("edit_delete_product/{productId}") {
//        fun createRoute(productId: Int) = "edit_delete_product/$productId"}
//    object SalesPage : MposScreens("sales_page")
//    object Checkout : MposScreens("checkout")
//
//    object ScanQr : MposScreens("scan_qr")
//}
//
//@Composable
//fun MposNavGraph(
//    navController: NavHostController = rememberNavController(),
//    startDestination: String = MposScreens.ProductManager.route,
//    onBackToDashboard: () -> Unit
//) {
//    // Instantiate ViewModel ONCE here, scoped to NavHost
//    val salesPageViewModel: SalesPageViewModel = viewModel()
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
//            // Pass the SAME ViewModel instance here
//            SalesPageScreen(
//                vm = salesPageViewModel,
//                onBack = onBackToDashboard,
//                onCheckoutClicked = { navController.navigate(MposScreens.Checkout.route) },
//                onScanClicked = { navController.navigate(MposScreens.ScanQr.route) }  // Navigate to Scan QR here
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
//            // You can instantiate ScanQrViewModel here or let the screen do it
//            ScanQRScreen(
//                onBack = { navController.popBackStack() },
//                onSuccessScan = { barcode ->
//                    // For now, just pop back to SalesPage (or wherever you want)
//                    // You can pass barcode as argument or handle next later
//                    navController.popBackStack()
//                    // TODO: Later navigate to ScanConfirmScreen with barcode
//                }
//            )
//        }
//
//    }
//}
