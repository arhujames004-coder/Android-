package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScannerUiState(
    val scanMode: ScannerMode = ScannerMode.QR,
    val isFlashOn: Boolean = false,
    val lastScannedRecord: AttendanceRecord? = null,
    val scanSuccessMessage: String? = null,
    val isScanningActive: Boolean = true,
    // Face verification state
    val faceLivenessStep: Int = 1, // 1: Align Face, 2: Blink, 3: Verified
    val faceMatchConfidence: Float = 0f,
    val identifiedMember: Member? = null,
    val isVerifyingFace: Boolean = false
)

enum class ScannerMode(val title: String) {
    QR("QR Code Scanner"),
    FACE_ID("Biometric Facial ID"),
    ROLL_CALL("Manual Roll-Call")
}

class AttendanceViewModel(
    val repository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    val currentOrg = repository.currentOrg
    val currentRole = repository.currentRole
    val isOfflineMode = repository.isOfflineMode
    val isSyncing = repository.isSyncing
    val members = repository.members
    val sessions = repository.sessions
    val activeSession = repository.activeSession
    val records = repository.records
    val announcements = repository.announcements
    val auditLogs = repository.auditLogs
    val privacySettings = repository.privacySettings

    // School and Job state flows
    val schoolCourses = repository.schoolCourses
    val jobShifts = repository.jobShifts
    val timecardPunches = repository.timecardPunches
    val backendMetrics = repository.backendMetrics
    val databaseTableStats = repository.databaseTableStats

    private val _scannerUiState = MutableStateFlow(ScannerUiState())
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDepartmentFilter = MutableStateFlow("All")
    val selectedDepartmentFilter: StateFlow<String> = _selectedDepartmentFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val selectedStatusFilter: StateFlow<AttendanceStatus?> = _selectedStatusFilter.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val pendingSyncCount: StateFlow<Int> = records.map { list ->
        list.count { it.syncState == SyncState.PENDING_SYNC }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentSessionAttendeesCount: StateFlow<Int> = combine(records, activeSession) { recs, sess ->
        if (sess == null) 0 else recs.count { it.sessionId == sess.id && (it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDepartmentFilter(dept: String) {
        _selectedDepartmentFilter.value = dept
    }

    fun setStatusFilter(status: AttendanceStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setScannerMode(mode: ScannerMode) {
        _scannerUiState.update { it.copy(scanMode = mode, scanSuccessMessage = null) }
    }

    fun toggleFlashlight() {
        _scannerUiState.update { it.copy(isFlashOn = !it.isFlashOn) }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun switchOrganization(org: Organization) {
        repository.switchOrganization(org)
        _userMessage.value = "Switched to ${org.name}"
    }

    fun switchRole(role: UserRole) {
        repository.switchRole(role)
        _userMessage.value = "Switched role view to ${role.title}"
    }

    fun toggleOffline() {
        repository.toggleOfflineMode()
        val offline = repository.isOfflineMode.value
        _userMessage.value = if (offline) "Offline Mode enabled. Check-ins stored in local queue." else "Online Mode restored. Cloud sync ready."
    }

    fun triggerSync() {
        viewModelScope.launch {
            val count = repository.syncPendingRecords()
            _userMessage.value = if (count > 0) "Successfully synced $count attendance records to cloud!" else "All records are already up to date."
        }
    }

    fun setActiveSession(session: AttendanceSession) {
        repository.setActiveSession(session)
    }

    fun processQrCodeScanned(token: String) {
        val session = activeSession.value ?: return
        val record = repository.markByQrToken(token, session)
        if (record != null) {
            _scannerUiState.update {
                it.copy(
                    lastScannedRecord = record,
                    scanSuccessMessage = "Verified: ${record.memberName} (${record.status.label})"
                )
            }
            _userMessage.value = "QR Verified: ${record.memberName}"
        } else {
            _scannerUiState.update {
                it.copy(scanSuccessMessage = "Invalid or Unrecognized QR Pass: $token")
            }
        }
    }

    fun startFaceVerificationSimulation(targetMember: Member? = null) {
        val session = activeSession.value ?: return
        val candidate = targetMember ?: members.value.firstOrNull() ?: return
        viewModelScope.launch {
            _scannerUiState.update {
                it.copy(
                    isVerifyingFace = true,
                    faceLivenessStep = 1,
                    identifiedMember = candidate,
                    scanSuccessMessage = "Aligning facial geometry..."
                )
            }
            kotlinx.coroutines.delay(800)
            _scannerUiState.update {
                it.copy(faceLivenessStep = 2, scanSuccessMessage = "Liveness check: Anti-spoofing verified")
            }
            kotlinx.coroutines.delay(800)
            val record = repository.verifyByFace(candidate.id, session, 0.988f)
            _scannerUiState.update {
                it.copy(
                    faceLivenessStep = 3,
                    faceMatchConfidence = 0.988f,
                    lastScannedRecord = record,
                    isVerifyingFace = false,
                    scanSuccessMessage = "Face ID Match: ${candidate.name} (98.8% confidence)"
                )
            }
            _userMessage.value = "Facial match verified for ${candidate.name}"
        }
    }

    fun markManual(member: Member, status: AttendanceStatus, notes: String = "") {
        val session = activeSession.value ?: return
        repository.markAttendance(
            member = member,
            session = session,
            status = status,
            method = VerificationMethod.MANUAL_REGISTER,
            notes = notes
        )
        _userMessage.value = "Marked ${member.name} as ${status.label}"
    }

    fun updateMember(member: Member) {
        val updated = repository.updateMember(member)
        _userMessage.value = "Profile updated: ${updated.name}"
    }

    fun addNewMember(
        name: String,
        email: String,
        group: String,
        role: UserRole,
        phone: String = "",
        studentGrade: String? = null,
        jobTitle: String? = null,
        jobShift: String? = null
    ) {
        val member = repository.addNewMember(name, email, group, role, phone, studentGrade, jobTitle, jobShift)
        _userMessage.value = "Registered new ${role.title}: ${member.name}"
    }

    fun uploadBatchMembers(csvContent: String): BatchUploadResult {
        val result = repository.uploadBatchMembers(csvContent)
        _userMessage.value = "Batch import complete: ${result.successful} members added (${result.failed} failed)"
        return result
    }

    // School Actions
    fun addSchoolCourse(code: String, title: String, instructor: String, room: String, time: String, enrolled: Int) {
        val course = repository.addSchoolCourse(code, title, instructor, room, time, enrolled)
        _userMessage.value = "Added course: ${course.courseCode} - ${course.title}"
    }

    // Job Actions
    fun addJobShift(name: String, timeWindow: String, dept: String, supervisor: String, required: Int) {
        val shift = repository.addJobShift(name, timeWindow, dept, supervisor, required)
        _userMessage.value = "Created shift: ${shift.shiftName}"
    }

    fun clockInWorker(memberId: String, shiftName: String) {
        val punch = repository.clockInWorker(memberId, shiftName)
        if (punch != null) {
            _userMessage.value = "${punch.memberName} clocked in to ${punch.shiftName}"
        }
    }

    fun clockOutWorker(memberId: String) {
        val punch = repository.clockOutWorker(memberId)
        if (punch != null) {
            _userMessage.value = "${punch.memberName} clocked out (Shift logged)"
        }
    }

    fun createSession(title: String, category: String, room: String, expected: Int) {
        val session = repository.createSession(title, category, room, expected)
        _userMessage.value = "Created session: ${session.title}"
    }

    fun broadcastAnnouncement(title: String, message: String, isUrgent: Boolean) {
        repository.broadcastAnnouncement(title, message, isUrgent)
        _userMessage.value = "Broadcasted notification: $title"
    }

    fun updatePrivacySettings(aes: Boolean? = null, biometric: Boolean? = null, autoSync: Boolean? = null, anon: Boolean? = null) {
        repository.updatePrivacySetting(aes, biometric, autoSync, anon)
        _userMessage.value = "Compliance settings updated"
    }

    fun deleteMember(memberId: String) {
        val success = repository.deleteMember(memberId)
        if (success) {
            _userMessage.value = "Member deleted successfully"
        }
    }

    fun bulkDeleteMembers(memberIds: List<String>) {
        val count = repository.bulkDeleteMembers(memberIds)
        _userMessage.value = "Permanently deleted $count member(s)"
    }

    fun importContactsAsMembers(contacts: List<DeviceContact>, defaultRole: UserRole, defaultGroup: String) {
        val count = repository.importContactsAsMembers(contacts, defaultRole, defaultGroup)
        _userMessage.value = "Imported $count members from device contacts"
    }

    fun triggerDatabaseBackup() {
        repository.triggerManualDatabaseBackup()
        _userMessage.value = "Cryptographic database backup completed by Bony Visuals"
    }

    fun toggleMaintenanceMode() {
        val isMaintenance = repository.toggleMaintenanceMode()
        _userMessage.value = if (isMaintenance) "Maintenance Mode enabled by Bony Visuals" else "Maintenance Mode disabled. System operational."
    }

    fun clearAuditLogs() {
        repository.clearAuditLogs()
        _userMessage.value = "Security audit ledger cleared by Bony Visuals"
    }

    fun exportCsv(): String = repository.exportReportAsCsv()
    fun exportJson(): String = repository.exportReportAsJson()
}
