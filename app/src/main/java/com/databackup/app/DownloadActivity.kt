package com.databackup.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.databackup.app.ui.theme.DataBackupAppTheme
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.component1
import com.google.firebase.storage.component2
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private lateinit var sensorManager: SensorManager
private var accelerometer: Sensor? = null
private var accelerometerListener: SensorEventListener? = null
private var lastUpdate: Long = 0
private var lastX: Float = 0f
private var lastY: Float = 0f
private var lastZ: Float = 0f
private val SHAKE_THRESHOLD = 800

class DownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        registerAccelerometerListener()
        val database: Database = Room.databaseBuilder(applicationContext, Database::class.java, "data").build()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            DataBackupAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DownloadScreen(database, coroutineScope, snackbarHostState)
                }
            }
        }


    }
    override fun onResume() {
        super.onResume()
        // Re-register the sensor listener when the app is resumed
        registerAccelerometerListener()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener to save battery when the app is in the background
        accelerometerListener?.let { sensorManager.unregisterListener(it) }
    }

    private fun registerAccelerometerListener() {
        accelerometerListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for accelerometer
            }

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                if (isDeviceMoving(x, y, z)) {
                    showToast("Device movement detected. Download paused.")
                }
            }
        }

        // Register the listener with the sensor manager
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                accelerometerListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun showToast(message: String) {
//        runOnUiThread {
//            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
//        }
    }

    private fun isDeviceMoving(x: Float, y: Float, z: Float): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastUpdate

        if (timeDifference > 100) { // Check every 100 milliseconds
            val diffTime = currentTime - lastUpdate
            lastUpdate = currentTime

            val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

            if (speed > SHAKE_THRESHOLD) {
                return true
            }

            lastX = x
            lastY = y
            lastZ = z
        }
        return false
    }

}

@Composable
fun DownloadScreen(database: Database, coroutineScope: CoroutineScope, snackbarHostState: SnackbarHostState) {
    val storage = Firebase.storage
    val listRef = storage.reference
    var listItems by remember { mutableStateOf(listOf<String>()) }
    var id by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    // Recursive Function to iterate over all files in the database
    fun listFiles(ref: StorageReference) {
        ref.listAll()
            .addOnSuccessListener { (items, prefixes) ->
                listItems += items.map { it.name }
                prefixes.forEach { listFiles(it) }
            }
            .addOnFailureListener {
                listItems = listOf("Failed to list files")
            }
    }

    listFiles(listRef)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Download Files",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(listItems) { item ->
                Button(onClick = {
                    // Check device movement before allowing download
                    if (!isDeviceMoving(0f, 0f, 0f)) {
                        // Perform download action
                        downloadFile(item, database, snackbarHostState,coroutineScope )
                    } else {
                        showMessage("Device movement detected. Download paused.", coroutineScope, snackbarHostState)
                    }
                }) {
                    Text(text = item)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }


        }
        Text(text = id)
    }
}
private fun isDeviceMoving(x: Float, y: Float, z: Float): Boolean {
    // This function checks if the device is moving excessively based on accelerometer data
    // Adjust the threshold and fine-tune the logic as per your requirements
    val currentTime = System.currentTimeMillis()
    val timeDifference = currentTime - lastUpdate

    if (timeDifference > 100) { // Check every 100 milliseconds
        val diffTime = currentTime - lastUpdate
        lastUpdate = currentTime

        val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

        if (speed > SHAKE_THRESHOLD) {
            // Device is moving too much
            return true
        }

        lastX = x
        lastY = y
        lastZ = z
    }
    return false
}
fun downloadFile(url: String, database: Database, snackbarHostState: SnackbarHostState,coroutineScope: CoroutineScope) {
    var result = ""
    val cloudStorage = Firebase.storage.reference
    val storageRef = cloudStorage.child("/$url")


    val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadPath, url)
    val fileName = url.split("/").last()

    storageRef.getFile(file)
        .addOnSuccessListener {
            // File has been downloaded and saved to the Downloads folder
            updateDatabase(database, fileName)
            // Show snackbar message using coroutine
            coroutineScope.launch {
                showMessage("File downloaded successfully to ${file.absolutePath}",coroutineScope,snackbarHostState)
            }
        }
        .addOnFailureListener {
            // Show snackbar message using coroutine
            coroutineScope.launch {
                showMessage("Failed to download file", coroutineScope,snackbarHostState)
            }
        }
}


fun showMessage(message: String,coroutineScope:CoroutineScope,snackbarHostState: SnackbarHostState) {
    coroutineScope.launch {
        snackbarHostState.showSnackbar(message)
    }
}
@OptIn(DelicateCoroutinesApi::class)
fun updateDatabase(database: Database, fileName: String) {
    GlobalScope.launch {
        var count = database.dao().get(fileName)
        if (count != null) {
            count += 1
            database.dao().update(fileName, count)
        } else {
            database.dao().insert(Data(fileName = fileName, count = 1))
        }
        Log.d("tag", count.toString())
    }
}