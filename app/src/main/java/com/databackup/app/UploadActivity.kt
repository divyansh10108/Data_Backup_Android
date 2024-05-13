package com.databackup.app
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.databackup.app.ui.theme.DataBackupAppTheme
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class UploadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DataBackupAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UploadScreen()
                }
            }
        }
    }
}

//
//@Composable
//fun UploadScreen() {
//    val context = LocalContext.current
//
//    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
//        uri?.let {
//            uploadFile(it, context)
//        }
//    }
//
//    Column(modifier = Modifier.padding(16.dp)) {
//        Button(onClick = { filePickerLauncher.launch("*/*") }) {
//            Text("Select File(s) to Upload")
//        }
//
//        Button(onClick = {
//            scanAndUploadLauncher.launch {
//                scanAndUploadOldFiles(context)
//            }
//        }) {
//            Text("Scan and Upload Old Files")
//        }
//
//
//    }
//}




@SuppressLint("Range")
fun uploadFile(uri: Uri, context: Context): String {
    var downloadUrl = ""
    val cloudStorage = Firebase.storage.reference

    // Get the file's original name
    val cursor = context.contentResolver.query(uri, null, null, null, null, null)
    var displayName = ""
    cursor.use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }

    val storageRef = cloudStorage.child(displayName)

    val inputStream = context.contentResolver.openInputStream(uri)
    inputStream?.let { stream ->
        storageRef.putStream(stream)
            .addOnSuccessListener {
                // Get a URL to the uploaded content
                storageRef.downloadUrl
                    .addOnSuccessListener { _ ->
                        downloadUrl = "File uploaded successfully"
                    }
            }
            .addOnFailureListener {
                // Handle unsuccessful uploads
                downloadUrl = "Failed to upload file"
            }
    }
    return downloadUrl
}

@Composable
fun UploadScreen() {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadFile(it, context)
        }
    }

    val scanAndUploadLauncher = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { filePickerLauncher.launch("*/*") }) {
            Text("Select File(s) to Upload")
        }

        Button(onClick = {
            scanAndUploadLauncher.launch {
                scanAndUploadAllFiles(context)
            }
        }) {
            Text("Scan and Upload Old Files")
        }
    }
}

@SuppressLint("NewApi")
fun scanAndUploadAllFiles(context: Context) {
    val projection = arrayOf(
        MediaStore.Downloads._ID
    )

    context.contentResolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                id
            )

            uploadFile(contentUri, context)
        }
    }
}
