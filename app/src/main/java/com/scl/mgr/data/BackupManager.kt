package com.scl.mgr.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Creates a consistent SQLite snapshot and safely merges another snapshot into Room. */
class BackupManager(private val context: Context, private val db: AppDatabase) {

    suspend fun createSnapshot(): File = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "school_manager_backup.db")
        if (out.exists()) out.delete()
        // wal_checkpoint returns a result set, so execute it as a query.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            while (cursor.moveToNext()) { /* consume result */ }
        }
        val escaped = out.absolutePath.replace("'", "''")
        db.openHelper.writableDatabase.execSQL("VACUUM INTO '$escaped'")
        out
    }

    /**
     * Merge remote data into the current database without deleting local data.
     * Students are matched by Class + Section + Roll. For conflicts, local student
     * details win; attendance records are unioned by student + date.
     */
    suspend fun mergeSnapshot(snapshot: File): MergeResult = withContext(Dispatchers.IO) {
        val tempName = "remote_merge_${System.currentTimeMillis()}.db"
        val tempFile = context.getDatabasePath(tempName)
        tempFile.parentFile?.mkdirs()
        Files.copy(snapshot.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val remoteDb = Room.databaseBuilder(context, AppDatabase::class.java, tempName).build()
        try {
            val remoteStudents = remoteDb.studentDao().getAllOnce()
            val remoteAttendance = remoteDb.attendanceDao().getAllOnce()
            val localByKey = db.studentDao().getAllOnce().associateBy { key(it) }.toMutableMap()
            val idMap = mutableMapOf<Long, Long>()
            var addedStudents = 0
            var addedAttendance = 0

            for (remoteStudent in remoteStudents) {
                val existing = localByKey[key(remoteStudent)]
                if (existing != null) {
                    idMap[remoteStudent.id] = existing.id
                } else {
                    val newId = db.studentDao().insert(
                        remoteStudent.copy(id = 0)
                    )
                    val inserted = db.studentDao().getById(newId)!!
                    localByKey[key(inserted)] = inserted
                    idMap[remoteStudent.id] = newId
                    addedStudents++
                }
            }

            val existingAttendance = db.attendanceDao().getAllOnce()
                .associateBy { it.studentId to it.date }
            for (record in remoteAttendance) {
                val localStudentId = idMap[record.studentId] ?: continue
                if ((localStudentId to record.date) !in existingAttendance) {
                    db.attendanceDao().upsert(record.copy(id = 0, studentId = localStudentId))
                    addedAttendance++
                }
            }
            MergeResult(addedStudents, addedAttendance)
        } finally {
            remoteDb.close()
            tempFile.delete()
            File(tempFile.absolutePath + "-wal").delete()
            File(tempFile.absolutePath + "-shm").delete()
        }
    }

    private fun key(s: Student): String =
        "${s.className.trim().uppercase()}|${s.section.trim().uppercase()}|${s.studentId.trim()}"

    data class MergeResult(val studentsAdded: Int, val attendanceAdded: Int)
}
