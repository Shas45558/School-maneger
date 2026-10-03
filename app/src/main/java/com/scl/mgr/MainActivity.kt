package com.scl.mgr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import com.scl.mgr.data.AppDatabase
import com.scl.mgr.data.BackupManager
import com.scl.mgr.data.GoogleDriveSyncManager
import com.scl.mgr.data.SchoolRepository
import com.scl.mgr.ui.SchoolManagerApp
import com.scl.mgr.ui.theme.SchoolManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.get(this)
        val repository = SchoolRepository(database)
        val syncManager = GoogleDriveSyncManager(this, BackupManager(this, database))

        setContent {
            SchoolManagerTheme {
                Surface {
                    SchoolManagerApp(repository, syncManager)
                }
            }
        }
    }
}
