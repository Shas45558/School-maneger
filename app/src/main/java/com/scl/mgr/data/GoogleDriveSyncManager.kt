package com.scl.mgr.data

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class GoogleDriveSyncManager(
    private val context: Context,
    private val backupManager: BackupManager
) {
    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive"
        const val SHARED_FOLDER_ID = "1dk-R0qwoQk_97v0T_jSco5FRko4kvZmi"
        const val BACKUP_NAME = "school_manager.db"
        
        private const val DRIVE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }

    private val client = OkHttpClient()

    /** Syncs the plain SQLite backup with Google Drive app-data. */
    suspend fun sync(account: GoogleSignInAccount): SyncResult =
        withContext(Dispatchers.IO) {
            val token = accessToken(account)
            val remote = findBackup(token)
            val localSnapshot = backupManager.createSnapshot()
            try {
                if (remote == null) {
                    val id = createFile(token)
                    uploadContent(token, id, localSnapshot)
                    return@withContext SyncResult(
                        connectedEmail = account.email ?: account.account?.name.orEmpty(),
                        restored = false, studentsAdded = 0, attendanceAdded = 0, uploaded = true
                    )
                }

                val remoteDownloaded = File(context.cacheDir, "school_manager_remote.db")
                try {
                    downloadContent(token, remote.id, remoteDownloaded)
                    val merged = backupManager.mergeSnapshot(remoteDownloaded)
                    val mergedSnapshot = backupManager.createSnapshot()
                    try {
                        uploadContent(token, remote.id, mergedSnapshot)
                    } finally {
                        mergedSnapshot.delete()
                    }
                    return@withContext SyncResult(
                        connectedEmail = account.email ?: account.account?.name.orEmpty(),
                        restored = true,
                        studentsAdded = merged.studentsAdded,
                        attendanceAdded = merged.attendanceAdded,
                        monthlyExamsAdded = merged.monthlyExamsAdded,
                        uploaded = true
                    )
                } finally {
                    remoteDownloaded.delete()
                }
            } finally {
                localSnapshot.delete()
            }
        }

    private fun accessToken(account: GoogleSignInAccount): String {
        val accountObj: Account = account.account ?: error("Google account is unavailable")
        return try {
            GoogleAuthUtil.getToken(context, accountObj, "oauth2:$DRIVE_FILE_SCOPE")
        } catch (e: UserRecoverableAuthException) {
            val recoveryIntent = e.intent
                ?: error("Google authentication recovery intent is unavailable")
            throw DriveAuthorizationRequiredException(recoveryIntent)
        }
    }

    private fun authorizedRequest(token: String, url: String): Request.Builder =
        Request.Builder().url(url).header("Authorization", "Bearer $token")

    private fun findBackup(token: String): BackupRef? {
        val query = "'$SHARED_FOLDER_ID' in parents and name = '$BACKUP_NAME' and trashed = false"
        val url = "$DRIVE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
            "&spaces=drive&fields=files(id,name,modifiedTime)"
        client.newCall(authorizedRequest(token, url).get().build()).execute().use { response ->
            if (!response.isSuccessful) error("Google Drive folder search failed (${response.code})")
            val files = JSONObject(response.body?.string().orEmpty()).optJSONArray("files")
            return if (files != null && files.length() > 0) {
                BackupRef(files.getJSONObject(0).getString("id"))
            } else null
        }
    }

    private fun createFile(token: String): String {
        val metadata = JSONObject().apply {
            put("name", BACKUP_NAME)
            put("parents", org.json.JSONArray().put(SHARED_FOLDER_ID))
        }.toString()
        val body = metadata.toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "$DRIVE/files?fields=id"
        client.newCall(authorizedRequest(token, url).post(body).build()).execute().use { response ->
            if (!response.isSuccessful) error("Google Drive file creation failed (${response.code})")
            return JSONObject(response.body?.string().orEmpty()).getString("id")
        }
    }

    private fun uploadContent(token: String, fileId: String, file: File) {
        val body = file.asRequestBody("application/octet-stream".toMediaType())
        val url = "$UPLOAD/files/$fileId?uploadType=media"
        client.newCall(authorizedRequest(token, url).patch(body).build()).execute().use { response ->
            if (!response.isSuccessful) error("Google Drive upload failed (${response.code})")
        }
    }

    private fun downloadContent(token: String, fileId: String, destination: File) {
        val url = "$DRIVE/files/$fileId?alt=media"
        client.newCall(authorizedRequest(token, url).get().build()).execute().use { response ->
            if (!response.isSuccessful) error("Google Drive download failed (${response.code})")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: error("Google Drive returned an empty backup")
        }
    }

    private data class BackupRef(val id: String)

    data class SyncResult(
        val connectedEmail: String,
        val restored: Boolean,
        val studentsAdded: Int,
        val attendanceAdded: Int,
        val monthlyExamsAdded: Int = 0,
        val uploaded: Boolean
    )

    class DriveAuthorizationRequiredException(val recoveryIntent: android.content.Intent) : Exception()
}
