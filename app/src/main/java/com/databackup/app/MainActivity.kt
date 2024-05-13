package com.databackup.app

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.databackup.app.ui.theme.DataBackupAppTheme
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    var isTextToSpeechOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this) // Initialize TextToSpeech
        setContent {
            DataBackupAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle language not supported
            }
        } else {
            // Handle TTS initialization failure
        }
    }

    fun speakAndNavigate(text: String, intent: Intent) {
        if (isTextToSpeechOn) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        startActivity(intent)
    }
}

@Composable
fun MainScreen() {
    val mContext = LocalContext.current
    val activity = (mContext as? MainActivity)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() } // Accessibility: Mark as heading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccessibilityButton(
                label = "Download",
                onClick = {
                    val intent = Intent(mContext, DownloadActivity::class.java)
                    activity?.speakAndNavigate("Download", intent)
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            AccessibilityButton(
                label = "Upload",
                onClick = {
                    val intent = Intent(mContext, UploadActivity::class.java)
                    activity?.speakAndNavigate("Upload", intent)
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            AccessibilityButton(
                label = "Download Stats",
                onClick = {
                    val intent = Intent(mContext, StatsActivity::class.java)
                    activity?.speakAndNavigate("Download Stats", intent)
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = activity?.isTextToSpeechOn ?: false,
                onCheckedChange = { activity?.toggleTextToSpeech(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0XFF0F9D58),
                    checkedTrackColor = Color(0XFFA2E8A6),
                    uncheckedThumbColor = Color(0XFFCCCCCC),
                    uncheckedTrackColor = Color(0XFFEEEEEE)
                )
            )
        }
    }
}

@Composable
fun AccessibilityButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0XFF0F9D58)),
    ) {
        Text(label, color = Color.White)
    }
}

fun MainActivity.toggleTextToSpeech(isOn: Boolean) {
    isTextToSpeechOn = isOn
}
