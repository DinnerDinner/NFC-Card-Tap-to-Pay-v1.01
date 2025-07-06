package com.example.nfccardtaptopayv101

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

class LoggedInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userJsonString = intent.getStringExtra("user_json") ?: "{}"
        val userData = JSONObject(userJsonString)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    UserInfoScreen(userData)
                }
            }
        }
    }
}

@Composable
fun UserInfoScreen(userData: JSONObject) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Your Information:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        InfoRow(label = "First Name", value = userData.optString("first_name", "N/A"))
        InfoRow(label = "Last Name", value = userData.optString("last_name", "N/A"))
        InfoRow(label = "Email", value = userData.optString("email", "N/A"))
        InfoRow(label = "Phone Number", value = userData.optString("phone_number", "N/A"))
        InfoRow(label = "Date of Birth", value = userData.optString("dob", "N/A"))
        InfoRow(label = "Card UID", value = userData.optString("card_uid", "N/A"))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp
        )
    }
}
