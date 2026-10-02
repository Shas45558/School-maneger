package com.scl.mgr.data

import kotlinx.coroutines.flow.Flow

class SchoolRepository(private val db: AppDatabase) {
    fun students(query: String): Flow<List<Student>> =
        if (query.isBlank()) db.studentDao().observeAll() else db.studentDao().search(query)

    fun studentCount(): Flow<Int> = db.studentDao().observeCount()

    suspend fun addStudent(student: Student) = db.studentDao().insert(student)
    suspend fun updateStudent(student: Student) = db.studentDao().update(student)
    suspend fun deleteStudent(student: Student) {
        db.attendanceDao().deleteForStudent(student.id)
        db.studentDao().delete(student)
    }

    suspend fun getStudent(id: Long): Student? = db.studentDao().getById(id)

    fun attendance(studentId: Long): Flow<List<Attendance>> =
        db.attendanceDao().observeForStudent(studentId)

    suspend fun markAttendance(studentId: Long, date: String, present: Boolean) {
        db.attendanceDao().upsert(
            Attendance(studentId = studentId, date = date, present = present)
        )
    }
}
