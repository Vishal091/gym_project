package com.gympilot.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GymDataTest {
    private fun member(start: String, expiry: String, due: String = "2026-10-17") = GymMember(
        id = 1,
        name = "QA Member",
        phone = "9876543210",
        planId = "monthly",
        planName = "Monthly",
        fee = 1200,
        startDate = start,
        expiryDate = expiry,
        dueDate = due
    )

    @Test fun membershipIsActiveInsideInclusiveDateRange() {
        assertTrue(isMembershipActive(member("2026-10-01", "2026-10-31"), LocalDate.parse("2026-10-17")))
        assertTrue(isMembershipActive(member("2026-10-17", "2026-10-31"), LocalDate.parse("2026-10-17")))
        assertTrue(isMembershipActive(member("2026-10-01", "2026-10-17"), LocalDate.parse("2026-10-17")))
    }

    @Test fun membershipIsInactiveOutsideDateRange() {
        assertFalse(isMembershipActive(member("2026-10-18", "2026-10-31"), LocalDate.parse("2026-10-17")))
        assertFalse(isMembershipActive(member("2026-09-01", "2026-10-16"), LocalDate.parse("2026-10-17")))
    }

    @Test fun collectionCountsOnlyRecordedPaymentsWithinCurrentMonth() {
        val payments = listOf(
            PaymentRecord("1", 1, 1200, "2026-10-01", "Cash"),
            PaymentRecord("2", 2, 3000, "2026-10-17", "UPI"),
            PaymentRecord("3", 3, 6000, "2026-09-30", "Cash")
        )
        assertEquals(4200, paidThisMonth(payments, LocalDate.parse("2026-10-17")))
        assertEquals(6000, paidThisMonth(payments, LocalDate.parse("2026-09-30")))
    }

    @Test fun dueLabelsHandleOverdueTodayTomorrowAndFuture() {
        val today = LocalDate.parse("2026-10-17")
        assertEquals("Overdue", dueLabel("2026-10-16", today))
        assertEquals("Today", dueLabel("2026-10-17", today))
        assertEquals("Tomorrow", dueLabel("2026-10-18", today))
        assertEquals("25 Oct", dueLabel("2026-10-25", today))
    }

    @Test fun phoneNormalizationSupportsIndianPrefixAndFormatting() {
        assertEquals("9876543210", normalisePhone("+91 98765-43210"))
        assertEquals("9876543210", normalisePhone("98765 43210"))
    }
}
