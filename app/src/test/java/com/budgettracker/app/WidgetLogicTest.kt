package com.pardonmyspending.app

import androidx.compose.ui.graphics.Color
import com.pardonmyspending.app.widget.EXTRA_CALENDAR_DATE_NAME
import com.pardonmyspending.app.widget.MonthSummaryData
import com.pardonmyspending.app.widget.TrendValue
import com.pardonmyspending.app.widget.blendColor
import com.pardonmyspending.app.widget.formatCompact
import com.pardonmyspending.app.widget.formatMoney
import com.pardonmyspending.app.widget.formatWhole
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WidgetLogicTest {

    private lateinit var previousLocale: Locale

    @Before fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test fun `formatMoney formats with thousands separators and two decimals`() {
        assertEquals("$1,240.50", formatMoney("$", 1240.5))
        assertEquals("$0.00", formatMoney("$", 0.0))
        assertEquals("€12,345.67", formatMoney("€", 12345.67))
    }

    @Test fun `formatWhole rounds to whole number with separators`() {
        assertEquals("$1,241", formatWhole("$", 1240.6))
        assertEquals("$500", formatWhole("$", 500.0))
    }

    @Test fun `formatCompact switches to k notation at 10000 and above`() {
        assertEquals("$9,999", formatCompact("$", 9999.0))
        assertEquals("$10.0k", formatCompact("$", 10_000.0))
        assertEquals("$12.5k", formatCompact("$", 12_500.0))
        assertEquals("$-15.0k", formatCompact("$", -15_000.0))
    }

    @Test fun `blendColor interpolates and clamps ratio`() {
        val white = Color(1f, 1f, 1f, 1f)
        val black = Color(0f, 0f, 0f, 1f)
        assertEquals(black, blendColor(white, black, 0f))
        assertEquals(white, blendColor(white, black, 1f))
        assertEquals(black, blendColor(white, black, -0.5f))
        assertEquals(white, blendColor(white, black, 2.0f))

        val mid = blendColor(white, black, 0.5f)
        assertEquals(0.5f, mid.red, 0.01f)
        assertEquals(0.5f, mid.green, 0.01f)
        assertEquals(0.5f, mid.blue, 0.01f)
    }

    @Test fun `MonthSummaryData net equals earned minus spent`() {
        val positive = MonthSummaryData(
            monthLabel  = "November",
            spentTrend  = TrendValue(1200.0, null),
            earnedTrend = TrendValue(3000.0, null),
            paymentRows = emptyList()
        )
        assertEquals(1800.0, positive.net, 0.001)

        val negative = positive.copy(
            spentTrend  = TrendValue(4000.0, null),
            earnedTrend = TrendValue(1500.0, null)
        )
        assertEquals(-2500.0, negative.net, 0.001)
    }

    @Test fun `calendar date extra key is non-blank and ISO date round-trips`() {
        assertTrue(EXTRA_CALENDAR_DATE_NAME.isNotBlank())
        val date = LocalDate.of(2026, 11, 23)
        assertEquals(date, LocalDate.parse(date.toString()))
    }
}
