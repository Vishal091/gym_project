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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF17201D)
private val Forest = Color(0xFF174D3A)
private val Mint = Color(0xFFB9F2D0)
private val Canvas = Color(0xFFF5F7F4)
private val Muted = Color(0xFF718078)
private val Border = Color(0xFFE3E9E4)

data class Member(val id: Int, val name: String, val phone: String, val plan: String, val fee: Int, val due: String, val checkedIn: Boolean = false)
private enum class Page(val label: String) { Home("Home"), Members("Members"), Attendance("Check-in"), Dues("Dues") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GymPilotApp() }
    }
}

@Composable
private fun GymPilotApp() {
    val members = remember { mutableStateListOf(
        Member(1, "Aman Sharma", "98765 43210", "Monthly", 1200, "Today", true),
        Member(2, "Priya Verma", "98901 23456", "Quarterly", 3000, "22 Oct"),
        Member(3, "Rahul Kumar", "98111 22334", "Monthly", 1200, "Tomorrow"),
        Member(4, "Neha Singh", "98770 11223", "Monthly", 1200, "25 Oct"),
        Member(5, "Arjun Malik", "99911 22334", "Half-yearly", 6000, "28 Oct"),
        Member(6, "Simran Kaur", "98140 22110", "Monthly", 1200, "Overdue")
    ) }
    var page by remember { mutableStateOf(Page.Home) }
    var addOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(7) }

    fun toggle(id: Int) {
        val i = members.indexOfFirst { it.id == id }
        if (i >= 0) members[i] = members[i].copy(checkedIn = !members[i].checkedIn)
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Mint, background = Canvas, surface = Color.White, onSurface = Ink)) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Forest, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, null, tint = Mint) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GYMPILOT", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                        Text("Your gym, in control.", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold)
                    }
                    if (page == Page.Members) FilledIconButton(onClick = { addOpen = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Forest)) { Icon(Icons.Default.Add, "Add member") }
                    else Icon(Icons.Default.NotificationsNone, "Notifications", tint = Ink)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    Page.entries.forEach { p ->
                        val icon = when (p) { Page.Home -> Icons.Default.GridView; Page.Members -> Icons.Default.Groups; Page.Attendance -> Icons.Default.FactCheck; Page.Dues -> Icons.Default.CurrencyRupee }
                        NavigationBarItem(selected = page == p, onClick = { page = p }, icon = { Icon(icon, p.label) }, label = { Text(p.label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, indicatorColor = Mint))
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (page) {
                    Page.Home -> Dashboard(members, { page = it }, { addOpen = true })
                    Page.Members -> MembersScreen(members, search, { search = it }, ::toggle)
                    Page.Attendance -> AttendanceScreen(members, ::toggle)
                    Page.Dues -> DuesScreen(members)
                }
            }
        }
        if (addOpen) AddMemberDialog(
            onDismiss = { addOpen = false },
            onSave = { name, phone, plan ->
                val fee = when (plan) { "Quarterly" -> 3000; "Half-yearly" -> 6000; "Annual" -> 10000; else -> 1200 }
                members.add(0, Member(nextId++, name.trim(), phone.trim(), plan, fee, "Not paid"))
                page = Page.Members
                addOpen = false
            }
        )
    }
}

@Composable
private fun Dashboard(members: List<Member>, open: (Page) -> Unit, add: () -> Unit) {
    val urgent = members.filter { it.due == "Overdue" || it.due == "Today" || it.due == "Not paid" }
    val collection = members.filter { it.due != "Overdue" && it.due != "Not paid" }.sumOf { it.fee }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Text("FRIDAY, 16 OCTOBER", color = Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(5.dp)); Text("Good evening 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Here’s what’s happening at your gym.", color = Muted, fontSize = 13.sp)
        }
        item {
            Column(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(24.dp)).padding(20.dp)) {
                Text("ESTIMATED COLLECTION", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("₹$collection", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Sample total from demo member records", color = Color.White.copy(alpha = .75f), fontSize = 11.sp)
                Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color.White.copy(alpha = .2f)); Spacer(Modifier.height(11.dp))
                Text("ⓘ  Connect payments to show live collections.", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Metric("Active members", members.size.toString(), "Across all plans", Icons.Default.Groups, Modifier.weight(1f))
                Metric("Checked in", members.count { it.checkedIn }.toString(), "Today's attendance", Icons.Default.HowToReg, Modifier.weight(1f))
            }
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Metric("Fee alerts", urgent.size.toString(), "Needs follow-up", Icons.Default.NotificationsActive, Modifier.weight(1f), true)
                Metric("Overdue", members.count { it.due == "Overdue" }.toString(), "Past due date", Icons.Default.EventBusy, Modifier.weight(1f), true)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick actions", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Text("See all", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { open(Page.Members) })
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
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
            Spacer(Modifier.height(9.dp))
            urgent.take(3).forEach { MemberRow(it) { open(Page.Dues) } }
        }
        item { Text("DEMO MODE · CHANGES RESET WHEN THE APP CLOSES", color = Muted, fontSize = 9.sp) }
    }
}

