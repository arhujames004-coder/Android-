package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JobShift
import com.example.model.Member
import com.example.model.TimecardPunch
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.*

@Composable
fun JobSectionScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val shifts by viewModel.jobShifts.collectAsState()
    val timecards by viewModel.timecardPunches.collectAsState()
    val members by viewModel.members.collectAsState()

    var showAddShiftDialog by remember { mutableStateOf(false) }
    var selectedWorkerForClock by remember { mutableStateOf<Member?>(null) }

    val clockedInCount = members.count { it.isClockedIn }
    val totalEmployees = members.size

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("job_section_screen"),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddShiftDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_shift_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Shift")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workplace & Shift Overview Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("job_overview_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Icon(
                                            Icons.Default.Work,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(8.dp).size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Job & Workplace Shift Manager",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Shift Coverage, Timecard Punches & Hourly Payroll",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Live Shifts",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Shift KPIs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                JobKpiItem("Active Shifts", "${shifts.size}", MaterialTheme.colorScheme.secondary)
                                VerticalDivider(modifier = Modifier.height(32.dp))
                                JobKpiItem("Staff Clocked In", "$clockedInCount / $totalEmployees", StatusPresent)
                                VerticalDivider(modifier = Modifier.height(32.dp))
                                JobKpiItem("Completed Punches", "${timecards.count { it.clockOut != null }}", MaterialTheme.colorScheme.onSurface)
                                VerticalDivider(modifier = Modifier.height(32.dp))
                                JobKpiItem("Avg Hours", "7.8 hrs", MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Section Header: Work Shifts & Coverage
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Workplace Shifts & Staffing Coverage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${shifts.size} Configured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(shifts) { shift ->
                    ShiftCard(shift = shift)
                }

                // Section Header: Employee Punch Clock (Quick Clock-In / Clock-Out)
                item {
                    Text(
                        text = "Employee Punch Clock & Shift Roster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(members) { worker ->
                    WorkerClockRow(
                        worker = worker,
                        onClockIn = {
                            val targetShift = shifts.firstOrNull()?.shiftName ?: "General Shift"
                            viewModel.clockInWorker(worker.id, targetShift)
                        },
                        onClockOut = {
                            viewModel.clockOutWorker(worker.id)
                        }
                    )
                }

                // Section Header: Certified Timecard Audit Ledger
                item {
                    Text(
                        text = "Certified Shift Timecard Ledger (${timecards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(timecards) { punch ->
                    TimecardLedgerRow(punch = punch)
                }
            }
        }
    }

    // Add Shift Dialog
    if (showAddShiftDialog) {
        var shiftName by remember { mutableStateOf("") }
        var timeWindow by remember { mutableStateOf("08:00 AM - 04:30 PM") }
        var dept by remember { mutableStateOf("") }
        var supervisor by remember { mutableStateOf("") }
        var requiredStaff by remember { mutableStateOf("15") }

        AlertDialog(
            onDismissRequest = { showAddShiftDialog = false },
            title = { Text("Configure Work Shift") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = shiftName,
                        onValueChange = { shiftName = it },
                        label = { Text("Shift Name (e.g. Morning Alpha Shift)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("shift_name_input")
                    )
                    OutlinedTextField(
                        value = timeWindow,
                        onValueChange = { timeWindow = it },
                        label = { Text("Shift Hours (e.g. 08:00 AM - 04:30 PM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Department / Plant Unit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supervisor,
                        onValueChange = { supervisor = it },
                        label = { Text("Shift Supervisor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = requiredStaff,
                        onValueChange = { requiredStaff = it },
                        label = { Text("Required Headcount") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (shiftName.isNotBlank()) {
                            viewModel.addJobShift(
                                name = shiftName.trim(),
                                timeWindow = timeWindow.trim(),
                                dept = if (dept.isNotBlank()) dept.trim() else "Operations",
                                supervisor = if (supervisor.isNotBlank()) supervisor.trim() else "Shift Lead",
                                required = requiredStaff.toIntOrNull() ?: 15
                            )
                            showAddShiftDialog = false
                        }
                    },
                    enabled = shiftName.isNotBlank(),
                    modifier = Modifier.testTag("confirm_add_shift_button")
                ) {
                    Text("Create Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShiftDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun JobKpiItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ShiftCard(shift: JobShift) {
    val coverageFraction = if (shift.requiredStaff > 0) (shift.clockedInCount.toFloat() / shift.requiredStaff).coerceIn(0f, 1f) else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shift_card_${shift.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = shift.shiftName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${shift.department} • Supervisor: ${shift.supervisor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (shift.isShiftActive) StatusPresent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (shift.isShiftActive) "ACTIVE" else "UPCOMING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (shift.isShiftActive) StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Time: ${shift.timeWindow}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Staffing progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Coverage: ${shift.clockedInCount} / ${shift.requiredStaff}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(coverageFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (coverageFraction >= 0.8f) StatusPresent else StatusLate
                )
            }

            Spacer(Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { coverageFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (coverageFraction >= 0.8f) StatusPresent else StatusLate,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun WorkerClockRow(
    worker: Member,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("worker_clock_row_${worker.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (worker.isClockedIn) StatusPresent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = worker.avatarInitials,
                        fontWeight = FontWeight.Bold,
                        color = if (worker.isClockedIn) StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${worker.jobTitle ?: worker.departmentGroup} • ${if (worker.isClockedIn) "Clocked In: ${worker.lastClockInTime}" else "Off Duty"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (worker.isClockedIn) StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (worker.isClockedIn) {
                Button(
                    onClick = onClockOut,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLate),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp).testTag("clock_out_${worker.id}")
                ) {
                    Icon(Icons.Default.TimerOff, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clock Out", fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = onClockIn,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp).testTag("clock_in_${worker.id}")
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clock In", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun TimecardLedgerRow(punch: TimecardPunch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(punch.memberName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${punch.jobTitle} • ${punch.shiftName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "In: ${punch.clockIn} | Out: ${punch.clockOut ?: "Currently Active"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (punch.clockOut != null) StatusPresent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (punch.clockOut != null) "${punch.hoursWorked} hrs" else "Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (punch.clockOut != null) StatusPresent else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(12.dp), tint = StatusPresent)
                    Text("Supervisor Approved", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
