package com.classroom.manager.reports

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ReportPeriod(val label: String) {
    WEEK("أسبوع"),
    MONTH("شهر"),
    ALL("منذ البداية"),
    CUSTOM("مخصص (من تاريخ إلى تاريخ)")
}

data class DateRange(
    val startDate: String,
    val endDate: String
)

object ReportRangeCalculator {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun resolveRange(
        period: ReportPeriod,
        customStart: LocalDate? = null,
        customEnd: LocalDate? = null,
        today: LocalDate = LocalDate.now()
    ): DateRange {
        return when (period) {
            ReportPeriod.WEEK -> DateRange(
                startDate = today.minusDays(6).format(formatter),
                endDate = today.format(formatter)
            )
            ReportPeriod.MONTH -> DateRange(
                startDate = today.minusDays(29).format(formatter),
                endDate = today.format(formatter)
            )
            ReportPeriod.ALL -> DateRange(
                startDate = "1900-01-01",
                endDate = "2999-12-31"
            )
            ReportPeriod.CUSTOM -> {
                val start = customStart ?: today
                val end = customEnd ?: start
                if (start.isAfter(end)) {
                    DateRange(end.format(formatter), start.format(formatter))
                } else {
                    DateRange(start.format(formatter), end.format(formatter))
                }
            }
        }
    }
}
