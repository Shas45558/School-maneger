package com.scl.mgr.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn

/** Periodically syncs the shared School Manager database when Auto Sync is enabled. */
class AutoSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AUTO_SYNC, false)) return Result.success()

        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            ?: return Result.success()

        return try {
            val db = AppDatabase.get(applicationContext)
            val manager = GoogleDriveSyncManager(applicationContext, BackupManager(applicationContext, db))
            manager.sync(account)
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
            Result.success()
        } catch (_: GoogleDriveSyncManager.DriveAuthorizationRequiredException) {
            // The next manual sync can launch Google's authorization UI.
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val PREFS = "google_drive_sync"
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_LAST_SYNC = "last_sync"
    }
}
