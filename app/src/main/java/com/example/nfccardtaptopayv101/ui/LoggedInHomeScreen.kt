package com.example.nfccardtaptopayv101.ui

import android.content.Context
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nfccardtaptopayv101.LoginActivity

@Composable
fun LoggedInHomeScreen(userData: String) {
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome back!\nUser data:\n$userData")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            // Clear saved user data (logout)
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            // Navigate back to LoginActivity
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }) {
            Text("Logout")
        }
    }
}
