package com.classroom.manager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classrooms")
data class Classroom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subject: String = ""
)

@Entity(
    tableName = "students",
    foreignKeys = [ForeignKey(
        entity = Classroom::class,
        parentColumns = ["id"],
        childColumns = ["classroomId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("classroomId")]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classroomId: Int,
    val fullName: String
)

enum class AttendanceStatus(val label: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    LATE("متأخر"),
    EXCUSED("مستأذن")
}

@Entity(
    tableName = "attendance",
    foreignKeys = [ForeignKey(
        entity = Student::class,
        parentColumns = ["id"],
        childColumns = ["studentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("studentId"), Index("date")]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String,
    val status: AttendanceStatus
)

@Entity(tableName = "behavior_types")
data class BehaviorType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val points: Int,
    val isPositive: Boolean
)

@Entity(
    tableName = "student_logs",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BehaviorType::class,
            parentColumns = ["id"],
            childColumns = ["behaviorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index("behaviorId")]
)
data class StudentBehaviorLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val behaviorId: Int,
    val date: String,
    val note: String = ""
)
