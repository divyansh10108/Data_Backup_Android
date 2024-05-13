package com.databackup.app

// DataStore setup
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.databackup.app.ui.theme.DataBackupAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "top_files")

class TopFilesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val TOP_FILES_KEY = stringPreferencesKey("top_files")
    }

    suspend fun saveTopFiles(files: List<Data>) {
        dataStore.edit { preferences ->
            val filesString = files.joinToString(",") { it.fileName }
            preferences[TOP_FILES_KEY] = filesString

        }
        Log.d("DataStore", "Top files cached successfully")
    }

    fun getTopFiles(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val filesString = preferences[TOP_FILES_KEY] ?: ""
            filesString.split(",")
        }
    }
}

class StatsActivity : ComponentActivity() {
    private lateinit var topFilesRepository: TopFilesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database: Database =
            Room.databaseBuilder(applicationContext, Database::class.java, "data").build()
        enableEdgeToEdge()

        // Initialize DataStore repository
        topFilesRepository = TopFilesRepository(dataStore)

        setContent {
            DataBackupAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StatsScreen(database, topFilesRepository)
                }
            }
        }
    }
}

@Composable
fun StatsScreen(database: Database, topFilesRepository: TopFilesRepository) {
    var topFiles by remember { mutableStateOf<List<Data>>(emptyList()) }

    LaunchedEffect(key1 = true) {
        topFiles = database.dao().getTopFiles(5).distinctBy { it.fileName }
        topFilesRepository.saveTopFiles(topFiles) // Cache top files
    }

    val cachedTopFiles by topFilesRepository.getTopFiles().collectAsState(emptyList())
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            IconButton(onClick = { (context as ComponentActivity).onBackPressed() }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Top 5 Files:", fontSize = 20.sp)
        cachedTopFiles.forEach { fileName ->
            Text(
                "$fileName - Count: ${topFiles.find { it.fileName == fileName }?.count ?: 0}",
                fontSize = 16.sp
            )
        }
    }
}