package com.scl.mgr.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Creates consistent SQLite snapshots and safely merges remote snapshots into Room. */
class BackupManager(private val context: Context, private val db: AppDatabase) {

    suspend fun createSnapshot(): File = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "school_manager_backup.db")
        if (out.exists()) out.delete()

        // PRAGMA wal_checkpoint returns rows, so query() is correct here.
        db.openHelper.writableDatabase.query(
            SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")
        ).use { cursor ->
            while (cursor.moveToNext()) Unit
        }

        // VACUUM INTO is a SQL command, not a SELECT query. execSQL avoids
        // the previous "Queries can be performed ... only" SQLITE_OK error.
        val escaped = out.absolutePath.replace("'", "''")
        db.openHelper.writableDatabase.execSQL("VACUUM INTO '$escaped'")

        if (!out.isFile || out.length() == 0L) {
            error("SQLite backup snapshot was not created")
        }
        out
    }

    /**
     * Merge remote data into the current database without deleting local data.
     * Students are matched by Class + Section + Roll. Existing local student
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
            val remoteMonthlyExams = remoteDb.monthlyExamDao().getAllOnce()
            val remoteExamMarks = remoteDb.examMarkDao().getAllOnce()
            val localByKey = db.studentDao().getAllOnce().associateBy { key(it) }.toMutableMap()
            val idMap = mutableMapOf<Long, Long>()
            var addedStudents = 0
            var addedAttendance = 0
            var addedMonthlyExams = 0
            var addedExamMarks = 0

            for (remoteStudent in remoteStudents) {
                val existing = localByKey[key(remoteStudent)]
                if (existing != null) {
                    idMap[remoteStudent.id] = existing.id
                } else {
                    val newId = db.studentDao().insert(remoteStudent.copy(id = 0))
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
            // Merge monthly exam marks by student + month. When both devices have
            // a record, keep every locally-entered subject mark and fill any
            // missing subject from Drive. This prevents pressing Enter on one
            // subject from overwriting marks saved by another device.
            val localExams = db.monthlyExamDao().getAllOnce()
                .associateBy { it.studentId to it.yearMonth }
            for (remoteExam in remoteMonthlyExams) {
                val localStudentId = idMap[remoteExam.studentId] ?: continue
                val key = localStudentId to remoteExam.yearMonth
                val localExam = localExams[key]
                val mergedExam = if (localExam == null) {
                    addedMonthlyExams++
                    remoteExam.copy(studentId = localStudentId)
                } else {
                    localExam.copy(
                        bangla = localExam.bangla ?: remoteExam.bangla,
                        english = localExam.english ?: remoteExam.english,
                        math = localExam.math ?: remoteExam.math,
                        socialScience = localExam.socialScience ?: remoteExam.socialScience,
                        science = localExam.science ?: remoteExam.science,
                        religion = localExam.religion ?: remoteExam.religion,
                        physics = localExam.physics ?: remoteExam.physics,
                        chemistry = localExam.chemistry ?: remoteExam.chemistry,
                        biology = localExam.biology ?: remoteExam.biology,
                        hMOrAgri = localExam.hMOrAgri ?: remoteExam.hMOrAgri
                    )
                }
                db.monthlyExamDao().upsert(mergedExam)
            }

            val localExamMarks = db.examMarkDao().getAllOnce().associateBy { Triple(it.studentId, it.examType, it.subjectKey) }
            for (remoteMark in remoteExamMarks) {
                val localStudentId = idMap[remoteMark.studentId] ?: continue
                val key = Triple(localStudentId, remoteMark.examType, remoteMark.subjectKey)
                if (key !in localExamMarks) {
                    db.examMarkDao().upsert(remoteMark.copy(studentId = localStudentId))
                    addedExamMarks++
                } else {
                    val local = localExamMarks[key]!!
                    db.examMarkDao().upsert(local.copy(cq = local.cq ?: remoteMark.cq, mcq = local.mcq ?: remoteMark.mcq))
                }
            }

            MergeResult(addedStudents, addedAttendance, addedMonthlyExams, addedExamMarks)
        } finally {
            remoteDb.close()
            tempFile.delete()
            File(tempFile.absolutePath + "-wal").delete()
            File(tempFile.absolutePath + "-shm").delete()
        }
    }

    private fun key(s: Student): String =
        "${s.className.trim().uppercase()}|${s.section.trim().uppercase()}|${s.studentId.trim()}"

    data class MergeResult(
        val studentsAdded: Int,
        val attendanceAdded: Int,
        val monthlyExamsAdded: Int = 0,
        val examMarksAdded: Int = 0
    )
}
