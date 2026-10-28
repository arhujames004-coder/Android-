package com.example.model

enum class OrgType(val displayName: String, val memberLabel: String, val sessionLabel: String) {
    SCHOOL("School / Academy", "Student", "Class / Period"),
    ENTERPRISE("Workplace / Job", "Employee", "Shift / Work Meeting"),
    CHURCH("Church / Ministry", "Congregant", "Service / Fellowship")
}

data class Organization(
    val id: String,
    val name: String,
    val code: String,
    val type: OrgType,
    val motto: String,
    val totalMembers: Int,
    val activeSessionsCount: Int
)

enum class UserRole(val title: String, val description: String, val badgeColorHex: Long) {
    ADMIN("Administrator", "Full system governance, privacy & reports export", 0xFF6366F1),
    TEACHER("Leader / Teacher / Supervisor", "Manage shifts, classes & roll-call scanner", 0xFF0D9488),
    STUDENT("Member / Student / Worker", "Personal pass, self check-in & presence logs", 0xFFF59E0B)
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val orgId: String,
    val departmentGroup: String,
    val avatarInitials: String,
    val qrPassToken: String
)

data class Member(
    val id: String,
    val orgId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val departmentGroup: String, // e.g. "Grade 11A", "Engineering", "Choir"
    val qrCodeToken: String,
    val enrolledBiometricFace: Boolean = true,
    val avatarInitials: String,
    val attendanceRate: Float = 94.0f, // percentage
    val currentStreak: Int = 8, // consecutive sessions
    // School-specific attributes
    val studentGrade: String? = null, // e.g., "Grade 11-A"
    val studentMajor: String? = null, // e.g., "Computer Science"
    val academicStanding: String? = "Good Standing", // "Honor Roll", "Good Standing", "Warning"
    // Job-specific attributes
    val jobTitle: String? = null, // e.g., "Full-Stack Engineer", "Shift Lead"
    val jobShift: String? = null, // e.g., "Morning Shift (08:00 - 16:30)"
    val hourlyRate: Double? = 28.50,
    val isClockedIn: Boolean = false,
    val lastClockInTime: String? = null
)

// Academic Course entity for School Section
data class SchoolCourse(
    val id: String,
    val courseCode: String,
    val title: String,
    val instructor: String,
    val room: String,
    val scheduleTime: String,
    val enrolledStudentsCount: Int,
    val attendanceRate: Float,
    val activeToday: Boolean = true
)

// Workplace Shift entity for Job Section
data class JobShift(
    val id: String,
    val shiftName: String,
    val timeWindow: String,
    val department: String,
    val supervisor: String,
    val requiredStaff: Int,
    val clockedInCount: Int,
    val isShiftActive: Boolean = true
)

// Punch Clock / Timecard for Job Section
data class TimecardPunch(
    val id: String,
    val memberId: String,
    val memberName: String,
    val jobTitle: String,
    val shiftName: String,
    val clockIn: String,
    val clockOut: String?,
    val hoursWorked: Float,
    val status: String, // "Active Shift", "Completed", "Overtime"
    val isSupervisorApproved: Boolean = true
)

data class AttendanceSession(
    val id: String,
    val orgId: String,
    val title: String,
    val category: String, // "AP Physics", "Morning Shift", "Sunday Worship"
    val date: String,
    val timeSlot: String,
    val roomLocation: String,
    val expectedAttendees: Int,
    val isActive: Boolean = true
)

enum class AttendanceStatus(val label: String, val colorHex: Long) {
    PRESENT("Present", 0xFF10B981),
    LATE("Late", 0xFFF59E0B),
    EXCUSED("Excused", 0xFF3B82F6),
    ABSENT("Absent", 0xFFEF4444)
}

enum class VerificationMethod(val displayName: String, val iconName: String) {
    QR_SCAN("QR Code Scan", "qr_code_scanner"),
    FACIAL_ID("Facial Recognition", "face"),
    MANUAL_REGISTER("Admin Pass", "edit_note"),
    OFFLINE_SYNC("Offline Synced", "sync")
}

enum class SyncState {
    SYNCED,
    PENDING_SYNC,
    SYNC_FAILED
}

data class AttendanceRecord(
    val id: String,
    val sessionId: String,
    val sessionTitle: String,
    val memberId: String,
    val memberName: String,
    val memberGroup: String,
    val timestamp: String,
    val status: AttendanceStatus,
    val method: VerificationMethod,
    val syncState: SyncState = SyncState.SYNCED,
    val confidenceScore: Float? = null,
    val notes: String = "",
    val hashProof: String = "" // Cryptographic proof hash
)

data class Announcement(
    val id: String,
    val orgId: String,
    val title: String,
    val body: String,
    val author: String,
    val datePosted: String,
    val isUrgent: Boolean = false,
    val targetGroup: String = "All"
)

data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val actor: String,
    val role: String,
    val action: String,
    val details: String,
    val ipHash: String = "192.168.1.xxx [AES-256]"
)

data class PrivacyComplianceState(
    val aes256Enabled: Boolean = true,
    val biometricConsentSigned: Boolean = true,
    val offlineAutoSync: Boolean = true,
    val anonymizeAfter90Days: Boolean = false,
    val ferpaGdprCompliant: Boolean = true
)

data class BatchUploadResult(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int,
    val addedMembers: List<Member>,
    val errorMessages: List<String>
)

data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val isSelected: Boolean = false
)

data class BackendServerMetrics(
    val serverName: String = "BonyVisuals-Cloud-Master",
    val status: String = "HEALTHY & RUNNING",
    val uptimePercent: Float = 99.98f,
    val activeConnections: Int = 54,
    val memoryUsageMb: Float = 340f,
    val memoryCapacityMb: Float = 2048f,
    val dbEngine: String = "SQLite / Cloud Ledger (AES-256)",
    val lastBackup: String = "Automated Snapshot (10 mins ago)",
    val maintenanceMode: Boolean = false
)

data class DatabaseTableStat(
    val tableName: String,
    val rowCount: Int,
    val sizeEstimate: String,
    val encryptionType: String = "AES-256 GCM"
)
