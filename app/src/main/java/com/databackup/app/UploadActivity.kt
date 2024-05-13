package com.databackup.app

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.databackup.app.ui.theme.DataBackupAppTheme
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import java.util.Calendar

class UploadActivity : ComponentActivity() {
    private val REQUEST_CODE = 1

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestManageAllFilesAccessPermission()
        setContent {
            DataBackupAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    UploadScreen()
                }
            }
        }
    }

    fun requestManageAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission not granted
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


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
        storageRef.putStream(stream).addOnSuccessListener {
                // Get a URL to the uploaded content
                storageRef.downloadUrl.addOnSuccessListener { _ ->
                        downloadUrl = "File uploaded successfully"
                    }
            }.addOnFailureListener {
                // Handle unsuccessful uploads
                downloadUrl = "Failed to upload file"
            }
    }
    return downloadUrl
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun UploadScreen() {
    val context = LocalContext.current
    val contentResolver: ContentResolver = context.contentResolver

    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                uploadFile(it, context)
            }
        }

    val scanAndUploadLauncher = rememberCoroutineScope()
    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ){
        IconButton(onClick = { (context as ComponentActivity).onBackPressed() }) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upload Files",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() } // Accessibility: Mark as heading
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Send, // replace with your actual icon
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AccessibilityButton(
            label = "Select File(s) to Upload",
            onClick = { filePickerLauncher.launch("*/*") },
            icon = Icons.Filled.List
        )

        Spacer(modifier = Modifier.height(16.dp))

        AccessibilityButton(
            label = "Scan and Upload Old Files", onClick = {
                scanAndUploadLauncher.launch {
                    scanAndUploadAllFiles(contentResolver, context)
                }
            }, icon = Icons.Filled.Refresh
        )

    }
}


@RequiresApi(Build.VERSION_CODES.Q)
suspend fun scanAndUploadAllFiles(contentResolver: ContentResolver, context: Context) {
    val uri: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

    cursor?.use { cursor ->
        Log.d("ScanUpload", cursor.count.toString())

        // Calculate date threshold (31 days ago)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -31)
        val thresholdDate = calendar.timeInMillis / 1000

        if (cursor.moveToFirst()) {
            val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val displayNameColumnIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val dateModifiedColumnIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)

            do {
                val id = cursor.getLong(idColumnIndex)
                val displayName = cursor.getString(displayNameColumnIndex)
                val dateModified = cursor.getLong(dateModifiedColumnIndex)


                if (dateModified <= thresholdDate) {
                    Log.d("ScanUpload", "File: $displayName, ID: $id, Modified: $dateModified")
                    Log.d("ScanUpload2", "$thresholdDate")
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    uploadFile(contentUri, context)
                }
            } while (cursor.moveToNext()) // Move to the next row
        }
    }

    cursor?.close()
}

