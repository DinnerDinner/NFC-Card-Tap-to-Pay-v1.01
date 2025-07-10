package com.example.nfccardtaptopayv101.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.util.*

@Composable
fun SignUpScreen(onSubmit: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Date of Birth Picker ---
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                label = { Text("Date of Birth") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}, // to prevent text input
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pick Date",
                        tint = Color.Gray
                    )
                }
            )

            // Transparent clickable layer on top
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dob = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                try {
                    val json = JSONObject().apply {
                        put("first_name", firstName.trim())
                        put("last_name", lastName.trim())
                        put("email", email.trim())
                        put("phone_number", phoneNumber.trim())
                        put("dob", dob.trim())
                        put("password", password)
                        put("card_uid", "000000") // will be replaced after tap
                    }

                    onSubmit(json.toString())
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid input: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }
    }
}