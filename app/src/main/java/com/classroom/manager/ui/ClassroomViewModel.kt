package com.classroom.manager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.manager.data.*
import com.classroom.manager.reports.DateRange
import com.classroom.manager.reports.ReportPeriod
import com.classroom.manager.reports.ReportRangeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class ClassroomViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).classroomDao()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val classrooms: StateFlow<List<Classroom>> = dao.getAllClassrooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedClassId = MutableStateFlow<Int?>(null)
    val selectedClassId: StateFlow<Int?> = _selectedClassId.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val students: StateFlow<List<Student>> = _selectedClassId
        .flatMapLatest { classId ->
            if (classId == null) flowOf(emptyList())
            else dao.getStudentsByClass(classId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyAttendance: StateFlow> =
        combine(_selectedClassId, _selectedDate) { id, date -> id to date }
            .flatMapLatest { (classId, date) ->
                if (classId == null) flowOf(emptyList())
                else dao.getAttendanceForDate(classId, date)
            }
            .map { records -> records.associate { it.studentId to it.status } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val behaviorTypes: StateFlow<List<BehaviorType>> = dao.getAllBehaviorTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportPeriod = MutableStateFlow(ReportPeriod.WEEK)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    private val _customStartDate = MutableStateFlow(LocalDate.now().minusDays(6))
    val customStartDate: StateFlow<LocalDate> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(LocalDate.now())
    val customEndDate: StateFlow<LocalDate> = _customEndDate.asStateFlow()

    val activeDateRange: StateFlow<DateRange> = combine(
        _reportPeriod, _customStartDate, _customEndDate
    ) { period, start, end ->
        ReportRangeCalculator.resolveRange(period, start, end)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportRangeCalculator.resolveRange(ReportPeriod.WEEK)
    )

    val classReport: StateFlow<List<StudentReportSummary>> =
        combine(_selectedClassId, activeDateRange) { id, range ->
            id to range
        }.flatMapLatest { (classId, range) ->
            if (classId == null) flowOf(emptyList())
            else dao.getClassReportByDateRange(classId, range.startDate, range.endDate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            classrooms.collect { list ->
                if (_selectedClassId.value == null && list.isNotEmpty()) {
                    _selectedClassId.value = list.first().id
                }
            }
        }
    }

    fun selectClassroom(id: Int) {
        _selectedClassId.value = id
    }

    fun addClassroom(name: String, subject: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = dao.insertClassroom(Classroom(name = name.trim(), subject = subject.trim()))
            _selectedClassId.value = newId.toInt()
        }
    }

    fun addStudentsBulk(rawNames: String) {
        val classId = _selectedClassId.value ?: return
        val names = rawNames.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (names.isEmpty()) return

        viewModelScope.launch {
            dao.insertStudents(names.map { Student(classroomId = classId, fullName = it) })
        }
    }

    fun setAttendance(studentId: Int, status: AttendanceStatus) {
        viewModelScope.launch {
            dao.saveAttendance(
                AttendanceRecord(
                    studentId = studentId,
                    date = _selectedDate.value,
                    status = status
                )
            )
        }
    }

    fun markAllPresent() {
        val currentStudents = students.value
        val date = _selectedDate.value
        viewModelScope.launch {
            currentStudents.forEach { student ->
                dao.saveAttendance(
                    AttendanceRecord(
                        studentId = student.id,
                        date = date,
                        status = AttendanceStatus.PRESENT
                    )
                )
            }
        }
    }

    fun logBehavior(studentId: Int, behaviorId: Int, note: String = "") {
        viewModelScope.launch {
            dao.logStudentBehavior(
                StudentBehaviorLog(
                    studentId = studentId,
                    behaviorId = behaviorId,
                    date = _selectedDate.value,
                    note = note
                )
            )
        }
    }

    fun addBehaviorType(title: String, points: Int, isPositive: Boolean) {
        if (title.isBlank()) return
        val signedPoints = if (isPositive) kotlin.math.abs(points) else -kotlin.math.abs(points)
        viewModelScope.launch {
            dao.insertBehaviorType(
                BehaviorType(
                    title = title.trim(),
                    points = signedPoints,
                    isPositive = isPositive
                )
            )
        }
    }

    fun deleteBehaviorType(behavior: BehaviorType) {
        viewModelScope.launch {
            dao.deleteBehaviorType(behavior)
        }
    }

    fun updateReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun updateCustomDates(start: LocalDate, end: LocalDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _reportPeriod.value = ReportPeriod.CUSTOM
    }
}
