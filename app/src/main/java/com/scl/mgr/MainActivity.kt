package com.scl.mgr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.scl.mgr.data.AppDatabase
import com.scl.mgr.data.BackupManager
import com.scl.mgr.data.GoogleDriveSyncManager
import com.scl.mgr.data.SchoolRepository
import com.scl.mgr.ui.theme.SchoolManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.get(this)
        val repository = SchoolRepository(database, this)
        val syncManager = GoogleDriveSyncManager(this, BackupManager(this, database))

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<com.scl.mgr.data.AutoSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(
            "school_manager_auto_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        workManager.enqueueUniqueWork(
            "school_manager_startup_sync",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<com.scl.mgr.data.AutoSyncWorker>()
                .setConstraints(constraints)
                .build()
        )

        setContent {
            SchoolManagerTheme {
                Surface {
                    SchoolManagerApp(repository, syncManager)
                }
            }
        }
    }
}
