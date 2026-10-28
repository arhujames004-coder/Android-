package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditLogEntry
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.StatusPresent

@Composable
fun SecurityAuditScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val privacySettings by viewModel.privacySettings.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentOrg by viewModel.currentOrg.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("security_audit_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Shield Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_compliance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Zero-Trust Privacy & Security",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AES-256 GCM Encryption • GDPR & FERPA Compliant",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "All biometric face vectors and attendance timestamps are cryptographically salted and hashed before entering local or cloud persistence. Administrative actions are permanently recorded in the immutable audit ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Role-Based Administrative Access Control Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rbac_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Role-Based Access Control (RBAC)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    RbacPermissionRow("Full Governance & Policy", "Admin Only", currentRole.name == "ADMIN")
                    RbacPermissionRow("Launch Scanner & Mark Roll-Call", "Admin, Teacher", currentRole.name != "STUDENT")
                    RbacPermissionRow("View Certified Attendance Exports", "Admin Only", currentRole.name == "ADMIN")
                    RbacPermissionRow("Personal QR Pass & Self Check-in", "All Roles", true)
                }
            }
        }

        // Compliance Toggles
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compliance_toggles_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Protection Policies",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    ComplianceSwitchRow(
                        title = "AES-256 Hardware Keystore Encryption",
                        subtitle = "Hardware-backed key encryption for all stored passes",
                        checked = privacySettings.aes256Enabled,
                        onCheckedChange = { viewModel.updatePrivacySettings(aes = it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    ComplianceSwitchRow(
                        title = "Biometric Privacy Consent Gate",
                        subtitle = "Enforces zero-knowledge storage for facial feature arrays",
                        checked = privacySettings.biometricConsentSigned,
                        onCheckedChange = { viewModel.updatePrivacySettings(biometric = it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    ComplianceSwitchRow(
                        title = "Auto-Sync on Network Restoration",
                        subtitle = "Flush pending offline queues immediately when online",
                        checked = privacySettings.offlineAutoSync,
                        onCheckedChange = { viewModel.updatePrivacySettings(autoSync = it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    ComplianceSwitchRow(
                        title = "90-Day Ephemeral Retention (FERPA)",
                        subtitle = "Automatically anonymize raw timestamp hashes after 90 days",
                        checked = privacySettings.anonymizeAfter90Days,
                        onCheckedChange = { viewModel.updatePrivacySettings(anon = it) }
                    )
                }
            }
        }

        // Live Immutable Audit Trail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Audit Ledger (${auditLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(auditLogs.take(15)) { log ->
            AuditLogRow(entry = log)
        }
    }
}

@Composable
private fun RbacPermissionRow(perm: String, roleAllowed: String, isPermitted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(perm, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(roleAllowed, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = if (isPermitted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isPermitted) StatusPresent else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ComplianceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AuditLogRow(entry: AuditLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.action,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.details,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${entry.actor} • ${entry.ipHash}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
