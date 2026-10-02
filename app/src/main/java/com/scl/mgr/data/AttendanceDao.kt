package com.scl.mgr.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("""
        SELECT * FROM attendance
        WHERE studentId = :studentId
        ORDER BY date DESC
    """)
    fun observeForStudent(studentId: Long): Flow<List<Attendance>>

    @Query("""
        SELECT * FROM attendance
        WHERE studentId = :studentId AND date = :date
        LIMIT 1
    """)
    suspend fun get(studentId: Long, date: String): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE studentId = :studentId")
    suspend fun deleteForStudent(studentId: Long)

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND present = 1")
    suspend fun presentCount(date: String): Int
}
