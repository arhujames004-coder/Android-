package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import com.example.model.*
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val org by viewModel.currentOrg.collectAsState()
    val records by viewModel.records.collectAsState()
    val members by viewModel.members.collectAsState()

    var statusFilter by remember { mutableStateOf<AttendanceStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("CSV") }

    val filteredRecords = records.filter { r ->
        val matchesSearch = r.memberName.contains(searchQuery, ignoreCase = true) ||
                r.memberGroup.contains(searchQuery, ignoreCase = true) ||
                r.sessionTitle.contains(searchQuery, ignoreCase = true)
        val matchesStatus = statusFilter == null || r.status == statusFilter
        matchesSearch && matchesStatus
    }

    // At-Risk Absentee members (< 85% attendance)
    val atRiskMembers = members.filter { it.attendanceRate < 85f }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        // Header & Export Actions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("reports_summary_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cloud Reporting & Audit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${org.name} • Certified Register",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { showExportDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_export_dialog_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Metric chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ReportStat("Total Logs", "${records.size}", MaterialTheme.colorScheme.primary)
                    ReportStat("Punctual", "${records.count { it.status == AttendanceStatus.PRESENT }}", StatusPresent)
                    ReportStat("Late / Excused", "${records.count { it.status == AttendanceStatus.LATE || it.status == AttendanceStatus.EXCUSED }}", StatusLate)
                    ReportStat("At-Risk", "${atRiskMembers.size}", if (atRiskMembers.isEmpty()) StatusPresent else StatusAbsent)
                }
            }
        }

        // Chronic Absenteeism alert if any
        if (atRiskMembers.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("at_risk_alert_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusAbsent)
                    Column {
                        Text(
                            text = "Attendance Intervention Recommended",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${atRiskMembers.joinToString { it.name }} have attendance below 85%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Search & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter records...") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("reports_search_field")
            )
        }

        // Status Filter Chips
        ScrollableTabRow(
            selectedTabIndex = if (statusFilter == null) 0 else AttendanceStatus.values().indexOf(statusFilter) + 1,
            edgePadding = 16.dp,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = statusFilter == null,
                onClick = { statusFilter = null },
                text = { Text("All (${records.size})", fontSize = 12.sp) }
            )
            AttendanceStatus.values().forEach { st ->
                val count = records.count { it.status == st }
                Tab(
                    selected = statusFilter == st,
                    onClick = { statusFilter = st },
                    text = { Text("${st.label} ($count)", fontSize = 12.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Records Lazy Column
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("reports_records_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredRecords) { rec ->
                DetailedRecordCard(record = rec)
            }
        }
    }

    // Export Modal Dialog
    if (showExportDialog) {
        val exportText = if (exportFormat == "CSV") viewModel.exportCsv() else viewModel.exportJson()

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Attendance Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select certified cloud report format:", style = MaterialTheme.typography.bodyMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = exportFormat == "CSV",
                            onClick = { exportFormat = "CSV" },
                            label = { Text("CSV Spreadsheet") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
                        )
                        FilterChip(
                            selected = exportFormat == "JSON",
                            onClick = { exportFormat = "JSON" },
                            label = { Text("JSON API Ledger") },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                        )
                    }

                    // Preview box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(
                                    text = exportText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("BonyVisuals ADT Tracker Export", exportText))
                        Toast.makeText(context, "Copied $exportFormat report to clipboard!", Toast.LENGTH_SHORT).show()

                        // Also launch system share sheet
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, exportText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Attendance Report"))
                        showExportDialog = false
                    },
                    modifier = Modifier.testTag("confirm_export_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReportStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DetailedRecordCard(record: AttendanceRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_item_${record.id}"),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (record.status) {
                                    AttendanceStatus.PRESENT -> StatusPresent
                                    AttendanceStatus.LATE -> StatusLate
                                    AttendanceStatus.EXCUSED -> StatusExcused
                                    AttendanceStatus.ABSENT -> StatusAbsent
                                }
                            )
                    )
                    Text(
                        text = record.memberName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (record.status) {
                        AttendanceStatus.PRESENT -> StatusPresent.copy(alpha = 0.15f)
                        AttendanceStatus.LATE -> StatusLate.copy(alpha = 0.15f)
                        AttendanceStatus.EXCUSED -> StatusExcused.copy(alpha = 0.15f)
                        AttendanceStatus.ABSENT -> StatusAbsent.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = record.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            AttendanceStatus.PRESENT -> StatusPresent
                            AttendanceStatus.LATE -> StatusLate
                            AttendanceStatus.EXCUSED -> StatusExcused
                            AttendanceStatus.ABSENT -> StatusAbsent
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${record.sessionTitle} • ${record.memberGroup}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (record.method) {
                            VerificationMethod.QR_SCAN -> Icons.Default.QrCodeScanner
                            VerificationMethod.FACIAL_ID -> Icons.Default.Face
                            else -> Icons.Default.EditNote
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = record.method.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Cryptographic proof hash badge
                Text(
                    text = "SHA: ${record.hashProof}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
