package com.example.nfccardtaptopayv101

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nfccardtaptopayv101.ui.theme.NFCCardTapToPayV101Theme
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.widget.Toast
import android.nfc.tech.Ndef          //  ← add this
import android.nfc.tech.IsoDep

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

    }


    override fun onResume() {
        super.onResume()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        )
        val techList = arrayOf(
            arrayOf(android.nfc.tech.IsoDep::class.java.name),
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techList)
    }


    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val action = intent?.action

        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED
            ) {

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: "N/A"

            runOnUiThread {
                Toast.makeText(
                    this,
                    "UID: $uid",
                    Toast.LENGTH_SHORT
                ).show()
            }


        }
    }
}