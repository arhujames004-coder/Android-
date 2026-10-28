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
import com.example.model.AttendanceStatus
import com.example.model.Member
import com.example.ui.AttendanceViewModel
import com.example.ui.components.OmniQrCodeView
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent

@Composable
fun MyPassScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsState()
    val org by viewModel.currentOrg.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val records by viewModel.records.collectAsState()

    // Pick current member or first member
    var currentMemberIndex by remember { mutableStateOf(0) }
    val currentMember = members.getOrNull(currentMemberIndex) ?: members.firstOrNull()

    // Records for this member
    val myRecords = records.filter { it.memberId == currentMember?.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_pass_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Member selector chip row (so user can switch which attendee pass they are viewing)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Switch Member Pass Profile:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.take(4).forEachIndexed { index, m ->
                        FilterChip(
                            selected = index == currentMemberIndex,
                            onClick = { currentMemberIndex = index },
                            label = { Text(m.name.substringBefore(" "), fontSize = 12.sp) },
                            modifier = Modifier.testTag("select_pass_member_$index")
                        )
                    }
                }
            }
        }

        // Digital Badge Card
        currentMember?.let { member ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("digital_pass_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge Top Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = org.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "OFFICIAL ATTENDANCE PASS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = member.role.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Avatar & Name
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.avatarInitials,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${member.departmentGroup} • ID: ${member.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        // QR Pass Matrix
                        OmniQrCodeView(
                            data = member.qrCodeToken,
                            size = 190.dp
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Scan at any kiosk or entrance reader",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        // Self check-in test button
                        activeSession?.let { sess ->
                            Button(
                                onClick = {
                                    viewModel.processQrCodeScanned(member.qrCodeToken)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("self_checkin_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Quick Self Check-In (${sess.title.take(15)}...)")
                            }
                        }
                    }
                }
            }

            // Attendance Streak & Rate Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Personal Rate", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${member.attendanceRate.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatusPresent
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Streak", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${member.currentStreak} Sessions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Face ID", style = MaterialTheme.typography.labelSmall)
                            Text(
                                if (member.enrolledBiometricFace) "Active" else "Pending",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (member.enrolledBiometricFace) StatusPresent else StatusLate
                            )
                        }
                    }
                }
            }

            // My Recent Check-in Logs
            item {
                Text(
                    text = "My Attendance History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (myRecords.isEmpty()) {
                item {
                    Text(
                        text = "No recorded check-ins for this pass yet today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(myRecords) { rec ->
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
                                Text(rec.sessionTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${rec.timestamp} • via ${rec.method.displayName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = rec.status.label,
                                fontWeight = FontWeight.Bold,
                                color = if (rec.status == AttendanceStatus.PRESENT) StatusPresent else StatusLate
                            )
                        }
                    }
                }
            }
        }
    }
}
