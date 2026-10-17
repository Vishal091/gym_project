package com.gympilot.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

data class GymPlan(
    val id: String,
    val name: String,
    val durationMonths: Int,
    val fee: Int
)

data class GymMember(
    val id: Int,
    val name: String,
    val phone: String,
    val planId: String,
    val planName: String,
    val fee: Int,
    val startDate: String,
    val expiryDate: String,
    val dueDate: String
)

data class PaymentRecord(
    val id: String,
    val memberId: Int,
    val amount: Int,
    val paidOn: String,
    val method: String
)

data class AttendanceRecord(
    val id: String,
    val memberId: Int,
    val checkedInAt: String
) {
    val day: String get() = checkedInAt.take(10)
}

data class GymState(
    val members: List<GymMember>,
    val plans: List<GymPlan>,
    val payments: List<PaymentRecord>,
    val attendance: List<AttendanceRecord>,
    val nextMemberId: Int
)

fun isMembershipActive(member: GymMember, today: LocalDate = LocalDate.now()): Boolean {
    return try {
        val start = LocalDate.parse(member.startDate)
        val expiry = LocalDate.parse(member.expiryDate)
        !today.isBefore(start) && !today.isAfter(expiry)
    } catch (_: DateTimeParseException) {
        false
    }
}


fun isMembershipExpired(member: GymMember, today: LocalDate = LocalDate.now()): Boolean {
    return try { LocalDate.parse(member.expiryDate).isBefore(today) } catch (_: DateTimeParseException) { false }
}

fun membershipStatus(member: GymMember, today: LocalDate = LocalDate.now()): String {
    return try {
        val start = LocalDate.parse(member.startDate)
        when {
            today.isBefore(start) -> "Not started"
            isMembershipExpired(member, today) -> "Expired"
            else -> "Active"
        }
    } catch (_: DateTimeParseException) {
        "Date needed"
    }
}

fun paidThisMonth(payments: List<PaymentRecord>, today: LocalDate = LocalDate.now()): Int {
    return payments.filter {
        try {
            val date = LocalDate.parse(it.paidOn)
            date.year == today.year && date.month == today.month
        } catch (_: DateTimeParseException) {
            false
        }
    }.sumOf { it.amount }
}

fun dueLabel(dueDate: String, today: LocalDate = LocalDate.now()): String {
    val due = try { LocalDate.parse(dueDate) } catch (_: DateTimeParseException) { return "Date needed" }
    return when {
        due.isBefore(today) -> "Overdue"
        due == today -> "Today"
        due == today.plusDays(1) -> "Tomorrow"
        else -> due.format(DateTimeFormatter.ofPattern("dd MMM"))
    }
}

fun normalisePhone(phone: String): String = phone.filter { it.isDigit() }.takeLast(10)

object GymStorage {
    private const val PREFS = "gympilot_local_data"
    private const val KEY = "state_v2"

    fun load(context: Context): GymState {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (saved.isNullOrBlank()) return sampleState()
        return try {
            val root = JSONObject(saved)
            GymState(
                members = root.getJSONArray("members").toMemberList(),
                plans = root.getJSONArray("plans").toPlanList(),
                payments = root.getJSONArray("payments").toPaymentList(),
                attendance = root.getJSONArray("attendance").toAttendanceList(),
                nextMemberId = root.optInt("nextMemberId", 1)
            )
        } catch (_: Exception) {
            sampleState()
        }
    }

    fun save(context: Context, state: GymState) {
        val root = JSONObject()
        root.put("members", JSONArray().apply { state.members.forEach { put(it.toJson()) } })
        root.put("plans", JSONArray().apply { state.plans.forEach { put(it.toJson()) } })
        root.put("payments", JSONArray().apply { state.payments.forEach { put(it.toJson()) } })
        root.put("attendance", JSONArray().apply { state.attendance.forEach { put(it.toJson()) } })
        root.put("nextMemberId", state.nextMemberId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, root.toString()).apply()
    }

