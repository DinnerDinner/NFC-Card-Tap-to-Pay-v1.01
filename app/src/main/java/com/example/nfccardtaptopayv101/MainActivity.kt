package com.example.nfccardtaptopayv101

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.example.nfccardtaptopayv101.ui.LoggedInHomeScreen
import com.example.nfccardtaptopayv101.ui.theme.NFCCardTapToPayV101Theme

class MainActivity : ComponentActivity() {

    // Holds last tapped card UID (if you still want to keep this for future use)
    private val tappedUid = mutableStateOf<String?>(null)

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
        return prefs.contains("user_data")
    }

    private fun getUserData(): String {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_data", "{}") ?: "{}"
    }
}
