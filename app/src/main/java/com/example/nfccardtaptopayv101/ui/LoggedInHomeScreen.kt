package com.example.nfccardtaptopayv101.ui
import androidx.compose.material3.TopAppBar
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nfccardtaptopayv101.LoginActivity
import kotlinx.coroutines.launch
//@file:OptIn(ExperimentalMaterial3Api::class)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedInHomeScreen(userData: String) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf("Profile") }
    val context = LocalContext.current

    val menuItems = listOf(
        "Profile" to Icons.Default.AccountCircle,
        "Merchant Tap Machine" to Icons.Default.PointOfSale,
        "Cards" to Icons.Default.CreditCard,
        "Wallet Top-Up" to Icons.Default.AccountBalanceWallet,
        "Product Database" to Icons.Default.List
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(250.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "🏦 Menu",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()

                menuItems.forEach { (label, icon) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = selectedScreen == label,
                        onClick = {
                            selectedScreen = label
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()
                NavigationDrawerItem(
                    label = { Text("Log Out") },
                    selected = false,
                    onClick = {
                        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Logout") },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedScreen) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (selectedScreen) {
                    "Profile" -> ProfileScreen()
                    "Merchant Tap Machine" -> MerchantScreen()
                    "Cards" -> CardsScreen()
                    "Wallet Top-Up" -> WalletTopUpScreen()
                    "Product Database" -> ProductDatabaseScreen()
                }
            }
        }
    }
}










//package com.example.nfccardtaptopayv101.ui
//
//import android.content.Context
//import android.content.Intent
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.foundation.layout.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.example.nfccardtaptopayv101.LoginActivity
//
//@Composable
//fun LoggedInHomeScreen(userData: String) {
//    val context = LocalContext.current
//
//    Column(modifier = Modifier
//        .fillMaxSize()
//        .padding(24.dp),
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(text = "Welcome back!\nUser data:\n$userData")
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Button(onClick = {
//            // Clear saved user data (logout)
//            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//            prefs.edit().clear().apply()
//
//            // Navigate back to LoginActivity
//            val intent = Intent(context, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            context.startActivity(intent)
//        }) {
//            Text("Logout")
//        }
//    }
//}
