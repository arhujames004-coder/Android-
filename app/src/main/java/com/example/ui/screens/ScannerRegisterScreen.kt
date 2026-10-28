package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.AttendanceViewModel
import com.example.ui.ScannerMode
import com.example.ui.components.FaceVerificationView
import com.example.ui.components.QrScannerView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerRegisterScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val scannerState by viewModel.scannerUiState.collectAsState()
    val members by viewModel.members.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val records by viewModel.records.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("All") }
    var showSessionDialog by remember { mutableStateOf(false) }

    // Unique departments
    val departments = remember(members) {
        listOf("All") + members.map { it.departmentGroup }.distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("scanner_register_screen")
    ) {
        // Active Session Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("active_session_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "REGISTERING FOR:",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeSession?.title ?: "Select Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeSession?.roomLocation} • ${activeSession?.timeSlot}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showSessionDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("switch_session_button")
                ) {
                    Text("Change", fontSize = 12.sp)
                }
            }
        }

        // Mode Segmented Bar
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("scanner_mode_selector")
        ) {
            ScannerMode.values().forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ScannerMode.values().size),
                    onClick = { viewModel.setScannerMode(mode) },
                    selected = scannerState.scanMode == mode,
                    icon = {},
                    modifier = Modifier.testTag("mode_tab_${mode.name}")
                ) {
                    Text(
                        text = when (mode) {
                            ScannerMode.QR -> "QR Code"
                            ScannerMode.FACE_ID -> "Face ID"
                            ScannerMode.ROLL_CALL -> "Roll-Call"
                        },
                        fontSize = 12.sp,
                        fontWeight = if (scannerState.scanMode == mode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content based on selected mode
        when (scannerState.scanMode) {
            ScannerMode.QR -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QrScannerView(
                            scannerState = scannerState,
                            members = members,
                            onQrScanned = { viewModel.processQrCodeScanned(it) },
                            onToggleFlash = { viewModel.toggleFlashlight() }
                        )
                    }
                }
            }

            ScannerMode.FACE_ID -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FaceVerificationView(
                            scannerState = scannerState,
                            members = members,
                            onStartVerification = { viewModel.startFaceVerificationSimulation(it) }
                        )
                    }
                }
            }

            ScannerMode.ROLL_CALL -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Search & Dept Filter
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by member name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rollcall_search_input")
                    )

                    Spacer(Modifier.height(8.dp))

                    // Department filters
                    ScrollableTabRow(
                        selectedTabIndex = departments.indexOf(selectedDept).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        departments.forEach { dept ->
                            Tab(
                                selected = selectedDept == dept,
                                onClick = { selectedDept = dept },
                                text = { Text(dept, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val filteredMembers = members.filter { m ->
                        val matchesSearch = m.name.contains(searchQuery, ignoreCase = true) || m.id.contains(searchQuery, ignoreCase = true)
                        val matchesDept = selectedDept == "All" || m.departmentGroup == selectedDept
                        matchesSearch && matchesDept
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("rollcall_members_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredMembers) { member ->
                            val existingRecord = records.find { it.sessionId == activeSession?.id && it.memberId == member.id }
                            RollCallMemberItem(
                                member = member,
                                currentStatus = existingRecord?.status,
                                onMarkStatus = { status ->
                                    viewModel.markManual(member, status)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Switch Session Dialog
    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = { Text("Select Attendance Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessions.forEach { s ->
                        Card(
                            onClick = {
                                viewModel.setActiveSession(s)
                                showSessionDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (s.id == activeSession?.id) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(s.title, fontWeight = FontWeight.Bold)
                                Text("${s.category} • ${s.timeSlot}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun RollCallMemberItem(
    member: Member,
    currentStatus: AttendanceStatus?,
    onMarkStatus: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rollcall_item_${member.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.avatarInitials,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }

                    Column {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${member.departmentGroup} • ${member.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                currentStatus?.let { stat ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (stat) {
                            AttendanceStatus.PRESENT -> StatusPresent.copy(alpha = 0.15f)
                            AttendanceStatus.LATE -> StatusLate.copy(alpha = 0.15f)
                            AttendanceStatus.EXCUSED -> StatusExcused.copy(alpha = 0.15f)
                            AttendanceStatus.ABSENT -> StatusAbsent.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (stat) {
                                AttendanceStatus.PRESENT -> StatusPresent
                                AttendanceStatus.LATE -> StatusLate
                                AttendanceStatus.EXCUSED -> StatusExcused
                                AttendanceStatus.ABSENT -> StatusAbsent
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 4 Status Action Buttons (P, L, E, A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusButton(
                    label = "Present",
                    code = "P",
                    color = StatusPresent,
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.PRESENT) }
                )
                StatusButton(
                    label = "Late",
                    code = "L",
                    color = StatusLate,
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.LATE) }
                )
                StatusButton(
                    label = "Excused",
                    code = "E",
                    color = StatusExcused,
                    isSelected = currentStatus == AttendanceStatus.EXCUSED,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.EXCUSED) }
                )
                StatusButton(
                    label = "Absent",
                    code = "A",
                    color = StatusAbsent,
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    modifier = Modifier.weight(1f),
                    onClick = { onMarkStatus(AttendanceStatus.ABSENT) }
                )
            }
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    code: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else color.copy(alpha = 0.12f),
        contentColor = if (isSelected) Color.White else color,
        modifier = modifier
            .height(34.dp)
            .testTag("status_btn_$code")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$code - $label",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
