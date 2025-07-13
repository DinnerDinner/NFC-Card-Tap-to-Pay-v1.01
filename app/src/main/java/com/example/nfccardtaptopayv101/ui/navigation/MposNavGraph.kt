package com.example.nfccardtaptopayv101.ui.navigation
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfccardtaptopayv101.ui.screens.AddProductScreen
import com.example.nfccardtaptopayv101.ui.screens.ProductManagerScreen
import com.example.nfccardtaptopayv101.ui.screens.EditDeleteProductScreen

sealed class MposScreens(val route: String) {
    object ProductManager : MposScreens("product_manager")
    object AddProduct : MposScreens("add_product")
    object EditDeleteProduct : MposScreens("edit_delete_product")
}

@Composable
fun MposNavGraph(
    navController: NavHostController = rememberNavController(),
    onBackToDashboard: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = MposScreens.ProductManager.route
    ) {
        composable(MposScreens.ProductManager.route) {
            ProductManagerScreen(
                onBack = onBackToDashboard,
                onAddProduct = {
                    navController.navigate(MposScreens.AddProduct.route)
                },
                onEditProduct = { sku ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("edit_sku", sku)
                    navController.navigate(MposScreens.EditDeleteProduct.route)
                }
            )
        }

        composable(MposScreens.AddProduct.route) {
            AddProductScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(MposScreens.EditDeleteProduct.route) {
            val sku = remember {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("edit_sku") ?: ""
            }

            EditDeleteProductScreen(
                sku = sku,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
