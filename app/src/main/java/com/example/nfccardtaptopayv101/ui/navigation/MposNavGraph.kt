package com.example.nfccardtaptopayv101.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfccardtaptopayv101.ui.screens.*
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel

sealed class MposScreens(val route: String) {
    object ProductManager : MposScreens("product_manager")
    object AddProduct : MposScreens("add_product")
    object EditDeleteProduct : MposScreens("edit_delete_product")
    object SalesPage : MposScreens("sales_page")
    object Checkout : MposScreens("checkout")
}

@Composable
fun MposNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = MposScreens.ProductManager.route,
    onBackToDashboard: () -> Unit
) {
    // Instantiate ViewModel ONCE here, scoped to NavHost
    val salesPageViewModel: SalesPageViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(MposScreens.ProductManager.route) {
            ProductManagerScreen(
                onBack = onBackToDashboard,
                onAddProduct = { navController.navigate(MposScreens.AddProduct.route) },
                onEditProduct = { sku ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("edit_sku", sku)
                    navController.navigate(MposScreens.EditDeleteProduct.route)
                }
            )
        }

        composable(MposScreens.AddProduct.route) {
            AddProductScreen(onBack = { navController.popBackStack() })
        }

        composable(MposScreens.EditDeleteProduct.route) {
            val sku = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("edit_sku") ?: ""
            EditDeleteProductScreen(sku = sku, onBack = { navController.popBackStack() })
        }

        composable(MposScreens.SalesPage.route) {
            // Pass the SAME ViewModel instance here
            SalesPageScreen(
                vm = salesPageViewModel,
                onBack = onBackToDashboard,
                onCheckoutClicked = { navController.navigate(MposScreens.Checkout.route) },
                onScanClicked = { /* TODO */ }
            )
        }

        composable(MposScreens.Checkout.route) {
            CheckoutScreen(
                vm = salesPageViewModel,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

    }
}
