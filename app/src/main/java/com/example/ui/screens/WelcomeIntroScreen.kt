package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Organization
import com.example.model.UserRole
import com.example.ui.AttendanceViewModel
import com.example.ui.ScannerMode
import com.example.ui.theme.*

@Composable
fun WelcomeIntroScreen(
    viewModel: AttendanceViewModel,
    onGetStarted: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToBackend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOrg by viewModel.currentOrg.collectAsState()
    val organizations = viewModel.repository.sampleOrganizations

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("welcome_intro_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("intro_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_attendance_hero_1793061383261),
                            contentDescription = "BonyVisuals ADT Tracker Hero",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x770F172A))
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "BonyVisuals Enterprise",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Welcome to BonyVisuals ADT Tracker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Responsible Real-Time Attendance, Biometrics & Shift Management",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "BonyVisuals ADT Tracker is an all-in-one attendance register and security intelligence platform designed for schools, corporate manufacturing workplaces, and religious assemblies. Built with cryptographic privacy, offline resilience, and biometric automation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onGetStarted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("intro_get_started_btn")
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Launch Attendance Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: 5 Core Architectural Pillars
        item {
            Text(
                text = "Key Features & Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            IntroFeatureCard(
                icon = Icons.Default.QrCodeScanner,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "High-Speed QR & Biometric Face Verification",
                description = "Members receive personalized 2D matrix QR badges. Entrances can authenticate via live CameraX laser scanning or anti-spoofing facial recognition with 98.8% geometric confidence."
            )
        }

        item {
            IntroFeatureCard(
                icon = Icons.Default.School,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = "Regional Maritime University (School Module)",
                description = "Manage academic timetables (Nautical Science, Marine Engineering, Port Logistics), period registers, enrolled student rosters, and track Honor Roll vs. Attendance Warning students."
            )
        }

        item {
            IntroFeatureCard(
                icon = Icons.Default.Work,
                iconColor = MaterialTheme.colorScheme.tertiary,
                title = "Tema Plast (Industrial Job Module)",
                description = "Monitor manufacturing shift coverage (Morning Extrusion, Afternoon Blow Molding, Night Continuous), track required vs. clocked-in staff, and record worker punch clock timecards."
            )
        }

        item {
            IntroFeatureCard(
                icon = Icons.Default.Church,
                iconColor = StatusPresent,
                title = "Christ Apostolic Church International",
                description = "Track Sunday Divine Service, Midweek fellowships, Sanctuary Choir rehearsals, and pastoral roll-calls with automatic attendance rate and streak tracking."
            )
        }

        item {
            IntroFeatureCard(
                icon = Icons.Default.Dns,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "Administrator Backend Console by Bony Visuals",
                description = "Chief Administrator Bony Visuals controls live cloud telemetry, manual cryptographic database backups, maintenance mode, and import from phone address book contacts."
            )
        }

        item {
            IntroFeatureCard(
                icon = Icons.Default.CloudSync,
                iconColor = StatusLate,
                title = "Offline-First Local Queue & Cloud Sync",
                description = "Never lose attendance during network blackouts. Punches are hashed locally with SHA-256 and automatically flushed to the central cloud ledger once connection is restored."
            )
        }

        // Quick Environment Switcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose Your Active Institution:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))

                    organizations.forEach { org ->
                        val isSelected = org.id == currentOrg.id
                        Card(
                            onClick = { viewModel.switchOrganization(org) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
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
                                    Icon(
                                        imageVector = when (org.type) {
                                            com.example.model.OrgType.SCHOOL -> Icons.Default.School
                                            com.example.model.OrgType.ENTERPRISE -> Icons.Default.Work
                                            com.example.model.OrgType.CHURCH -> Icons.Default.Church
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column {
                                        Text(org.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(org.motto, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Exploration Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Try Scanner", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToBackend,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Admin Backend", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun IntroFeatureCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
