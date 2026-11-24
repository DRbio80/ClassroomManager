package com.classroom.manager.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.classroom.manager.data.StudentReportSummary
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object ReportExporter {

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return "\"$cleaned\""
    }

    fun exportClassReportCsv(
        context: Context,
        classroomName: String,
        dateRange: DateRange,
        reports: List<StudentReportSummary>
    ): File {
        val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeClassName = classroomName.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
        val fileName = "تقرير_${safeClassName}_${dateRange.startDate}_الى_${dateRange.endDate}.csv"
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.appendLine("الفصل,من تاريخ,إلى تاريخ,اسم الطالب,أيام الحضور,أيام الغياب,مرات التأخر,مشاركات/سلوك إيجابي,ملاحظات سلبية,إجمالي النقاط")
                reports.forEach { item ->
                    val line = listOf(
                        escapeCsv(classroomName),
                        escapeCsv(dateRange.startDate),
                        escapeCsv(dateRange.endDate),
                        escapeCsv(item.fullName),
                        item.presentCount.toString(),
                        item.absentCount.toString(),
                        item.lateCount.toString(),
                        item.positiveCount.toString(),
                        item.negativeCount.toString(),
                        item.totalPoints.toString()
                    ).joinToString(",")
                    writer.appendLine(line)
                }
                writer.flush()
            }
        }
        return file
    }

    fun shareCsvFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "مشاركة ملف التقرير (CSV)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun buildStudentSummaryText(
        classroomName: String,
        dateRange: DateRange,
        student: StudentReportSummary
    ): String {
        return """
            تقرير متابعة طالب
            الفصل: $classroomName
            الفترة: من ${dateRange.startDate} إلى ${dateRange.endDate}
            الطالب: ${student.fullName}
            • الحضور: ${student.presentCount} | الغياب: ${student.absentCount} | التأخر: ${student.lateCount}
            • السلوك الإيجابي والمشاركة: ${student.positiveCount} | الملاحظات: ${student.negativeCount}
            • إجمالي النقاط: ${student.totalPoints}
        """.trimIndent()
    }

    fun shareTextSummary(context: Context, text: String, title: String = "مشاركة التقرير") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
