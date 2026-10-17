package com.gympilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val Ink = Color(0xFF17201D)
private val Forest = Color(0xFF174D3A)
private val Mint = Color(0xFFB9F2D0)
private val Canvas = Color(0xFFF5F7F4)
private val Muted = Color(0xFF718078)
private val Border = Color(0xFFE3E9E4)
private val Alert = Color(0xFFAD5736)

private enum class Page(val label: String) { Home("Home"), Members("Members"), Attendance("Check-in"), Dues("Dues"), Profile("Profile"), Plans("Plans"), Alerts("Reminders") }
private data class PlanEdit(val name: String, val duration: String, val fee: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GymPilotApp() }
    }
}

@Composable
private fun GymPilotApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var state by remember { mutableStateOf(GymStorage.load(context)) }
    var page by remember { mutableStateOf(Page.Home) }
    var addOpen by remember { mutableStateOf(false) }
    var datesOpen by remember { mutableStateOf(false) }
    var paymentMemberId by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableIntStateOf(1) }
    fun persist(next: GymState) { state = next; GymStorage.save(context, next) }
    fun checkIn(id: Int) {
        val today = LocalDate.now().toString()
        val old = state.attendance.firstOrNull { it.memberId == id && it.day == today }
        val records = if (old != null) state.attendance.filterNot { it.id == old.id }
        else state.attendance + AttendanceRecord(UUID.randomUUID().toString(), id, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        persist(state.copy(attendance = records))
    }
    fun paid(id: Int, method: String = "Cash") {
        val member = state.members.firstOrNull { it.id == id } ?: return
        val months = state.plans.firstOrNull { it.id == member.planId }?.durationMonths ?: 1
        val today = LocalDate.now()
        val oldDue = runCatching { LocalDate.parse(member.dueDate) }.getOrDefault(today)
        val nextDue = (if (oldDue.isAfter(today)) oldDue else today).plusMonths(months.toLong())
        val changed = member.copy(dueDate = nextDue.toString(), expiryDate = nextDue.minusDays(1).toString())
        val payment = PaymentRecord(UUID.randomUUID().toString(), id, member.fee, today.toString(), method)
        persist(state.copy(members = state.members.map { if (it.id == id) changed else it }, payments = state.payments + payment))
    }
    val member = state.members.firstOrNull { it.id == selectedId }
    val mainPages = listOf(Page.Home, Page.Members, Page.Attendance, Page.Dues)

    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Mint, background = Canvas, surface = Color.White, onSurface = Ink)) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page == Page.Profile || page == Page.Plans || page == Page.Alerts) {
                        IconButton(onClick = { page = if (page == Page.Profile) Page.Members else Page.Home }) { Icon(Icons.Default.ArrowBack, "Back", tint = Ink) }
                    } else Box(Modifier.size(42.dp).background(Forest, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, null, tint = Mint) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GYMPILOT", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                        Text(when(page) { Page.Home -> "Your gym, in control."; Page.Members -> "Member directory"; Page.Attendance -> "Daily attendance"; Page.Dues -> "Fee tracker"; Page.Profile -> "Member profile"; Page.Plans -> "Plan settings"; Page.Alerts -> "Renewal reminders" }, fontSize = 15.sp, color = Ink, fontWeight = FontWeight.Bold)
                    }
                    when(page) {
                        Page.Members -> IconButton(onClick = { addOpen = true }) { Icon(Icons.Default.PersonAdd, "Add member", tint = Forest) }
                        Page.Home -> IconButton(onClick = { page = Page.Alerts }) { Icon(Icons.Default.NotificationsNone, "Reminders", tint = Ink) }
                        else -> IconButton(onClick = { page = Page.Plans }) { Icon(Icons.Default.Settings, "Plan settings", tint = Ink) }
                    }
                }
            },
            bottomBar = {
                if (page in mainPages) NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    mainPages.forEach { p ->
                        val icon = when(p) { Page.Home -> Icons.Default.GridView; Page.Members -> Icons.Default.Groups; Page.Attendance -> Icons.Default.FactCheck; else -> Icons.Default.CurrencyRupee }
                        NavigationBarItem(selected = page == p, onClick = { page = p }, icon = { Icon(icon, p.label) }, label = { Text(p.label, fontSize = 10.sp, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, indicatorColor = Mint, selectedTextColor = Forest))
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when(page) {
                    Page.Home -> Dashboard(state, { page = it }, { addOpen = true }, { selectedId = it; page = Page.Profile })
                    Page.Members -> MembersScreen(state, query, { query = it }, { checkIn(it) }, { selectedId = it; page = Page.Profile })
                    Page.Attendance -> AttendanceScreen(state, { checkIn(it) }, { selectedId = it; page = Page.Profile })
                    Page.Dues -> DuesScreen(state, { paymentMemberId = it }, { selectedId = it; page = Page.Profile })
                    Page.Profile -> if (member != null) MemberProfile(state, member, { checkIn(it) }, { paymentMemberId = it }, { datesOpen = true }) else EmptyState("Member not found", "Return to the directory and choose a member.")
                    Page.Plans -> PlanSettings(state.plans) { edits ->
                        val plans = state.plans.map { p ->
                            val e = edits[p.id]
                            if (e == null) p else p.copy(name = e.name.trim(), durationMonths = e.duration.toInt(), fee = e.fee.toInt())
                        }
                        persist(state.copy(plans = plans))
                    }
                    Page.Alerts -> AlertsScreen(state, { page = Page.Dues }, { selectedId = it; page = Page.Profile })
                }
            }
        }
        if (addOpen) AddMemberDialog(state.plans, state.members.map { it.phone }, { addOpen = false }) { name, phone, plan, start, expiry ->
            val created = GymMember(state.nextMemberId, name.trim(), phone.trim(), plan.id, plan.name, plan.fee, start, expiry, start)
            persist(state.copy(members = listOf(created) + state.members, nextMemberId = state.nextMemberId + 1))
            selectedId = created.id; page = Page.Profile; addOpen = false
        }
        if (paymentMemberId != null) PaymentMethodDialog(
            amount = state.members.firstOrNull { it.id == paymentMemberId }?.fee ?: 0,
            onDismiss = { paymentMemberId = null },
            onConfirm = { method ->
                val id = paymentMemberId
                if (id != null) paid(id, method)
                paymentMemberId = null
            }
        )
        if (datesOpen && member != null) EditDatesDialog(member, { datesOpen = false }) { start, expiry, due ->
            val updated = member.copy(startDate = start, expiryDate = expiry, dueDate = due)
            persist(state.copy(members = state.members.map { if (it.id == updated.id) updated else it }))
            datesOpen = false
        }
    }
}

