package com.example.nfccardtaptopayv101
import android.content.Context

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nfccardtaptopayv101.ui.LoggedInHomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (userIsLoggedIn()) {
            val userData = getUserData() // Implement how you get stored user data (e.g., SharedPreferences or DB)

            setContent {
                LoggedInHomeScreen(userData)
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()  // Prevent back to this activity
        }
    }

    private fun userIsLoggedIn(): Boolean {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.contains("user_data")
    }

    private fun getUserData(): String {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_data", "{}") ?: "{}"
    }

}










//package com.example.nfccardtaptopayv101
//
//import android.content.Intent
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent { HomeScreen() }
//    }
//
//    @Composable
//    fun HomeScreen() {
//        Surface(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(24.dp),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text("🏠  Home", style = MaterialTheme.typography.headlineMedium)
//                Spacer(Modifier.height(32.dp))
//                Button(onClick = {
//                    startActivity(Intent(this@MainActivity, SignUpActivity::class.java))
//                }) {
//                    Text("Go to Sign‑Up")
//                }
//            }
//        }
//    }
//}
//
