package com.pardonmyspending.app.data.model

import java.time.LocalDate
import java.time.YearMonth

enum class RecurringFrequency { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
enum class IntervalUnit        { DAYS, WEEKS, MONTHS, YEARS }
enum class RecurringStatus     { ACTIVE, PAUSED, FINISHED }
enum class EndCondition        { NEVER, DATE, OCCURRENCES }

data class RecurringTransaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val paymentType: PaymentType,
    val note: String = "",
    val frequency: RecurringFrequency,
    val interval: Int = 1,
    val intervalUnit: IntervalUnit = IntervalUnit.MONTHS,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    val endCondition: EndCondition = EndCondition.NEVER,
    val endDate: LocalDate? = null,
    val endAfterOccurrences: Int? = null,
    val occurrencesGenerated: Int = 0
) {
    fun advanceDate(from: LocalDate): LocalDate = when (frequency) {
        RecurringFrequency.DAILY   -> from.plusDays(1)
        RecurringFrequency.WEEKLY  -> from.plusWeeks(1)
        RecurringFrequency.MONTHLY -> from.plusMonths(1)
        RecurringFrequency.YEARLY  -> from.plusYears(1)
        RecurringFrequency.CUSTOM  -> when (intervalUnit) {
            IntervalUnit.DAYS   -> from.plusDays(interval.toLong())
            IntervalUnit.WEEKS  -> from.plusWeeks(interval.toLong())
            IntervalUnit.MONTHS -> from.plusMonths(interval.toLong())
            IntervalUnit.YEARS  -> from.plusYears(interval.toLong())
        }
    }

    fun frequencyLabel(): String = when (frequency) {
        RecurringFrequency.DAILY   -> "Daily"
        RecurringFrequency.WEEKLY  -> "Weekly"
        RecurringFrequency.MONTHLY -> "Monthly"
        RecurringFrequency.YEARLY  -> "Yearly"
        RecurringFrequency.CUSTOM  -> {
            val unit = when (intervalUnit) {
                IntervalUnit.DAYS   -> if (interval == 1) "day"   else "days"
                IntervalUnit.WEEKS  -> if (interval == 1) "week"  else "weeks"
                IntervalUnit.MONTHS -> if (interval == 1) "month" else "months"
                IntervalUnit.YEARS  -> if (interval == 1) "year"  else "years"
            }
            "Every $interval $unit"
        }
    }

    /** Returns all dates this recurring transaction fires within [yearMonth]. */
    fun datesInMonth(yearMonth: YearMonth): List<LocalDate> {
        if (status != RecurringStatus.ACTIVE) return emptyList()
        val monthStart = yearMonth.atDay(1)
        val monthEnd   = yearMonth.atEndOfMonth()
        if (startDate.isAfter(monthEnd)) return emptyList()

        var date = startDate
        var safety = 0
        while (date.isBefore(monthStart)) {
            date = advanceDate(date)
            if (++safety > 100_000) return emptyList()
        }

        val result = mutableListOf<LocalDate>()
        while (!date.isAfter(monthEnd)) {
            if (endCondition == EndCondition.DATE && endDate != null && date.isAfter(endDate)) break
            result.add(date)
            date = advanceDate(date)
        }
        return result
    }
}
