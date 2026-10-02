package com.scl.mgr.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY studentName COLLATE NOCASE")
    fun observeAll(): Flow<List<Student>>

    @Query("""
        SELECT * FROM students
        WHERE studentName LIKE '%' || :query || '%'
           OR studentId LIKE '%' || :query || '%'
           OR className LIKE '%' || :query || '%'
           OR section LIKE '%' || :query || '%'
        ORDER BY studentName COLLATE NOCASE
    """)
    fun search(query: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Student?

    @Insert
    suspend fun insert(student: Student): Long

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)

    @Query("SELECT COUNT(*) FROM students")
    fun observeCount(): Flow<Int>
}
