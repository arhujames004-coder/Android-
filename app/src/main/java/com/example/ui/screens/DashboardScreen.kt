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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.*
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent

@Composable
fun DashboardScreen(
    viewModel: AttendanceViewModel,
    onNavigateToScanner: (ScannerMode) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSchool: () -> Unit = {},
    onNavigateToJob: () -> Unit = {},
    onNavigateToIntro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val org by viewModel.currentOrg.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val records by viewModel.records.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val currentAttendeesCount by viewModel.currentSessionAttendeesCount.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Welcome & Intro Guide Banner
        item {
            Card(
                onClick = onNavigateToIntro,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_intro_guide_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Welcome Intro",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New to BonyVisuals ADT Tracker?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Explore feature tours, CACI, RMU, Tema Plast, & Admin Bony Visuals console.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    FilledTonalButton(
                        onClick = onNavigateToIntro,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("dashboard_open_intro_button")
                    ) {
                        Text("Tour", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Offline / Sync status banner
        item {
            SyncStatusBanner(
                isOffline = isOffline,
                pendingCount = pendingSyncCount,
                isSyncing = isSyncing,
                onToggleOffline = { viewModel.toggleOffline() },
                onSyncNow = { viewModel.triggerSync() }
            )
        }

        // Hero Card
        item {
            DashboardHeroCard(
                org = org,
                activeSession = activeSession,
                attendeesCount = currentAttendeesCount
            )
        }

        // Quick Action Check-in Launchers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_actions_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    title = "QR Scanner",
                    subtitle = "Instant badge pass",
                    icon = Icons.Default.QrCodeScanner,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "action_qr_scan",
                    onClick = { onNavigateToScanner(ScannerMode.QR) }
                )

                ActionCard(
                    title = "Face ID",
                    subtitle = "Biometric check-in",
                    icon = Icons.Default.Face,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    testTag = "action_face_id",
                    onClick = { onNavigateToScanner(ScannerMode.FACE_ID) }
                )

                ActionCard(
                    title = "Roll-Call",
                    subtitle = "Manual register",
                    icon = Icons.Default.PlaylistAddCheck,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    testTag = "action_roll_call",
                    onClick = { onNavigateToScanner(ScannerMode.ROLL_CALL) }
                )
            }
        }

        // School and Job Quick Launchers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("modules_shortcuts_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    onClick = onNavigateToSchool,
                    modifier = Modifier.weight(1f).testTag("action_open_school"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(6.dp).size(20.dp))
                        }
                        Column {
                            Text("School Module", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Periods & GPA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    onClick = onNavigateToJob,
                    modifier = Modifier.weight(1f).testTag("action_open_job"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(6.dp).size(20.dp))
                        }
                        Column {
                            Text("Job Shifts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Clock In / Punches", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Urgent Announcements Notice
        val urgentNotice = announcements.firstOrNull { it.isUrgent }
        if (urgentNotice != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("urgent_announcement_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Urgent Notice",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = urgentNotice.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = urgentNotice.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Real-Time Analytics Breakdown
        item {
            StatusBreakdownBar(records = records)
        }

        // Weekly Attendance Velocity Trend Chart
        item {
            WeeklyTrendsBarChart()
        }

        // Live Real-Time Check-In Stream
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Attendance Feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToReports) {
                    Text("View Reports (${records.size})")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No attendance records yet for this session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(records.take(6)) { rec ->
                LiveFeedItem(record = rec)
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LiveFeedItem(record: AttendanceRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_item_${record.id}"),
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
                // Method Icon badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (record.method) {
                                VerificationMethod.QR_SCAN -> MaterialTheme.colorScheme.primaryContainer
                                VerificationMethod.FACIAL_ID -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (record.method) {
                            VerificationMethod.QR_SCAN -> Icons.Default.QrCodeScanner
                            VerificationMethod.FACIAL_ID -> Icons.Default.Face
                            else -> Icons.Default.Check
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (record.method) {
                            VerificationMethod.QR_SCAN -> MaterialTheme.colorScheme.primary
                            VerificationMethod.FACIAL_ID -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }

                Column {
                    Text(
                        text = record.memberName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = record.memberGroup,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (record.status) {
                        AttendanceStatus.PRESENT -> StatusPresent.copy(alpha = 0.15f)
                        AttendanceStatus.LATE -> StatusLate.copy(alpha = 0.15f)
                        AttendanceStatus.EXCUSED -> MaterialTheme.colorScheme.secondaryContainer
                        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = record.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            AttendanceStatus.PRESENT -> StatusPresent
                            AttendanceStatus.LATE -> StatusLate
                            AttendanceStatus.EXCUSED -> MaterialTheme.colorScheme.secondary
                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (record.syncState == SyncState.PENDING_SYNC) {
                    Text(
                        text = "Queued",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = StatusLate
                    )
                }
            }
        }
    }
}