@Composable
private fun Metric(title: String, value: String, caption: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, warning: Boolean = false) {
    Column(modifier.background(Color.White, RoundedCornerShape(19.dp)).padding(14.dp)) {
        Box(Modifier.size(34.dp).background(if (warning) Color(0xFFFFF0DD) else Color(0xFFE7F6EC), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (warning) Color(0xFFAD6A1A) else Forest) }
        Spacer(Modifier.height(12.dp)); Text(value, fontSize = 25.sp, color = Ink, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold); Text(caption, fontSize = 10.sp, color = Muted)
    }
}

@Composable
private fun Quick(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.background(Color.White, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 13.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).background(Color(0xFFE7F6EC), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Forest) }
        Spacer(Modifier.height(7.dp)); Text(label, fontSize = 10.sp, color = Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MembersScreen(members: List<Member>, search: String, onSearch: (String) -> Unit, toggle: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp)); Text("Members", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("${members.size} members in your directory", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(13.dp))
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), placeholder = { Text("Search name or phone…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(15.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Border, focusedBorderColor = Forest))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(members.filter { it.name.contains(search, true) || it.phone.contains(search, true) }, key = { it.id }) { MemberCard(it, if (it.checkedIn) "Undo check-in" else "Check in") { toggle(it.id) } }
        }
    }
}

@Composable
private fun AttendanceScreen(members: List<Member>, toggle: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp)); Text("Attendance", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Tap a member to record today's check-in.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(18.dp)).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TODAY'S CHECK-INS", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${members.count { it.checkedIn }} / ${members.size}", fontSize = 27.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.FactCheck, null, tint = Mint, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(13.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) { items(members, key = { it.id }) { MemberCard(it, if (it.checkedIn) "Undo" else "Check in") { toggle(it.id) } } }
    }
}

@Composable
private fun DuesScreen(members: List<Member>) {
    val dues = members.filter { it.due == "Overdue" || it.due == "Today" || it.due == "Not paid" }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp)); Text("Fee tracker", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Find outstanding fees and follow-ups.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Summary("NEEDS ATTENTION", dues.size.toString(), Modifier.weight(1f))
            Summary("OVERDUE", members.count { it.due == "Overdue" }.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(13.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(dues, key = { it.id }) { m ->
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(m.name); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontWeight = FontWeight.Bold); Text("${m.plan} · Due ${m.due.lowercase()}", color = Muted, fontSize = 11.sp) }
                        Text("₹${m.fee}", color = Ink, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Badge(m.due); Spacer(Modifier.weight(1f)); Text("Payment ledger coming next", color = Muted, fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun Summary(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(17.dp)).padding(15.dp)) { Text(title, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold); Text(value, fontSize = 25.sp, color = Ink, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MemberCard(m: Member, actionLabel: String, action: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(m.name); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(m.phone, color = Muted, fontSize = 12.sp) }
            Badge(m.due)
        }
        Spacer(Modifier.height(11.dp)); HorizontalDivider(color = Border); Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(m.plan, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text("₹${m.fee} · Due ${m.due}", color = Muted, fontSize = 11.sp) }
            OutlinedButton(onClick = action, shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text(actionLabel, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun MemberRow(m: Member, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(m.name); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(m.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Membership · ₹${m.fee}", color = Muted, fontSize = 11.sp) }
        Badge(m.due)
    }
}

@Composable
private fun Avatar(name: String) {
    val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
    Box(Modifier.size(42.dp).background(Color(0xFFE7F6EC), CircleShape), contentAlignment = Alignment.Center) { Text(initials, color = Forest, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}

@Composable
private fun Badge(due: String) {
    val urgent = due == "Overdue" || due == "Today" || due == "Not paid"
    Box(Modifier.background(if (urgent) Color(0xFFFFF0E8) else Color(0xFFE7F6EC), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 5.dp)) { Text(due, color = if (urgent) Color(0xFFAD5736) else Forest, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
}

@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("Monthly") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, singleLine = true)
                Text("Membership plan", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                listOf("Monthly", "Quarterly", "Half-yearly", "Annual").forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable { plan = option }.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = plan == option, onClick = { plan = option }); Text(option, color = Ink, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onSave(name, phone, plan) }, enabled = name.isNotBlank()) { Text("Save member") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
