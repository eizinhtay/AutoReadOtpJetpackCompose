package com.example.autoreadotp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.autoreadotp.ui.theme.AutoReadOtpTheme

class GetOtpVerifyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoReadOtpTheme {
                GetOtpVerifyScreen()
            }
        }
    }

}

fun showToastMessage(context: Context, s: String) {
    Toast.makeText(context, s, Toast.LENGTH_LONG).show()

}
