package com.scl.mgr.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object SyncScheduler {
    private const val WORK_NAME = "school_manager_change_sync"

    fun scheduleChangeSync(context: Context) {
        val prefs = context.getSharedPreferences(AutoSyncWorker.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AutoSyncWorker.KEY_AUTO_SYNC, false)) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AutoSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
