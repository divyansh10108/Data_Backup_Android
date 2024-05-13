package com.databackup.app


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.databackup.app.ui.theme.DataBackupAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

@Composable
fun MainScreen() {
    val mContext = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "Data Backup App",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                mContext.startActivity(Intent(mContext, DownloadActivity::class.java))
            },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0XFF0F9D58)),
            ) {
                Text("Download", color = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = {
                mContext.startActivity(Intent(mContext, UploadActivity::class.java))
            },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0XFF0F9D58)),
            ) {
                Text("Upload", color = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = {
                mContext.startActivity(Intent(mContext, StatsActivity::class.java))
            },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0XFF0F9D58)),
            ) {
                Text("Download Stats", color = Color.White)
            }
        }
    }
}

