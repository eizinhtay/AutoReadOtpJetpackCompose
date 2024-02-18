package com.example.autoreadotp

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.autoreadotp.ui.theme.AutoReadOtpTheme
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

@Composable
fun GetOtpVerifyScreen() {
    val verificationCode = remember { mutableStateOf("") }
    val shouldStartSMSRetrieval = remember { mutableStateOf(false) }

    val context = LocalContext.current



    //SendSMSCodeFromReceiverToIntent
    val smsReceiverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                // Get SMS message content
                val message =
                    result.data!!.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                message?.takeLast(4)?.let { verificationCode.value = it }

            } else {
                showToastMessage(context, "Otp retrieval failed")
            }
        }
    )


    // use DisposableEffect to register a BroadcastReceiver when our composable enters the composition.
    // We pass context as the key to DisposableEffect,
    // so the callback function will only be called if context changes.
    DisposableEffect(shouldStartSMSRetrieval) {
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)

        val smsVerificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                    val extras = intent.extras
                    val smsRetrieverStatus =
                        extras?.get(SmsRetriever.EXTRA_STATUS) as Status

                    when (smsRetrieverStatus.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            // Get consent intent
                            val consentIntent =
                                extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                            try {
                                // Start activity to show consent dialog to user, activity must be started in
                                // 5 minutes, otherwise you'll receive another TIMEOUT intent
                                smsReceiverLauncher.launch(consentIntent)
                            } catch (e: ActivityNotFoundException) {
                                // Handle the exception ...
                            }
                        }

                        CommonStatusCodes.TIMEOUT -> {
                            // Time out occurred, handle the error.
                        }
                    }
                }
            }
        }

        //Flag for registerReceiver: The receiver cannot receive broadcasts
        // from other Apps. Has the same behavior as marking a statically
        // registered receiver with "exported=false"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    smsVerificationReceiver, intentFilter,
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    smsVerificationReceiver, intentFilter

                )
            }
        }

        //WhenContextDisposeUnregisterReceiver
        onDispose {
            context.unregisterReceiver(smsVerificationReceiver)
            showToastMessage(context, "onDispose")
        }
    }



    LaunchedEffect(shouldStartSMSRetrieval) {
        SmsRetriever.getClient(context).startSmsUserConsent(null)

    }

    GetOtpVerifyUi(verificationCode = verificationCode)
}


@Composable
fun GetOtpVerifyUi(verificationCode: MutableState<String>) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = stringResource(id = R.string.otp_verification),
                modifier = Modifier.padding(
                    16.dp
                ),
                style = MaterialTheme.typography.headlineSmall,

                )
            Text(
                text = stringResource(id = R.string.enter_verification_code),
                modifier = Modifier.padding(
                    16.dp
                ),
                style = MaterialTheme.typography.bodyMedium,

                )

            OtpTextField(
                modifier = Modifier.padding(16.dp),
                otpText = verificationCode.value,
                otpCount = 4,
                onOtpTextChange = { value, _ ->
                    verificationCode.value = value
                },

                )

            Button(
                onClick = { /**/ },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Verify",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun GetOtpVerifyScreenPreview() {
    val verificationCode = remember {
        mutableStateOf("")
    }
    AutoReadOtpTheme {
        GetOtpVerifyUi(verificationCode = verificationCode)
    }
}

@Composable
fun OtpTextField(
    modifier: Modifier = Modifier,
    otpText: String,
    otpCount: Int = 4,
    onOtpTextChange: (String, Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        if (otpText.length > otpCount) {
            throw IllegalArgumentException("Otp text value must not have more than otpCount: $otpCount characters")
        }
    }

    BasicTextField(
        modifier = modifier,
        value = TextFieldValue(otpText, selection = TextRange(otpText.length)),
        onValueChange = {
            if (it.text.length <= otpCount) {
                onOtpTextChange.invoke(it.text, it.text.length == otpCount)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(otpCount) { index ->
                    CharView(
                        index = index,
                        text = otpText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    )
}

@Composable
private fun CharView(
    index: Int,
    text: String
) {
    val isFocused = text.length == index
    val char = when {
        index == text.length -> ""
        index > text.length -> ""
        else -> text[index].toString()
    }
    Text(
        modifier = Modifier
            .width(40.dp)
            .border(
                1.dp, when {
                    isFocused -> Color.DarkGray
                    else -> Color.LightGray
                }, RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        text = char,
        style = MaterialTheme.typography.headlineLarge,
        color = if (isFocused) {
            Color.LightGray
        } else {
            Color.DarkGray
        },
        textAlign = TextAlign.Center
    )
}