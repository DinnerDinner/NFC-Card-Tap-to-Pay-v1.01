
package com.example.nfccardtaptopayv101.ui.screens
import com.example.nfccardtaptopayv101.ui.navigation.ScreensNavGraph
import org.json.JSONObject

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
import com.example.nfccardtaptopayv101.activities.LoginActivity
import com.example.nfccardtaptopayv101.activities.TaptoTransferMachine
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment


@Composable
fun LoggedInHomeScreen(
    userData: String,  // This should contain user_id
    startOnProfile: Boolean = false
) {
    var selectedScreen by remember { mutableStateOf(if (startOnProfile) "Profile" else "Profile") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Extract user_id from userData (adjust based on your userData format)
    val userId = extractUserIdFromUserData(userData)

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
        userId = userId,  // Pass userId to UI
        onScreenSelected = { screen ->
            selectedScreen = screen
            scope.launch { drawerState.close() }
        },
        onLogout = { logout() }
    )
}

// 3. UPDATE the LoggedInHomeScreenUI function signature:
@Composable
fun LoggedInHomeScreenUI(
    drawerState: DrawerState,
    selectedScreen: String,
    userId: String,  // Add userId parameter
    onScreenSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        "Profile" to Icons.Default.AccountCircle,
        "Tap to Transfer Machine" to Icons.Default.PointOfSale,
        "Emulated Cards' Wallet" to Icons.Default.CreditCard,  // This is our payment screens
        "mPOS System" to Icons.Default.List
    )

    val docUrl = "https://docs.google.com/document/d/1QQD2LfHxB9PL7WVrrD_x2Aro4eLrwzwWUdVPxt8dofc/edit?usp=sharing"
    @OptIn(ExperimentalMaterial3Api::class)
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

                val context = LocalContext.current

                NavigationDrawerItem(
                    label = { Text("Latest Release Docs") },
                    selected = false,
                    onClick = { openPdfInBrowser(context, docUrl) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Docs") },
                    modifier = Modifier.padding(16.dp)
                )

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
                    "Emulated Cards' Wallet" -> CardsScreen(userId = userId)  // Pass userId here
                    "mPOS System" -> BusinessUnifiedScreen()
                }
            }
        }
    }
}

// 4. UPDATE the CardsScreen function:
@Composable
fun CardsScreen(userId: String) {
    ScreensNavGraph(userId = userId)
}

// 5. ADD this helper function to extract user ID:
fun extractUserIdFromUserData(userData: String): String {
    // Adjust this based on how your userData is structured
    // If userData is JSON, parse it:
    return try {
        val json = JSONObject(userData)
        json.getString("user_id")
    } catch (e: Exception) {
        // If userData is just the user_id as string:
        userData
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

fun openPdfInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = android.net.Uri.parse(url)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