    private fun GymMember.toJson() = JSONObject()
        .put("id", id).put("name", name).put("phone", phone).put("planId", planId)
        .put("planName", planName).put("fee", fee).put("startDate", startDate)
        .put("expiryDate", expiryDate).put("dueDate", dueDate)

    private fun GymPlan.toJson() = JSONObject()
        .put("id", id).put("name", name).put("durationMonths", durationMonths).put("fee", fee)

    private fun PaymentRecord.toJson() = JSONObject()
        .put("id", id).put("memberId", memberId).put("amount", amount).put("paidOn", paidOn).put("method", method)

    private fun AttendanceRecord.toJson() = JSONObject()
        .put("id", id).put("memberId", memberId).put("checkedInAt", checkedInAt)

    private fun JSONArray.toMemberList() = (0 until length()).map { i ->
        val o = getJSONObject(i)
        GymMember(o.getInt("id"), o.getString("name"), o.optString("phone"),
            o.optString("planId", "monthly"), o.optString("planName", "Monthly"),
            o.optInt("fee", 1200), o.optString("startDate", LocalDate.now().toString()),
            o.optString("expiryDate", LocalDate.now().plusMonths(1).minusDays(1).toString()),
            o.optString("dueDate", LocalDate.now().toString()))
    }

    private fun JSONArray.toPlanList() = (0 until length()).map { i ->
        val o = getJSONObject(i)
        GymPlan(o.getString("id"), o.getString("name"), o.optInt("durationMonths", 1), o.optInt("fee", 1200))
    }

    private fun JSONArray.toPaymentList() = (0 until length()).map { i ->
        val o = getJSONObject(i)
        PaymentRecord(o.getString("id"), o.getInt("memberId"), o.getInt("amount"), o.getString("paidOn"), o.optString("method", "Cash"))
    }

    private fun JSONArray.toAttendanceList() = (0 until length()).map { i ->
        val o = getJSONObject(i)
        AttendanceRecord(o.getString("id"), o.getInt("memberId"), o.getString("checkedInAt"))
    }

    private fun sampleState(): GymState {
        val today = LocalDate.now()
        val plans = listOf(
            GymPlan("monthly", "Monthly", 1, 1200),
            GymPlan("quarterly", "Quarterly", 3, 3000),
            GymPlan("half-yearly", "Half-yearly", 6, 6000),
            GymPlan("annual", "Annual", 12, 10000)
        )
        fun member(id: Int, name: String, phone: String, plan: GymPlan, start: LocalDate, expiry: LocalDate, due: LocalDate) =
            GymMember(id, name, phone, plan.id, plan.name, plan.fee, start.toString(), expiry.toString(), due.toString())
        val members = listOf(
            member(1, "Aman Sharma", "98765 43210", plans[0], today.minusDays(20), today.plusDays(10), today),
            member(2, "Priya Verma", "98901 23456", plans[1], today.minusMonths(1), today.plusMonths(2), today.plusDays(6)),
            member(3, "Rahul Kumar", "98111 22334", plans[0], today.minusDays(12), today.plusDays(17), today.plusDays(1)),
            member(4, "Neha Singh", "98770 11223", plans[0], today.minusDays(10), today.plusDays(20), today.plusDays(8)),
            member(5, "Arjun Malik", "99911 22334", plans[2], today.minusMonths(2), today.plusMonths(4), today.plusDays(12)),
            member(6, "Simran Kaur", "98140 22110", plans[0], today.minusMonths(2), today.minusDays(2), today.minusDays(7))
        )
        val payments = listOf(
            PaymentRecord(UUID.randomUUID().toString(), 2, 3000, today.minusDays(4).toString(), "UPI"),
            PaymentRecord(UUID.randomUUID().toString(), 4, 1200, today.minusDays(8).toString(), "Cash")
        )
        val attendance = listOf(
            AttendanceRecord(UUID.randomUUID().toString(), 1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        )
        return GymState(members, plans, payments, attendance, 7)
    }
}
