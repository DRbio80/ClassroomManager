package com.classroom.manager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Classroom::class,
        Student::class,
        AttendanceRecord::class,
        BehaviorType::class,
        StudentBehaviorLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun classroomDao(): ClassroomDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "classroom_manager.db"
                )
                    .addCallback(PrepopulateCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.classroomDao()
                    val defaults = listOf(
                        BehaviorType(title = "مشاركة صفية متميزة", points = 2, isPositive = true),
                        BehaviorType(title = "إجابة صحيحة أو حل تمرين", points = 1, isPositive = true),
                        BehaviorType(title = "تعاون ومساعدة الزملاء", points = 2, isPositive = true),
                        BehaviorType(title = "إحضار الأدوات والواجب", points = 1, isPositive = true),
                        BehaviorType(title = "حديث جانبي أو انشغال", points = -1, isPositive = false),
                        BehaviorType(title = "عدم إحضار الأدوات أو الواجب", points = -1, isPositive = false),
                        BehaviorType(title = "مخالفة تعليمات الصف", points = -2, isPositive = false)
                    )
                    defaults.forEach { dao.insertBehaviorType(it) }
                }
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus): String = status.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus =
        runCatching { AttendanceStatus.valueOf(value) }.getOrDefault(AttendanceStatus.PRESENT)
}
