package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.DeviceContact
import com.example.model.UserRole
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun AdminBackendScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOrg by viewModel.currentOrg.collectAsState()
    val backendMetrics by viewModel.backendMetrics.collectAsState()
    val tableStats by viewModel.databaseTableStats.collectAsState()
    val members by viewModel.members.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showContactsDialog by remember { mutableStateOf(false) }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }
    var loadedContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var selectedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Contact Permission Launcher
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadedContacts = fetchDeviceContacts(context)
            showContactsDialog = true
        } else {
            // Provide fallback sample address book contacts if permission denied
            loadedContacts = getFallbackSampleContacts()
            showContactsDialog = true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_backend_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Administrator Console Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_header_card"),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "BV",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Column {
                                Text(
                                    text = "Administrator: Bony Visuals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Backend Server & Database Console",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (backendMetrics.maintenanceMode) MaterialTheme.colorScheme.errorContainer
                            else StatusPresent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (backendMetrics.maintenanceMode) "MAINTENANCE" else "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (backendMetrics.maintenanceMode) MaterialTheme.colorScheme.error else StatusPresent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Active Environment: ${currentOrg.name} (${currentOrg.code}) • Zero-Trust Database Node",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Server Health & Telemetry Metrics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cloud Server Health & Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BackendMetricCol("Server Node", backendMetrics.serverName.take(12), MaterialTheme.colorScheme.primary)
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        BackendMetricCol("Uptime", "${backendMetrics.uptimePercent}%", StatusPresent)
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        BackendMetricCol("Connections", "${backendMetrics.activeConnections} active", MaterialTheme.colorScheme.onSurface)
                        VerticalDivider(modifier = Modifier.height(36.dp))
                        BackendMetricCol("Security", "AES-256 GCM", MaterialTheme.colorScheme.secondary)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Memory progress
                    val memPercent = (backendMetrics.memoryUsageMb / backendMetrics.memoryCapacityMb).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Memory Usage: ${backendMetrics.memoryUsageMb.toInt()} MB / ${backendMetrics.memoryCapacityMb.toInt()} MB",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${(memPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { memPercent },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Quick Administrative Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Administrator Controls & Device Tools",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    // Buttons row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPerm) {
                                    loadedContacts = fetchDeviceContacts(context)
                                    showContactsDialog = true
                                } else {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            modifier = Modifier.weight(1f).height(42.dp).testTag("import_contacts_button")
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Device Contacts", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.triggerDatabaseBackup() },
                            modifier = Modifier.weight(1f).height(42.dp).testTag("admin_backup_button")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Backup DB", fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Buttons row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleMaintenanceMode() },
                            modifier = Modifier.weight(1f).height(40.dp).testTag("admin_maintenance_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (backendMetrics.maintenanceMode) "Exit Maint." else "Maintenance Mode", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showPurgeConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).height(40.dp).testTag("admin_clear_audit_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear Audit Logs", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Database Table Inspector
        item {
            Text(
                text = "Database Tables & Storage Schema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(tableStats) { stat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(stat.tableName, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                        Text("${stat.rowCount} records • ${stat.sizeEstimate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stat.encryptionType,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Contacts Import Dialog
    if (showContactsDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Import Device Contacts")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select phone contacts to enroll directly into ${currentOrg.name}:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(loadedContacts) { contact ->
                            val isSelected = selectedContactIds.contains(contact.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedContactIds = if (checked) {
                                                selectedContactIds + contact.id
                                            } else {
                                                selectedContactIds - contact.id
                                            }
                                        }
                                    )
                                    Column {
                                        Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toImport = loadedContacts.filter { selectedContactIds.contains(it.id) }
                        if (toImport.isNotEmpty()) {
                            viewModel.importContactsAsMembers(
                                contacts = toImport,
                                defaultRole = UserRole.STUDENT,
                                defaultGroup = if (currentOrg.type.name == "SCHOOL") "Nautical Cadets" else "Operations"
                            )
                            showContactsDialog = false
                        }
                    },
                    enabled = selectedContactIds.isNotEmpty(),
                    modifier = Modifier.testTag("confirm_import_contacts_button")
                ) {
                    Text("Import (${selectedContactIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Purge Confirm Dialog
    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = { Text("Clear Audit Logs?") },
            text = {
                Text("This action will purge all security and attendance audit entries. This action will be logged in system memory by Administrator Bony Visuals.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAuditLogs()
                        showPurgeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Purge Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BackendMetricCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun fetchDeviceContacts(context: Context): List<DeviceContact> {
    val list = mutableListOf<DeviceContact>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext() && list.size < 50) {
                val id = it.getString(idIndex) ?: UUID.randomUUID().toString()
                val name = it.getString(nameIndex) ?: "Contact"
                val num = it.getString(numIndex) ?: ""
                list.add(DeviceContact(id = id, name = name, phoneNumber = num))
            }
        }
    } catch (e: Exception) {
        // Fallback
    }

    return if (list.isNotEmpty()) list else getFallbackSampleContacts()
}

private fun getFallbackSampleContacts(): List<DeviceContact> {
    return listOf(
        DeviceContact("c_1", "Kwame Asante", "+233 24 111 2233", "kwame.asante@contacts.gh"),
        DeviceContact("c_2", "Ama Mensah", "+233 20 222 3344", "ama.mensah@contacts.gh"),
        DeviceContact("c_3", "Kojo Osei", "+233 27 333 4455", "kojo.osei@contacts.gh"),
        DeviceContact("c_4", "Esi Boakye", "+233 55 444 5566", "esi.boakye@contacts.gh"),
        DeviceContact("c_5", "Nana Yaw Addo", "+233 26 555 6677", "nana.addo@contacts.gh")
    )
}
