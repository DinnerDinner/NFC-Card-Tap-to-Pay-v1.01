@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nfccardtaptopayv101.ui

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
import com.example.nfccardtaptopayv101.TaptoTransferMachine
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

// --- Logic + State Holder ---
@Composable
fun LoggedInHomeScreen(
    userData: String,
    startOnProfile: Boolean = false
) {
    var selectedScreen by remember { mutableStateOf(if (startOnProfile) "Profile" else "Profile") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Function to handle logout
    fun logout() {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        context.startActivity(Intent(context, LoginActivity::class.java))
    }

    // Pass everything to pure UI
    LoggedInHomeScreenUI(
        drawerState = drawerState,
        selectedScreen = selectedScreen,
        onScreenSelected = { screen ->
            selectedScreen = screen
            scope.launch { drawerState.close() }
        },
        onLogout = { logout() }
    )
}

// --- Pure UI / Design only, no logic ---
@Composable
fun LoggedInHomeScreenUI(
    drawerState: DrawerState,
    selectedScreen: String,
    onScreenSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        "Profile" to Icons.Default.AccountCircle,
        "Tap to Transfer Machine" to Icons.Default.PointOfSale,
        "Emulated Cards' Wallet" to Icons.Default.CreditCard,
        "mPOS System" to Icons.Default.List
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(250.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    " ",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()

                menuItems.forEach { (label, icon) ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = selectedScreen == label,
                        onClick = { onScreenSelected(label) },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider()

                NavigationDrawerItem(
                    label = { Text("Log Out") },
                    selected = false,
                    onClick = onLogout,
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
                    "Tap to Transfer Machine" -> LaunchTransferActivity()
                    "Emulated Cards' Wallet" -> CardsScreen()
                    "mPOS System" -> ProductDatabaseScreen()
                }
            }
        }
    }
}

@Composable
fun LaunchTransferActivity() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.startActivity(Intent(context, TaptoTransferMachine::class.java))
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Opening Merchant Tap Machine...")
    }
}