@Composable
private fun Dashboard(s: GymState, open: (Page) -> Unit, add: () -> Unit, profile: (Int) -> Unit) {
    val today = LocalDate.now()
    val active = s.members.count { isMembershipActive(it, today) }
    val dueSoon = s.members.count { runCatching { !LocalDate.parse(it.dueDate).isAfter(today.plusDays(7)) }.getOrDefault(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(19.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Text(today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.getDefault())).uppercase(Locale.getDefault()), color = Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(5.dp))
            Text(when(java.time.LocalTime.now().hour) { in 5..11 -> "Good morning 👋"; in 12..16 -> "Good afternoon 👋"; else -> "Good evening 👋" }, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Here’s what’s happening at your gym.", color = Muted, fontSize = 13.sp)
        }
        item {
            Column(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("COLLECTED THIS MONTH", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(formatRupees(paidThisMonth(s.payments, today)), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Demo transactions are included in this preview.", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Color.White.copy(alpha = .2f)); Spacer(Modifier.height(10.dp))
                Text("Saved on this device. Cross-device sync requires a shared backend.", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Metric("Active members", active.toString(), "Within membership dates", Icons.Default.Groups, Modifier.weight(1f))
                Metric("Today's check-ins", s.attendance.count { it.day == today.toString() }.toString(), "Recorded today", Icons.Default.HowToReg, Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Metric("Fee alerts", dueSoon.toString(), "Due within 7 days", Icons.Default.NotificationsActive, Modifier.weight(1f), true)
                Metric("Expired", s.members.count { isMembershipExpired(it, today) }.toString(), "Outside active dates", Icons.Default.EventBusy, Modifier.weight(1f), true)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick actions", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Text("Plans", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { open(Page.Plans) })
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Quick("Add member", Icons.Default.PersonAdd, Modifier.weight(1f), add)
                Quick("Attendance", Icons.Default.FactCheck, Modifier.weight(1f)) { open(Page.Attendance) }
                Quick("Fee dues", Icons.Default.CurrencyRupee, Modifier.weight(1f)) { open(Page.Dues) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Needs attention", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Text("View all", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { open(Page.Dues) })
            }
            Spacer(Modifier.height(8.dp))
            val near = s.members.filter { runCatching { !LocalDate.parse(it.dueDate).isAfter(today.plusDays(7)) }.getOrDefault(false) }.sortedBy { it.dueDate }.take(3)
            if (near.isEmpty()) Text("No upcoming fee reminders.", color = Muted, fontSize = 12.sp)
            near.forEach { MemberRow(it) { profile(it.id) } }
        }
    }
}
@Composable
private fun Metric(title: String, value: String, note: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, warning: Boolean = false) {
    Column(modifier.background(Color.White, RoundedCornerShape(17.dp)).padding(12.dp)) {
        Box(Modifier.size(33.dp).background(if (warning) Color(0xFFFFF0DD) else Color(0xFFE7F6EC), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (warning) Alert else Forest) }
        Spacer(Modifier.height(10.dp)); Text(value, fontSize = 23.sp, color = Ink, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold); Text(note, fontSize = 10.sp, color = Muted)
    }
}
@Composable
private fun Quick(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, action: () -> Unit) {
    Column(modifier.background(Color.White, RoundedCornerShape(15.dp)).clickable(onClick = action).padding(vertical = 12.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(38.dp).background(Color(0xFFE7F6EC), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Forest) }
        Spacer(Modifier.height(6.dp)); Text(label, fontSize = 10.sp, color = Ink, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
private fun MembersScreen(state: GymState, query: String, onQuery: (String) -> Unit, onToggle: (Int) -> Unit, onProfile: (Int) -> Unit) {
    val members = state.members
    val checkedIds = state.attendance.filter { it.day == LocalDate.now().toString() }.map { it.memberId }.toSet()
    val filtered = members.filter { it.name.contains(query, true) || it.phone.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 19.dp)) {
        Spacer(Modifier.height(17.dp)); Text("Members", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(members.count { isMembershipActive(it) }.toString() + " active · " + members.size + " total", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(11.dp))
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Search name or phone…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(14.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Forest))
        Spacer(Modifier.height(11.dp))
        if (filtered.isEmpty()) EmptyState("No members found", "Try another name or phone number.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered, key = { it.id }) { m -> MemberCard(m, checkedIds.contains(m.id), { onToggle(m.id) }, { onProfile(m.id) }) }
        }
    }
}
@Composable
private fun AttendanceScreen(s: GymState, onToggle: (Int) -> Unit, onProfile: (Int) -> Unit) {
    val today = LocalDate.now().toString()
    val ids = s.attendance.filter { it.day == today }.map { it.memberId }.toSet()
    val active = s.members.filter { isMembershipActive(it) }
    Column(Modifier.fillMaxSize().padding(horizontal = 19.dp)) {
        Spacer(Modifier.height(17.dp)); Text("Attendance", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Dated check-ins are saved on this device.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(17.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("TODAY'S CHECK-INS", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(ids.size.toString() + " / " + active.size, fontSize = 27.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            Icon(Icons.Default.FactCheck, null, tint = Mint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(11.dp))
        if (active.isEmpty()) EmptyState("No active members", "Add or renew a membership to record attendance.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(active, key = { it.id }) { m -> MemberCard(m, ids.contains(m.id), { onToggle(m.id) }, { onProfile(m.id) }) }
        }
    }
}
@Composable
private fun DuesScreen(s: GymState, onPaid: (Int) -> Unit, onProfile: (Int) -> Unit) {
    val today = LocalDate.now()
    val due = s.members.filter { runCatching { !LocalDate.parse(it.dueDate).isAfter(today.plusDays(30)) }.getOrDefault(true) }.sortedBy { it.dueDate }
    val overdue = due.count { runCatching { LocalDate.parse(it.dueDate).isBefore(today) }.getOrDefault(true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 19.dp)) {
        Spacer(Modifier.height(17.dp)); Text("Fee tracker", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Overdue and upcoming fees for the next 30 days.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { Summary("DUE SOON", due.size.toString(), Modifier.weight(1f)); Summary("OVERDUE", overdue.toString(), Modifier.weight(1f)) }
        Spacer(Modifier.height(11.dp))
        if (due.isEmpty()) EmptyState("You're all caught up", "No fees due in the next 30 days.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(due, key = { it.id }) { m ->
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(m.name); Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontWeight = FontWeight.Bold); Text(m.planName + " · Due " + dueLabel(m.dueDate), color = Muted, fontSize = 11.sp) }
                        Text(formatRupees(m.fee), color = Ink, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DueBadge(dueLabel(m.dueDate)); Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onProfile(m.id) }) { Text("Profile", fontSize = 10.sp) }
                        Button(onClick = { onPaid(m.id) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Record payment", fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}
@Composable
private fun MemberProfile(s: GymState, m: GymMember, onCheckIn: (Int) -> Unit, onPaid: (Int) -> Unit, onEditDates: () -> Unit) {
    val payments = s.payments.filter { it.memberId == m.id }.sortedByDescending { it.paidOn }
    val visits = s.attendance.filter { it.memberId == m.id }.sortedByDescending { it.checkedInAt }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(m.name); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(m.phone, color = Muted, fontSize = 12.sp) }
            DueBadge(membershipStatus(m))
        }
        Spacer(Modifier.height(13.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Text("MEMBERSHIP", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Detail("Plan", m.planName); Detail("Fee", formatRupees(m.fee)); Detail("Start", m.startDate); Detail("Expiry", m.expiryDate); Detail("Next fee due", m.dueDate)
                    OutlinedButton(onClick = onEditDates, modifier = Modifier.fillMaxWidth()) { Text("Edit membership dates") }
                }
            }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPaid(m.id) }, modifier = Modifier.weight(1f)) { Text("Record payment", fontSize = 11.sp) }
                OutlinedButton(onClick = { onCheckIn(m.id) }, modifier = Modifier.weight(1f)) { Text("Toggle check-in", fontSize = 11.sp) }
            } }
            item { Text("Payment history (" + payments.size + ")", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            if (payments.isEmpty()) item { EmptyState("No payments recorded", "Recorded payments will appear here.") }
            items(payments.take(20), key = { it.id }) { p -> Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, null, tint = Forest); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) { Text(p.paidOn, color = Ink, fontWeight = FontWeight.SemiBold); Text(p.method, color = Muted, fontSize = 11.sp) }
                Text(formatRupees(p.amount), color = Forest, fontWeight = FontWeight.Bold)
            } }
            item { Text("Attendance history (" + visits.size + ")", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            if (visits.isEmpty()) item { EmptyState("No attendance yet", "Check-ins will appear here with timestamps.") }
            items(visits.take(20), key = { it.id }) { a -> Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Forest); Spacer(Modifier.width(8.dp)); Text(a.checkedInAt.replace("T", " "), color = Ink, fontSize = 12.sp)
            } }
        }
    }
}
@Composable
private fun AlertsScreen(s: GymState, viewDues: () -> Unit, profile: (Int) -> Unit) {
    val today = LocalDate.now()
    val alerts = s.members.filter { runCatching { !LocalDate.parse(it.dueDate).isAfter(today.plusDays(30)) }.getOrDefault(true) }.sortedBy { it.dueDate }
    Column(Modifier.fillMaxSize().padding(horizontal = 19.dp)) {
        Spacer(Modifier.height(17.dp)); Text("Renewal reminders", fontSize = 25.sp, color = Ink, fontWeight = FontWeight.Bold)
        Text("Fees due or overdue in the next 30 days.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(12.dp))
        if (alerts.isEmpty()) EmptyState("No reminders", "No fees are due in the next 30 days.")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(alerts, key = { it.id }) { m -> MemberRow(m) { profile(m.id) } } }
        Button(onClick = viewDues, modifier = Modifier.fillMaxWidth()) { Text("Open fee tracker") }
        Spacer(Modifier.height(8.dp))
    }
}
@Composable
private fun PlanSettings(plans: List<GymPlan>, onSave: (Map<String, PlanEdit>) -> Unit) {
    var edits by remember(plans) { mutableStateOf(plans.associate { it.id to PlanEdit(it.name, it.durationMonths.toString(), it.fee.toString()) }) }
    val valid = edits.values.all { it.name.isNotBlank() && (it.duration.toIntOrNull() ?: 0) > 0 && (it.fee.toIntOrNull() ?: -1) >= 0 }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(17.dp)); Text("Membership plans", fontSize = 25.sp, color = Ink, fontWeight = FontWeight.Bold)
        Text("Edits apply to new memberships; existing member fees are snapshots.", color = Muted, fontSize = 12.sp); Spacer(Modifier.height(11.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(plans, key = { it.id }) { p ->
                val e = edits[p.id] ?: PlanEdit(p.name, p.durationMonths.toString(), p.fee.toString())
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(e.name, { edits = edits + (p.id to e.copy(name = it)) }, label = { Text("Plan name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(e.duration, { edits = edits + (p.id to e.copy(duration = it.filter(Char::isDigit))) }, label = { Text("Months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(e.fee, { edits = edits + (p.id to e.copy(fee = it.filter(Char::isDigit))) }, label = { Text("Fee (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            }
        }
        Button(onClick = { onSave(edits) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save plan settings") }
        Spacer(Modifier.height(8.dp))
    }
}
@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f)); Text(value, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
@Composable
private fun MemberCard(m: GymMember, checkedIn: Boolean, toggle: () -> Unit, profile: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(m.name); Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(m.phone, color = Muted, fontSize = 12.sp) }
            DueBadge(if (isMembershipActive(m)) dueLabel(m.dueDate) else membershipStatus(m))
        }
        Spacer(Modifier.height(9.dp)); HorizontalDivider(color = Border); Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(m.planName, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(formatRupees(m.fee) + " · Due " + dueLabel(m.dueDate), color = Muted, fontSize = 11.sp) }
            TextButton(onClick = profile) { Text("Profile", fontSize = 10.sp) }
            OutlinedButton(onClick = toggle, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), shape = RoundedCornerShape(10.dp)) { Text(if (checkedIn) "Undo" else "Check in", fontSize = 10.sp) }
        }
    }
}
@Composable
private fun MemberRow(m: GymMember, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).clickable(onClick = click).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(m.name); Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Due " + dueLabel(m.dueDate) + " · " + formatRupees(m.fee), color = Muted, fontSize = 11.sp) }
        DueBadge(dueLabel(m.dueDate))
    }
}
@Composable
private fun Summary(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(15.dp)).padding(13.dp)) { Text(title, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold); Text(value, fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold) }
}
@Composable
private fun Avatar(name: String) {
    val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
    Box(Modifier.size(41.dp).background(Color(0xFFE7F6EC), CircleShape), contentAlignment = Alignment.Center) { Text(initials, color = Forest, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}
@Composable
private fun DueBadge(due: String) {
    val urgent = due == "Overdue" || due == "Today" || due == "Tomorrow" || due == "Expired"
    Box(Modifier.background(if (urgent) Color(0xFFFFF0E8) else Color(0xFFE7F6EC), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(due, color = if (urgent) Alert else Forest, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}
@Composable
private fun EmptyState(title: String, description: String) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SearchOff, null, tint = Muted, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(7.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(description, color = Muted, fontSize = 12.sp)
    }
}
@Composable
private fun AddMemberDialog(plans: List<GymPlan>, existingPhones: List<String>, dismiss: () -> Unit, save: (String, String, GymPlan, String, String) -> Unit) {
    val today = LocalDate.now()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plan by remember(plans) { mutableStateOf(plans.firstOrNull() ?: GymPlan("monthly", "Monthly", 1, 1200)) }
    var start by remember { mutableStateOf(today.toString()) }
    var expiry by remember { mutableStateOf(today.plusMonths(plan.durationMonths.toLong()).minusDays(1).toString()) }
    val digits = phone.filter(Char::isDigit)
    val phoneValid = digits.length == 10 || (digits.length == 12 && digits.startsWith("91"))
    val duplicate = phoneValid && existingPhones.any { normalisePhone(it) == normalisePhone(phone) }
    val datesValid = runCatching { !LocalDate.parse(expiry).isBefore(LocalDate.parse(start)) }.getOrDefault(false)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Add a member", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 530.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Full name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (phone.isNotBlank() && !phoneValid) Text("Enter a valid 10-digit phone number or +91 number.", color = Alert, fontSize = 11.sp)
                if (duplicate) Text("That phone number already exists.", color = Alert, fontSize = 11.sp)
                Text("Membership plan", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Column(Modifier.heightIn(max = 145.dp)) {
                    plans.forEach { p -> Row(Modifier.fillMaxWidth().clickable {
                        plan = p
                        val s = runCatching { LocalDate.parse(start) }.getOrDefault(today)
                        expiry = s.plusMonths(p.durationMonths.toLong()).minusDays(1).toString()
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(plan.id == p.id, onClick = {
                            plan = p
                            val s = runCatching { LocalDate.parse(start) }.getOrDefault(today)
                            expiry = s.plusMonths(p.durationMonths.toLong()).minusDays(1).toString()
                        })
                        Text(p.name + " · " + formatRupees(p.fee), color = Ink, fontSize = 12.sp)
                    } }
                }
                OutlinedTextField(start, { start = it }, label = { Text("Start (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(expiry, { expiry = it }, label = { Text("Expiry (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (start.isNotBlank() && expiry.isNotBlank() && !datesValid) Text("Enter valid dates; expiry must be on or after start.", color = Alert, fontSize = 11.sp)
            }
        },
        confirmButton = { Button(onClick = { save(name, phone, plan, LocalDate.parse(start).toString(), LocalDate.parse(expiry).toString()) }, enabled = name.isNotBlank() && phoneValid && !duplicate && datesValid) { Text("Save member") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } }
    )
}
@Composable
private fun EditDatesDialog(m: GymMember, dismiss: () -> Unit, save: (String, String, String) -> Unit) {
    var start by remember(m.id) { mutableStateOf(m.startDate) }
    var expiry by remember(m.id) { mutableStateOf(m.expiryDate) }
    var due by remember(m.id) { mutableStateOf(m.dueDate) }
    val valid = runCatching { val s = LocalDate.parse(start); val e = LocalDate.parse(expiry); LocalDate.parse(due); !e.isBefore(s) }.getOrDefault(false)
    AlertDialog(onDismissRequest = dismiss, title = { Text("Edit membership dates") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it }, label = { Text("Start (YYYY-MM-DD)") }, singleLine = true)
            OutlinedTextField(expiry, { expiry = it }, label = { Text("Expiry (YYYY-MM-DD)") }, singleLine = true)
            OutlinedTextField(due, { due = it }, label = { Text("Next fee due (YYYY-MM-DD)") }, singleLine = true)
            if (!valid) Text("Enter valid dates; expiry must be on or after start.", color = Alert, fontSize = 11.sp)
        } },
        confirmButton = { Button(onClick = { save(start, expiry, due) }, enabled = valid) { Text("Save dates") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } }
    )
}
private fun formatRupees(amount: Int): String = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(amount)


@Composable
private fun PaymentMethodDialog(amount: Int, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val methods = listOf("Cash", "UPI", "Bank transfer", "Card", "Other")
    var selected by remember { mutableStateOf("Cash") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Amount: " + formatRupees(amount), color = Ink, fontWeight = FontWeight.SemiBold)
                Text("Choose how the member paid.", color = Muted, fontSize = 12.sp)
                methods.forEach { method ->
                    Row(Modifier.fillMaxWidth().clickable { selected = method }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected == method, onClick = { selected = method })
                        Text(method, color = Ink, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selected) }) { Text("Confirm payment") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
