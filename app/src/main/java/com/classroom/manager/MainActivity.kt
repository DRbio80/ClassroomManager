package com.classroom.manager

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classroom.manager.data.*
import com.classroom.manager.reports.ReportExporter
import com.classroom.manager.reports.ReportPeriod
import com.classroom.manager.ui.ClassroomViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    ClassroomApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomApp(vm: ClassroomViewModel = viewModel()) {
    val classrooms by vm.classrooms.collectAsState()
    val selectedClassId by vm.selectedClassId.collectAsState()
    val selectedClass = classrooms.find { it.id == selectedClassId }

    var currentTab by remember { mutableIntStateOf(0) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddStudentsDialog by remember { mutableStateOf(false) }
    var classroomToDelete by remember { mutableStateOf<Classroom?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedClass?.name ?: "الإدارة الصفية",
                            fontWeight = FontWeight.Bold
                        )
                        if (!selectedClass?.subject.isNullOrBlank()) {
                            Text(
                                text = selectedClass?.subject ?: "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                actions = {
                    if (selectedClass != null) {
                        IconButton(onClick = { showAddStudentsDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "إضافة طلاب")
                        }
                        IconButton(onClick = { classroomToDelete = selectedClass }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الفصل")
                        }
                    }
                    IconButton(onClick = { showAddClassDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "إضافة فصل")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    label = { Text("التحضير") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("السلوك") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text("السلوكيات") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("التقارير") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (classrooms.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classrooms) { room ->
                        FilterChip(
                            selected = room.id == selectedClassId,
                            onClick = { vm.selectClassroom(room.id) },
                            label = { Text(room.name) }
                        )
                    }
                }
            }

            if (classrooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { showAddClassDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إضافة أول فصل دراسي")
                    }
                }
            } else {
                when (currentTab) {
                    0 -> AttendanceScreen(vm)
                    1 -> BehaviorTrackingScreen(vm)
                    2 -> ManageBehaviorsScreen(vm)
                    3 -> ReportsScreen(vm, selectedClass?.name ?: "الفصل")
                }
            }
        }
    }

    if (showAddClassDialog) {
        AddClassroomDialog(
            onDismiss = { showAddClassDialog = false },
            onSave = { name, subject ->
                vm.addClassroom(name, subject)
                showAddClassDialog = false
            }
        )
    }

    if (showAddStudentsDialog) {
        AddStudentsDialog(
            onDismiss = { showAddStudentsDialog = false },
            onSave = { rawNames ->
                vm.addStudentsBulk(rawNames)
                showAddStudentsDialog = false
            }
        )
    }

    classroomToDelete?.let { targetClassroom ->
        AlertDialog(
            onDismissRequest = { classroomToDelete = null },
            title = { Text("تأكيد حذف الفصل") },
            text = { Text("هل أنت متأكد من حذف الفصل \"${targetClassroom.name}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteClassroom(targetClassroom)
                    classroomToDelete = null
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { classroomToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AttendanceScreen(vm: ClassroomViewModel) {
    val students by vm.students.collectAsState()
    val attendance by vm.dailyAttendance.collectAsState()
    val date by vm.selectedDate.collectAsState()
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تاريخ اليوم: $date", fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { vm.markAllPresent() }) {
                Text("تحضير الجميع")
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students) { student ->
                val currentStatus = attendance[student.id]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(student.fullName, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { studentToDelete = student }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الطالب",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AttendanceStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = currentStatus == status,
                                    onClick = { vm.setAttendance(student.id, status) },
                                    label = { Text(status.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    studentToDelete?.let { targetStudent ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("تأكيد حذف الطالب") },
            text = { Text("هل أنت متأكد من حذف الطالب \"${targetStudent.fullName}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteStudent(targetStudent)
                    studentToDelete = null
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun BehaviorTrackingScreen(vm: ClassroomViewModel) {
    val students by vm.students.collectAsState()
    val behaviors by vm.behaviorTypes.collectAsState()
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(students) { student ->
            Card(
                onClick = { selectedStudent = student },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(student.fullName, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("رصد مشاركة / سلوك", color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { studentToDelete = student }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف الطالب",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    selectedStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { selectedStudent = null },
            title = { Text("رصد للطالب: ${student.fullName}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(behaviors) { behavior ->
                        ElevatedButton(
                            onClick = {
                                vm.logBehavior(student.id, behavior.id)
                                selectedStudent = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sign = if (behavior.points > 0) "+" else ""
                            Text("${behavior.title} ($sign${behavior.points})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudent = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    studentToDelete?.let { targetStudent ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("تأكيد حذف الطالب") },
            text = { Text("هل أنت متأكد من حذف الطالب \"${targetStudent.fullName}\"؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteStudent(targetStudent)
                    studentToDelete = null
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ManageBehaviorsScreen(vm: ClassroomViewModel) {
    val behaviors by vm.behaviorTypes.collectAsState()
    var title by remember { mutableStateOf("") }
    var pointsText by remember { mutableStateOf("1") }
    var isPositive by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة سلوك أو بند مشاركة جديد", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم السلوك (مثال: حل مسألة على السبورة)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("النقاط") },
                        modifier = Modifier.width(100.dp)
                    )
                    FilterChip(
                        selected = isPositive,
                        onClick = { isPositive = true },
                        label = { Text("إيجابي (+)") }
                    )
                    FilterChip(
                        selected = !isPositive,
                        onClick = { isPositive = false },
                        label = { Text("ملاحظة (-)") }
                    )
                }
                Button(
                    onClick = {
                        val pts = pointsText.toIntOrNull() ?: 1
                        vm.addBehaviorType(title, pts, isPositive)
                        title = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ السلوك")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(behaviors) { behavior ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sign = if (behavior.points > 0) "+" else ""
                        Text("${behavior.title} ($sign${behavior.points})")
                        IconButton(onClick = { vm.deleteBehaviorType(behavior) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(vm: ClassroomViewModel, classroomName: String) {
    val context = LocalContext.current
    val reportPeriod by vm.reportPeriod.collectAsState()
    val activeRange by vm.activeDateRange.collectAsState()
    val customStart by vm.customStartDate.collectAsState()
    val customEnd by vm.customEndDate.collectAsState()
    val reports by vm.classReport.collectAsState()

    fun openDatePicker(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReportPeriod.entries) { period ->
                FilterChip(
                    selected = reportPeriod == period,
                    onClick = { vm.updateReportPeriod(period) },
                    label = { Text(period.label) }
                )
            }
        }

        if (reportPeriod == ReportPeriod.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { openDatePicker(customStart) { vm.updateCustomDates(it, customEnd) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("من: $customStart")
                }
                OutlinedButton(
                    onClick = { openDatePicker(customEnd) { vm.updateCustomDates(customStart, it) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلى: $customEnd")
                }
            }
        }

        Text(
            text = "الفترة المعروضة: ${activeRange.startDate} إلى ${activeRange.endDate}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Button(
            onClick = {
                val file = ReportExporter.exportClassReportCsv(context, classroomName, activeRange, reports)
                ReportExporter.shareCsvFile(context, file)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("تصدير تقرير الفصل الكامل (Excel / CSV)")
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reports) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.fullName, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val text = ReportExporter.buildStudentSummaryText(classroomName, activeRange, item)
                                ReportExporter.shareTextSummary(context, text, "تقرير ${item.fullName}")
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة تقرير الطالب")
                            }
                        }
                        Text("الحضور: ${item.presentCount} | الغياب: ${item.absentCount} | التأخر: ${item.lateCount}")
                        Text("إيجابي: ${item.positiveCount} | ملاحظات: ${item.negativeCount} | النقاط: ${item.totalPoints}")
                    }
                }
            }
        }
    }
}

@Composable
fun AddClassroomDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة فصل جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الفصل (مثال: 2/1)") })
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("المادة الدراسية") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, subject) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddStudentsDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var namesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة طلاب الفصل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اكتب أو الصق أسماء الطلاب (كل طالب في سطر مستقل):")
                OutlinedTextField(
                    value = namesText,
                    onValueChange = { namesText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(namesText) }) { Text("إضافة القائمة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
