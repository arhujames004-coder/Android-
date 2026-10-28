package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.DeviceContact
import com.example.model.Member
import com.example.model.OrgType
import com.example.model.UserRole
import com.example.ui.AttendanceViewModel
import com.example.ui.components.OmniQrCodeView
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import java.util.UUID

@Composable
fun MembersScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val org by viewModel.currentOrg.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMemberForBadge by remember { mutableStateOf<Member?>(null) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showBatchUploadDialog by remember { mutableStateOf(false) }
    var showContactsDialog by remember { mutableStateOf(false) }

    var loadedContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var selectedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Contacts Permission Launcher
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadedContacts = fetchDeviceContacts(context)
            showContactsDialog = true
        } else {
            loadedContacts = getFallbackSampleContacts()
            showContactsDialog = true
        }
    }

    val filtered = members.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.departmentGroup.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                (it.studentGrade?.contains(searchQuery, ignoreCase = true) == true) ||
                (it.jobTitle?.contains(searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("members_screen"),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Device Contacts Floating Action
                SmallFloatingActionButton(
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
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.testTag("contacts_fab")
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = "Import Contacts")
                }

                // Bulk Upload Floating Action
                SmallFloatingActionButton(
                    onClick = { showBatchUploadDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("batch_upload_fab")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload CSV")
                }

                // Add Individual Member Floating Action
                FloatingActionButton(
                    onClick = { showAddMemberDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_member_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Action Banner for Upload & Roster Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("roster_header_card"),
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
                            text = "${org.name} Directory",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${members.size} Enrolled • Update, Upload, or Delete Members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp).testTag("header_contacts_button")
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Contacts", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { showBatchUploadDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp).testTag("bulk_upload_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload CSV", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, grade, job title or email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_search_input")
            )

            Spacer(Modifier.height(12.dp))

            // Member list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("members_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filtered) { member ->
                    MemberRowItem(
                        member = member,
                        onViewBadge = { selectedMemberForBadge = member },
                        onEdit = { memberToEdit = member },
                        onDelete = { memberToDelete = member }
                    )
                }
            }
        }
    }

    // Member Badge & QR Pass Dialog
    selectedMemberForBadge?.let { member ->
        AlertDialog(
            onDismissRequest = { selectedMemberForBadge = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Official Digital Pass")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${member.studentGrade ?: member.jobTitle ?: member.departmentGroup} • ID: ${member.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    OmniQrCodeView(
                        data = member.qrCodeToken,
                        size = 180.dp
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Attendance", style = MaterialTheme.typography.labelSmall)
                            Text("${member.attendanceRate.toInt()}%", fontWeight = FontWeight.Bold, color = StatusPresent)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Streak", style = MaterialTheme.typography.labelSmall)
                            Text("${member.currentStreak} sessions", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Face ID", style = MaterialTheme.typography.labelSmall)
                            Text(if (member.enrolledBiometricFace) "Enrolled" else "Pending", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedMemberForBadge = null }) {
                    Text("Done")
                }
            }
        )
    }

    // DELETE MEMBER CONFIRMATION DIALOG (Requested)
    memberToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Delete Member")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Are you sure you want to delete ${target.name} from ${org.name}?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This will permanently revoke their digital QR badge (${target.qrCodeToken}) and erase their biometric enrollment. Action will be logged by Administrator Bony Visuals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(target.id)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_member_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // UPDATE MEMBER DIALOG
    memberToEdit?.let { target ->
        var editName by remember { mutableStateOf(target.name) }
        var editEmail by remember { mutableStateOf(target.email) }
        var editPhone by remember { mutableStateOf(target.phone) }
        var editGroup by remember { mutableStateOf(target.departmentGroup) }
        var editRole by remember { mutableStateOf(target.role) }
        var editGrade by remember { mutableStateOf(target.studentGrade ?: "") }
        var editJobTitle by remember { mutableStateOf(target.jobTitle ?: "") }
        var editFaceEnrolled by remember { mutableStateOf(target.enrolledBiometricFace) }

        AlertDialog(
            onDismissRequest = { memberToEdit = null },
            title = { Text("Update Member: ${target.name}") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_member_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editGroup,
                            onValueChange = { editGroup = it },
                            label = { Text("Department / Unit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editGrade,
                            onValueChange = { editGrade = it },
                            label = { Text("School Grade / Academic Standing") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editJobTitle,
                            onValueChange = { editJobTitle = it },
                            label = { Text("Job Position / Shift Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Enrolled in Biometric Face ID", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = editFaceEnrolled,
                                onCheckedChange = { editFaceEnrolled = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editEmail.isNotBlank()) {
                            val updated = target.copy(
                                name = editName.trim(),
                                email = editEmail.trim(),
                                phone = editPhone.trim(),
                                departmentGroup = editGroup.trim(),
                                role = editRole,
                                studentGrade = editGrade.ifBlank { null },
                                jobTitle = editJobTitle.ifBlank { null },
                                enrolledBiometricFace = editFaceEnrolled
                            )
                            viewModel.updateMember(updated)
                            memberToEdit = null
                        }
                    },
                    modifier = Modifier.testTag("save_member_update_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // UPLOAD / BATCH IMPORT MEMBERS DIALOG
    if (showBatchUploadDialog) {
        var csvContent by remember {
            mutableStateOf(
                """
                Kofi Boateng, k.boateng@student.rmu.edu.gh, Student, Nautical Level 200, +233 24 111 2233
                Abena Serwaa, a.serwaa@temaplast.com, Student, Blow Molding Line 1, +233 20 222 3344
                Brother Joshua, joshua@caci.org, Student, Sanctuary Choir, +233 27 333 4455
                """.trimIndent()
            )
        }

        AlertDialog(
            onDismissRequest = { showBatchUploadDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Upload & Bulk Import Roster")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste CSV roster lines or select template preset:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Template Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                csvContent = """
                                Kwame Mensah, k.mensah@rmu.edu.gh, Student, Nautical Science L200, +233 24 101 0001
                                Efua Dadson, e.dadson@rmu.edu.gh, Student, Marine Engineering L300, +233 24 101 0002
                                Selorm Klutse, s.klutse@rmu.edu.gh, Student, Port Logistics L100, +233 24 101 0003
                                """.trimIndent()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RMU Template", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                csvContent = """
                                Ebenezer Quaye, e.quaye@temaplast.com, Student, Extrusion Line Alpha, +233 20 202 0001
                                Rita Abbey, r.abbey@temaplast.com, Student, Quality Assurance, +233 20 202 0002
                                Frank Odoi, f.odoi@temaplast.com, Student, Maintenance Unit, +233 20 202 0003
                                """.trimIndent()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tema Plast", fontSize = 11.sp)
                        }
                    }

                    // CSV Input Field
                    OutlinedTextField(
                        value = csvContent,
                        onValueChange = { csvContent = it },
                        label = { Text("CSV Data (Name, Email, Role, Department/Grade, Phone)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("batch_csv_input_field"),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )

                    Text(
                        text = "Format: Full Name, Email, Role (Admin/Teacher/Student), Department or Grade, Phone",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (csvContent.isNotBlank()) {
                            viewModel.uploadBatchMembers(csvContent)
                            showBatchUploadDialog = false
                        }
                    },
                    enabled = csvContent.isNotBlank(),
                    modifier = Modifier.testTag("confirm_batch_import_button")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import & Generate Passes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CONTACTS IMPORT DIALOG
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
                        text = "Select phone address book contacts to enroll into ${org.name}:",
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
                                defaultGroup = if (org.type == OrgType.SCHOOL) "Cadet Group" else "Shift Member"
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

    // Add Member Dialog (Single)
    if (showAddMemberDialog) {
        var newName by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newGroup by remember { mutableStateOf("") }
        var newGrade by remember { mutableStateOf("") }
        var newJobTitle by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Enroll New ${org.type.memberLabel}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_member_name_input")
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_member_email_input")
                    )
                    OutlinedTextField(
                        value = newGroup,
                        onValueChange = { newGroup = it },
                        label = { Text("Department / Unit") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_member_group_input")
                    )
                    OutlinedTextField(
                        value = newGrade,
                        onValueChange = { newGrade = it },
                        label = { Text("Academic Level (e.g. Level 300)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newJobTitle,
                        onValueChange = { newJobTitle = it },
                        label = { Text("Job Position / Shift (if Worker)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newEmail.isNotBlank()) {
                            viewModel.addNewMember(
                                name = newName.trim(),
                                email = newEmail.trim(),
                                group = if (newGroup.isNotBlank()) newGroup.trim() else "General",
                                role = selectedRole,
                                studentGrade = newGrade.ifBlank { null },
                                jobTitle = newJobTitle.ifBlank { null }
                            )
                            showAddMemberDialog = false
                        }
                    },
                    enabled = newName.isNotBlank() && newEmail.isNotBlank(),
                    modifier = Modifier.testTag("confirm_add_member_button")
                ) {
                    Text("Enroll & Generate Pass")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberRowItem(
    member: Member,
    onViewBadge: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_row_${member.id}"),
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
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.avatarInitials,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${member.studentGrade ?: member.jobTitle ?: member.departmentGroup} • ${member.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Rate: ${member.attendanceRate.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPresent,
                            fontWeight = FontWeight.Bold
                        )
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${member.currentStreak} streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp).testTag("edit_member_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Member",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Digital QR Badge Pass button
                IconButton(
                    onClick = onViewBadge,
                    modifier = Modifier.size(34.dp).testTag("badge_member_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "View QR",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Delete member button (Requested)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("delete_member_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Member",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
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
        DeviceContact("c_1", "Kwame Asante", "+233 24 111 2233", "kwame.asante@rmu.edu.gh"),
        DeviceContact("c_2", "Ama Mensah", "+233 20 222 3344", "ama.mensah@temaplast.com"),
        DeviceContact("c_3", "Kojo Osei", "+233 27 333 4455", "kojo.osei@caci.org"),
        DeviceContact("c_4", "Esi Boakye", "+233 55 444 5566", "esi.boakye@rmu.edu.gh"),
        DeviceContact("c_5", "Nana Yaw Addo", "+233 26 555 6677", "nana.addo@temaplast.com")
    )
}
