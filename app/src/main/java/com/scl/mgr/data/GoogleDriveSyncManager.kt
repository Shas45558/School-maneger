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
        const val APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val BACKUP_NAME = "school_manager.db.enc"
        private const val LEGACY_BACKUP_NAME = "school_manager.db"
        private const val DRIVE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }

    private val client = OkHttpClient()

    /**
     * Syncs with Google Drive. The database is encrypted with AES-256-GCM before
     * upload. The same password is required to restore the backup on another phone.
     */
    suspend fun sync(account: GoogleSignInAccount, backupPassword: CharArray): SyncResult =
        withContext(Dispatchers.IO) {
            require(backupPassword.size >= 8) { "Backup password must be at least 8 characters." }
            val password = backupPassword

            try {
                val token = accessToken(account)
                val remote = findBackup(token)
                val localSnapshot = backupManager.createSnapshot()
                val encryptedLocal = File(context.cacheDir, "school_manager_backup.db.enc")

                try {
                    BackupCrypto.encrypt(localSnapshot, encryptedLocal, password)

                    if (remote == null) {
                        val id = createFile(token)
                        uploadContent(token, id, encryptedLocal)
                        return@withContext SyncResult(
                            connectedEmail = account.email ?: account.account?.name.orEmpty(),
                            restored = false,
                            studentsAdded = 0,
                            attendanceAdded = 0,
                            uploaded = true
                        )
                    }

                    val remoteDownloaded = File(context.cacheDir, "school_manager_remote.bin")
                    val remoteDecrypted = File(context.cacheDir, "school_manager_remote.db")
                    try {
                        downloadContent(token, remote.id, remoteDownloaded)

                        // Older v1.3 backups were plaintext. Read them once and replace
                        // them with an encrypted backup after the merge.
                        val snapshotForMerge = if (BackupCrypto.isEncrypted(remoteDownloaded)) {
                            remoteDecrypted.delete()
                            BackupCrypto.decrypt(remoteDownloaded, remoteDecrypted, password)
                            remoteDecrypted
                        } else {
                            remoteDownloaded
                        }

                        val merged = backupManager.mergeSnapshot(snapshotForMerge)
                        val mergedSnapshot = backupManager.createSnapshot()
                        try {
                            BackupCrypto.encrypt(mergedSnapshot, encryptedLocal, password)
                            uploadContent(token, remote.id, encryptedLocal)
                        } finally {
                            mergedSnapshot.delete()
                        }

                        return@withContext SyncResult(
                            connectedEmail = account.email ?: account.account?.name.orEmpty(),
                            restored = true,
                            studentsAdded = merged.studentsAdded,
                            attendanceAdded = merged.attendanceAdded,
                            uploaded = true
                        )
                    } finally {
                        remoteDownloaded.delete()
                        remoteDecrypted.delete()
                    }
                } finally {
                    encryptedLocal.delete()
                    localSnapshot.delete()
                }
            } finally {
                password.fill('\u0000')
            }
        }

    private fun accessToken(account: GoogleSignInAccount): String {
        val accountObj: Account = account.account ?: error("Google account is unavailable")
        return try {
            GoogleAuthUtil.getToken(context, accountObj, "oauth2:$APP_DATA_SCOPE")
        } catch (e: UserRecoverableAuthException) {
            val recoveryIntent = e.intent
                ?: error("Google authentication recovery intent is unavailable")
            throw DriveAuthorizationRequiredException(recoveryIntent)
        }
    }

    private fun authorizedRequest(token: String, url: String): Request.Builder =
        Request.Builder().url(url).header("Authorization", "Bearer $token")

    private fun findBackup(token: String): BackupRef? {
        fun findByName(name: String): BackupRef? {
            val query = "'appDataFolder' in parents and name = '$name' and trashed = false"
            val url = "$DRIVE/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&spaces=appDataFolder&fields=files(id,name,modifiedTime)"
            client.newCall(authorizedRequest(token, url).get().build()).execute().use { response ->
                if (!response.isSuccessful) error("Google Drive list failed (${response.code})")
                val files = JSONObject(response.body?.string().orEmpty()).optJSONArray("files")
                return if (files != null && files.length() > 0) {
                    BackupRef(files.getJSONObject(0).getString("id"))
                } else null
            }
        }
        return findByName(BACKUP_NAME) ?: findByName(LEGACY_BACKUP_NAME)
    }

    private fun createFile(token: String): String {
        val metadata = JSONObject().apply {
            put("name", BACKUP_NAME)
            put("parents", org.json.JSONArray().put("appDataFolder"))
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
        val uploaded: Boolean
    )

    class DriveAuthorizationRequiredException(val recoveryIntent: android.content.Intent) : Exception()
}
