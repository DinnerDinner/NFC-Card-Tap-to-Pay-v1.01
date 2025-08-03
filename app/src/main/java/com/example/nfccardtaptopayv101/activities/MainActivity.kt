package com.example.nfccardtaptopayv101.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nfccardtaptopayv101.ui.screens.LoggedInHomeScreen
import com.example.nfccardtaptopayv101.ui.theme.NFCCardTapToPayV101Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (userIsLoggedIn()) {
            val userData = getUserData()
            val startOnProfile = intent.getBooleanExtra("goToProfile", false)

            setContent {
                NFCCardTapToPayV101Theme {

                    LoggedInHomeScreen(userData, startOnProfile = startOnProfile)
                }
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun userIsLoggedIn(): Boolean {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.contains("user_data") && prefs.contains("user_id")
    }

    private fun getUserData(): String {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_data", "{}") ?: "{}"
    }
}
