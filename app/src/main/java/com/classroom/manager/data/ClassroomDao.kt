package com.classroom.manager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class StudentReportSummary(
    val studentId: Int,
    val fullName: String,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val totalPoints: Int,
    val positiveCount: Int,
    val negativeCount: Int
)

@Dao
interface ClassroomDao {
@Delete
    suspend fun deleteStudent(student: Student)

    @Delete
    suspend fun deleteClassroom(classroom: Classroom)
    @Insert
    suspend fun insertClassroom(classroom: Classroom): Long

    @Query("SELECT * FROM classrooms ORDER BY id DESC")
    fun getAllClassrooms(): Flow<List<Classroom>>

    @Insert
    suspend fun insertStudents(students: List<Student>)

    @Query("SELECT * FROM students WHERE classroomId = :classId ORDER BY fullName ASC")
    fun getStudentsByClass(classId: Int): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttendance(record: AttendanceRecord)

    @Query("""
        SELECT * FROM attendance
        WHERE date = :date
        AND studentId IN (SELECT id FROM students WHERE classroomId = :classId)
    """)
    fun getAttendanceForDate(classId: Int, date: String): Flow<List<AttendanceRecord>>

    @Insert
    suspend fun insertBehaviorType(behavior: BehaviorType)

    @Delete
    suspend fun deleteBehaviorType(behavior: BehaviorType)

    @Query("SELECT * FROM behavior_types ORDER BY isPositive DESC, points DESC")
    fun getAllBehaviorTypes(): Flow<List<BehaviorType>>

    @Insert
    suspend fun logStudentBehavior(log: StudentBehaviorLog)

    @Query("""
        SELECT * FROM student_logs
        WHERE studentId = :studentId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, id DESC
    """)
    fun getLogsForStudentInRange(
        studentId: Int,
        startDate: String,
        endDate: String
    ): Flow<List<StudentBehaviorLog>>

    @Query("""
        SELECT
            s.id AS studentId,
            s.fullName AS fullName,
            COALESCE((
                SELECT COUNT(*) FROM attendance a
                WHERE a.studentId = s.id
                AND a.status = 'PRESENT'
                AND a.date BETWEEN :startDate AND :endDate
            ), 0) AS presentCount,
            COALESCE((
                SELECT COUNT(*) FROM attendance a
                WHERE a.studentId = s.id
                AND a.status = 'ABSENT'
                AND a.date BETWEEN :startDate AND :endDate
            ), 0) AS absentCount,
            COALESCE((
                SELECT COUNT(*) FROM attendance a
                WHERE a.studentId = s.id
                AND a.status = 'LATE'
                AND a.date BETWEEN :startDate AND :endDate
            ), 0) AS lateCount,
            COALESCE((
                SELECT SUM(bt.points)
                FROM student_logs sl
                JOIN behavior_types bt ON sl.behaviorId = bt.id
                WHERE sl.studentId = s.id
                AND sl.date BETWEEN :startDate AND :endDate
            ), 0) AS totalPoints,
            COALESCE((
                SELECT COUNT(*) FROM student_logs sl
                JOIN behavior_types bt ON sl.behaviorId = bt.id
                WHERE sl.studentId = s.id
                AND bt.isPositive = 1
                AND sl.date BETWEEN :startDate AND :endDate
            ), 0) AS positiveCount,
            COALESCE((
                SELECT COUNT(*) FROM student_logs sl
                JOIN behavior_types bt ON sl.behaviorId = bt.id
                WHERE sl.studentId = s.id
                AND bt.isPositive = 0
                AND sl.date BETWEEN :startDate AND :endDate
            ), 0) AS negativeCount
        FROM students s
        WHERE s.classroomId = :classId
        ORDER BY s.fullName ASC
    """)
    fun getClassReportByDateRange(
        classId: Int,
        startDate: String,
        endDate: String
    ): Flow<List<StudentReportSummary>>
}
