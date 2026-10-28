package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository {

    // Predefined Organizations as requested: Regional Maritime University, Tema Plast, Christ Apostolic Church International
    val sampleOrganizations = listOf(
        Organization(
            id = "org_school",
            name = "Regional Maritime University",
            code = "RMU-GH",
            type = OrgType.SCHOOL,
            motto = "Excellence in Maritime Education, Nautical Science & Marine Engineering",
            totalMembers = 340,
            activeSessionsCount = 5
        ),
        Organization(
            id = "org_enterprise",
            name = "Tema Plast",
            code = "TEMA-PLAST",
            type = OrgType.ENTERPRISE,
            motto = "Polymer Extrusion, Industrial Blow Molding & Packaging",
            totalMembers = 195,
            activeSessionsCount = 4
        ),
        Organization(
            id = "org_church",
            name = "Christ Apostolic Church International",
            code = "CACI-INT",
            type = OrgType.CHURCH,
            motto = "The Anointed Vessel of Truth, Power & Holiness",
            totalMembers = 220,
            activeSessionsCount = 3
        )
    )

    private val _currentOrg = MutableStateFlow(sampleOrganizations[0])
    val currentOrg: StateFlow<Organization> = _currentOrg.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.ADMIN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _sessions = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val sessions: StateFlow<List<AttendanceSession>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<AttendanceSession?>(null)
    val activeSession: StateFlow<AttendanceSession?> = _activeSession.asStateFlow()

    private val _records = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val records: StateFlow<List<AttendanceRecord>> = _records.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _privacySettings = MutableStateFlow(PrivacyComplianceState())
    val privacySettings: StateFlow<PrivacyComplianceState> = _privacySettings.asStateFlow()

    // School Section Specific State
    private val _schoolCourses = MutableStateFlow<List<SchoolCourse>>(emptyList())
    val schoolCourses: StateFlow<List<SchoolCourse>> = _schoolCourses.asStateFlow()

    // Job Section Specific State
    private val _jobShifts = MutableStateFlow<List<JobShift>>(emptyList())
    val jobShifts: StateFlow<List<JobShift>> = _jobShifts.asStateFlow()

    private val _timecardPunches = MutableStateFlow<List<TimecardPunch>>(emptyList())
    val timecardPunches: StateFlow<List<TimecardPunch>> = _timecardPunches.asStateFlow()

    // Administrator Backend Server Health & Telemetry
    private val _backendMetrics = MutableStateFlow(BackendServerMetrics())
    val backendMetrics: StateFlow<BackendServerMetrics> = _backendMetrics.asStateFlow()

    private val _databaseTableStats = MutableStateFlow<List<DatabaseTableStat>>(emptyList())
    val databaseTableStats: StateFlow<List<DatabaseTableStat>> = _databaseTableStats.asStateFlow()

    init {
        loadInitialData(sampleOrganizations[0].id)
        refreshDatabaseTableStats()
    }

    fun switchOrganization(org: Organization) {
        _currentOrg.value = org
        loadInitialData(org.id)
        refreshDatabaseTableStats()
        logAudit("ORG_SWITCH", "Bony Visuals switched active organization to ${org.name}")
    }

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        logAudit("ROLE_CHANGE", "Bony Visuals switched role view to ${role.title}")
    }

    fun toggleOfflineMode() {
        val newStatus = !_isOfflineMode.value
        _isOfflineMode.value = newStatus
        logAudit("NETWORK_STATUS", if (newStatus) "Bony Visuals activated Offline Simulation Mode" else "Bony Visuals restored Online Cloud Connectivity")
    }

    suspend fun syncPendingRecords(): Int {
        _isSyncing.value = true
        kotlinx.coroutines.delay(1200)
        val pendingCount = _records.value.count { it.syncState == SyncState.PENDING_SYNC }
        _records.update { list ->
            list.map { if (it.syncState == SyncState.PENDING_SYNC) it.copy(syncState = SyncState.SYNCED) else it }
        }
        _isSyncing.value = false
        refreshDatabaseTableStats()
        logAudit("OFFLINE_SYNC", "Bony Visuals synchronized $pendingCount records with backend ledger")
        return pendingCount
    }

    fun setActiveSession(session: AttendanceSession) {
        _activeSession.value = session
    }

    fun markAttendance(
        member: Member,
        session: AttendanceSession,
        status: AttendanceStatus,
        method: VerificationMethod,
        confidence: Float? = null,
        notes: String = ""
    ): AttendanceRecord {
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val hash = generateCryptoHash("${member.id}-${session.id}-$timeNow")
        val syncState = if (_isOfflineMode.value) SyncState.PENDING_SYNC else SyncState.SYNCED

        val newRecord = AttendanceRecord(
            id = "rec_${UUID.randomUUID().toString().take(8)}",
            sessionId = session.id,
            sessionTitle = session.title,
            memberId = member.id,
            memberName = member.name,
            memberGroup = member.departmentGroup,
            timestamp = timeNow,
            status = status,
            method = method,
            syncState = syncState,
            confidenceScore = confidence,
            notes = notes,
            hashProof = hash
        )

        _records.update { current ->
            val filtered = current.filterNot { it.sessionId == session.id && it.memberId == member.id }
            listOf(newRecord) + filtered
        }

        refreshDatabaseTableStats()
        logAudit("ATTENDANCE_CHECKIN", "${member.name} marked as ${status.label} via ${method.displayName}")
        return newRecord
    }

    fun markByQrToken(qrToken: String, session: AttendanceSession): AttendanceRecord? {
        val member = _members.value.find { it.qrCodeToken.equals(qrToken.trim(), ignoreCase = true) }
            ?: _members.value.find { qrToken.contains(it.id) }
        return if (member != null) {
            markAttendance(member, session, AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN)
        } else null
    }

    fun verifyByFace(memberId: String, session: AttendanceSession, simulatedConfidence: Float = 0.988f): AttendanceRecord? {
        val member = _members.value.find { it.id == memberId } ?: return null
        return markAttendance(
            member = member,
            session = session,
            status = AttendanceStatus.PRESENT,
            method = VerificationMethod.FACIAL_ID,
            confidence = simulatedConfidence,
            notes = "Biometric face verification matched with 98.8% confidence"
        )
    }

    // --- Member Updates & DELETION (Requested) ---

    fun updateMember(updated: Member): Member {
        _members.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
        refreshDatabaseTableStats()
        logAudit("MEMBER_UPDATE", "Bony Visuals updated member profile: ${updated.name} (${updated.id})")
        return updated
    }

    /**
     * Delete a single member from the app and database
     */
    fun deleteMember(memberId: String): Boolean {
        val target = _members.value.find { it.id == memberId } ?: return false
        _members.update { list -> list.filterNot { it.id == memberId } }
        // Clean records or leave audit proof
        _records.update { list -> list.filterNot { it.memberId == memberId } }
        refreshDatabaseTableStats()
        logAudit("MEMBER_DELETE", "Bony Visuals permanently deleted member: ${target.name} (${target.id})")
        return true
    }

    /**
     * Bulk delete multiple members
     */
    fun bulkDeleteMembers(memberIds: List<String>): Int {
        val initialSize = _members.value.size
        _members.update { list -> list.filterNot { memberIds.contains(it.id) } }
        val removed = initialSize - _members.value.size
        refreshDatabaseTableStats()
        logAudit("BULK_DELETE", "Bony Visuals executed bulk purge of $removed member(s)")
        return removed
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
    ): Member {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        val newId = "MBR-${100 + _members.value.size + 1}"
        val member = Member(
            id = newId,
            orgId = _currentOrg.value.id,
            name = name,
            email = email,
            phone = if (phone.isNotBlank()) phone else "+233 24 492 ${(1000..9999).random()}",
            role = role,
            departmentGroup = group,
            qrCodeToken = "BONY-PASS:$newId:${_currentOrg.value.code}",
            enrolledBiometricFace = true,
            avatarInitials = initials.ifEmpty { "BV" },
            attendanceRate = 96.0f,
            currentStreak = 1,
            studentGrade = studentGrade,
            jobTitle = jobTitle,
            jobShift = jobShift
        )
        _members.update { listOf(member) + it }
        refreshDatabaseTableStats()
        logAudit("MEMBER_ENROLL", "Bony Visuals enrolled new ${role.title}: $name in $group")
        return member
    }

    fun uploadBatchMembers(csvContent: String): BatchUploadResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        val newMembersList = mutableListOf<Member>()

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("name", ignoreCase = true) || trimmed.startsWith("full name", ignoreCase = true)) {
                continue
            }
            val parts = trimmed.split(",").map { it.trim().removeSurrounding("\"") }
            if (parts.size >= 2) {
                try {
                    val name = parts[0]
                    val email = parts[1]
                    val roleStr = if (parts.size > 2) parts[2].uppercase() else "STUDENT"
                    val group = if (parts.size > 3 && parts[3].isNotBlank()) parts[3] else "General"
                    val phone = if (parts.size > 4 && parts[4].isNotBlank()) parts[4] else "+233 20 000 ${(1000..9999).random()}"

                    val userRole = when {
                        roleStr.contains("ADMIN") -> UserRole.ADMIN
                        roleStr.contains("TEACH") || roleStr.contains("LEAD") || roleStr.contains("SUPER") -> UserRole.TEACHER
                        else -> UserRole.STUDENT
                    }

                    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                    val newId = "MBR-${100 + _members.value.size + newMembersList.size + 1}"

                    val member = Member(
                        id = newId,
                        orgId = _currentOrg.value.id,
                        name = name,
                        email = email,
                        phone = phone,
                        role = userRole,
                        departmentGroup = group,
                        qrCodeToken = "BONY-PASS:$newId:${_currentOrg.value.code}",
                        enrolledBiometricFace = true,
                        avatarInitials = initials.ifEmpty { "MB" },
                        attendanceRate = 95.0f,
                        currentStreak = 2,
                        studentGrade = if (_currentOrg.value.type == OrgType.SCHOOL) group else null,
                        jobTitle = if (_currentOrg.value.type == OrgType.ENTERPRISE) group else null
                    )
                    newMembersList.add(member)
                    successCount++
                } catch (e: Exception) {
                    failedCount++
                    errors.add("Line ${index + 1}: ${e.message}")
                }
            } else {
                failedCount++
                errors.add("Line ${index + 1}: Insufficient columns")
            }
        }

        if (newMembersList.isNotEmpty()) {
            _members.update { newMembersList + it }
            refreshDatabaseTableStats()
            logAudit("BATCH_UPLOAD", "Bony Visuals imported $successCount members via CSV roster")
        }

        return BatchUploadResult(
            totalProcessed = lines.size,
            successful = successCount,
            failed = failedCount,
            addedMembers = newMembersList,
            errorMessages = errors
        )
    }

    /**
     * Import from Device Contacts
     */
    fun importContactsAsMembers(contacts: List<DeviceContact>, defaultRole: UserRole, defaultGroup: String): Int {
        var count = 0
        val created = contacts.map { contact ->
            count++
            val initials = contact.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
            val newId = "MBR-CNT-${(100..999).random()}"
            Member(
                id = newId,
                orgId = _currentOrg.value.id,
                name = contact.name,
                email = contact.email ?: "${contact.name.lowercase().replace(" ", ".")}@imported.org",
                phone = contact.phoneNumber,
                role = defaultRole,
                departmentGroup = defaultGroup,
                qrCodeToken = "BONY-PASS:$newId:${_currentOrg.value.code}",
                enrolledBiometricFace = true,
                avatarInitials = initials.ifEmpty { "CT" },
                attendanceRate = 97.0f,
                currentStreak = 1,
                studentGrade = if (_currentOrg.value.type == OrgType.SCHOOL) defaultGroup else null,
                jobTitle = if (_currentOrg.value.type == OrgType.ENTERPRISE) defaultGroup else null
            )
        }
        _members.update { created + it }
        refreshDatabaseTableStats()
        logAudit("CONTACTS_IMPORT", "Bony Visuals imported ${contacts.size} members from phone address book")
        return count
    }

    // --- Backend Administration & Database Management ---

    fun triggerManualDatabaseBackup() {
        val timeNow = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date())
        _backendMetrics.update {
            it.copy(lastBackup = "Manual Snapshot by Bony Visuals at $timeNow")
        }
        logAudit("BACKEND_BACKUP", "Bony Visuals initiated manual cryptographic database backup")
    }

    fun toggleMaintenanceMode(): Boolean {
        val current = _backendMetrics.value.maintenanceMode
        _backendMetrics.update { it.copy(maintenanceMode = !current) }
        logAudit("MAINTENANCE_TOGGLE", "Bony Visuals set Maintenance Mode to ${!current}")
        return !current
    }

    fun clearAuditLogs() {
        val count = _auditLogs.value.size
        _auditLogs.value = emptyList()
        logAudit("AUDIT_PURGE", "Bony Visuals purged $count security audit logs")
    }

    private fun refreshDatabaseTableStats() {
        _databaseTableStats.value = listOf(
            DatabaseTableStat("members", _members.value.size, "${_members.value.size * 280} bytes"),
            DatabaseTableStat("attendance_records", _records.value.size, "${_records.value.size * 320} bytes"),
            DatabaseTableStat("school_courses", _schoolCourses.value.size, "${_schoolCourses.value.size * 190} bytes"),
            DatabaseTableStat("job_shifts", _jobShifts.value.size, "${_jobShifts.value.size * 210} bytes"),
            DatabaseTableStat("timecard_punches", _timecardPunches.value.size, "${_timecardPunches.value.size * 240} bytes"),
            DatabaseTableStat("audit_ledger", _auditLogs.value.size, "${_auditLogs.value.size * 350} bytes")
        )
    }

    // --- School Module Features ---

    fun addSchoolCourse(code: String, title: String, instructor: String, room: String, time: String, enrolled: Int): SchoolCourse {
        val course = SchoolCourse(
            id = "crs_${UUID.randomUUID().toString().take(6)}",
            courseCode = code,
            title = title,
            instructor = instructor,
            room = room,
            scheduleTime = time,
            enrolledStudentsCount = enrolled,
            attendanceRate = 92.5f,
            activeToday = true
        )
        _schoolCourses.update { listOf(course) + it }
        refreshDatabaseTableStats()
        logAudit("COURSE_ADD", "Bony Visuals created course: $code - $title at Regional Maritime University")
        return course
    }

    // --- Job / Workplace Module Features ---

    fun addJobShift(name: String, timeWindow: String, dept: String, supervisor: String, required: Int): JobShift {
        val shift = JobShift(
            id = "shf_${UUID.randomUUID().toString().take(6)}",
            shiftName = name,
            timeWindow = timeWindow,
            department = dept,
            supervisor = supervisor,
            requiredStaff = required,
            clockedInCount = 0,
            isShiftActive = true
        )
        _jobShifts.update { listOf(shift) + it }
        refreshDatabaseTableStats()
        logAudit("SHIFT_CREATE", "Bony Visuals configured workplace shift: $name at Tema Plast")
        return shift
    }

    fun clockInWorker(memberId: String, shiftName: String): TimecardPunch? {
        val member = _members.value.find { it.id == memberId } ?: return null
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val punch = TimecardPunch(
            id = "pnch_${UUID.randomUUID().toString().take(6)}",
            memberId = member.id,
            memberName = member.name,
            jobTitle = member.jobTitle ?: member.departmentGroup,
            shiftName = shiftName,
            clockIn = timeNow,
            clockOut = null,
            hoursWorked = 0f,
            status = "Active Shift",
            isSupervisorApproved = true
        )

        updateMember(member.copy(isClockedIn = true, lastClockInTime = timeNow, jobShift = shiftName))
        _timecardPunches.update { listOf(punch) + it }

        _activeSession.value?.let { sess ->
            markAttendance(member, sess, AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN, notes = "Clocked into $shiftName at Tema Plast")
        }

        refreshDatabaseTableStats()
        logAudit("CLOCK_IN", "${member.name} clocked into $shiftName at Tema Plast")
        return punch
    }

    fun clockOutWorker(memberId: String): TimecardPunch? {
        val member = _members.value.find { it.id == memberId } ?: return null
        val activePunch = _timecardPunches.value.find { it.memberId == memberId && it.clockOut == null } ?: return null
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val completedPunch = activePunch.copy(
            clockOut = timeNow,
            hoursWorked = 8.0f,
            status = "Completed Shift"
        )

        updateMember(member.copy(isClockedIn = false, lastClockInTime = null))
        _timecardPunches.update { list ->
            list.map { if (it.id == activePunch.id) completedPunch else it }
        }

        refreshDatabaseTableStats()
        logAudit("CLOCK_OUT", "${member.name} clocked out from Tema Plast (8.0 hrs logged)")
        return completedPunch
    }

    // --- Sessions & Broadcasts ---

    fun createSession(title: String, category: String, room: String, expected: Int): AttendanceSession {
        val dateToday = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        val session = AttendanceSession(
            id = "sess_${UUID.randomUUID().toString().take(6)}",
            orgId = _currentOrg.value.id,
            title = title,
            category = category,
            date = dateToday,
            timeSlot = "08:30 AM - 10:30 AM",
            roomLocation = room,
            expectedAttendees = expected,
            isActive = true
        )
        _sessions.update { listOf(session) + it }
        _activeSession.value = session
        refreshDatabaseTableStats()
        logAudit("SESSION_CREATE", "Bony Visuals scheduled session: $title ($category)")
        return session
    }

    fun broadcastAnnouncement(title: String, message: String, isUrgent: Boolean) {
        val dateNow = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val announcement = Announcement(
            id = "ann_${UUID.randomUUID().toString().take(6)}",
            orgId = _currentOrg.value.id,
            title = title,
            body = message,
            author = "Bony Visuals (Administrator)",
            datePosted = dateNow,
            isUrgent = isUrgent
        )
        _announcements.update { listOf(announcement) + it }
        logAudit("BROADCAST", "Bony Visuals sent ${if (isUrgent) "URGENT " else ""}announcement: $title")
    }

    fun updatePrivacySetting(aes: Boolean? = null, biometric: Boolean? = null, autoSync: Boolean? = null, anon: Boolean? = null) {
        _privacySettings.update { cur ->
            cur.copy(
                aes256Enabled = aes ?: cur.aes256Enabled,
                biometricConsentSigned = biometric ?: cur.biometricConsentSigned,
                offlineAutoSync = autoSync ?: cur.offlineAutoSync,
                anonymizeAfter90Days = anon ?: cur.anonymizeAfter90Days
            )
        }
        logAudit("PRIVACY_POLICY", "Bony Visuals updated compliance security policy")
    }

    fun exportReportAsCsv(): String {
        val sb = StringBuilder()
        sb.append("BonyVisuals ADT Tracker Attendance Register Export\n")
        sb.append("Organization: ${_currentOrg.value.name} (${_currentOrg.value.code})\n")
        sb.append("Generated by Administrator: Bony Visuals\n")
        sb.append("Generated Date: ${Date()}\n")
        sb.append("AES-256 Checksum: SHA256-${generateCryptoHash(_currentOrg.value.id + Date().time)}\n\n")
        sb.append("Record ID,Session,Member ID,Member Name,Group/Grade/Job,Timestamp,Status,Verification Method,Sync State,Proof Hash\n")
        _records.value.forEach { r ->
            sb.append("${r.id},\"${r.sessionTitle}\",${r.memberId},\"${r.memberName}\",\"${r.memberGroup}\",${r.timestamp},${r.status.name},${r.method.name},${r.syncState.name},${r.hashProof}\n")
        }
        logAudit("DATA_EXPORT", "Bony Visuals exported CSV compliance attendance audit report (${_records.value.size} rows)")
        return sb.toString()
    }

    fun exportReportAsJson(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"BonyVisuals ADT Tracker\",\n")
        sb.append("  \"administrator\": \"Bony Visuals\",\n")
        sb.append("  \"organization\": \"${_currentOrg.value.name}\",\n")
        sb.append("  \"code\": \"${_currentOrg.value.code}\",\n")
        sb.append("  \"recordsCount\": ${_records.value.size},\n")
        sb.append("  \"records\": [\n")
        _records.value.forEachIndexed { i, r ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${r.id}\",\n")
            sb.append("      \"sessionTitle\": \"${r.sessionTitle}\",\n")
            sb.append("      \"memberId\": \"${r.memberId}\",\n")
            sb.append("      \"memberName\": \"${r.memberName}\",\n")
            sb.append("      \"group\": \"${r.memberGroup}\",\n")
            sb.append("      \"status\": \"${r.status.name}\",\n")
            sb.append("      \"method\": \"${r.method.name}\",\n")
            sb.append("      \"timestamp\": \"${r.timestamp}\",\n")
            sb.append("      \"proofHash\": \"${r.hashProof}\"\n")
            sb.append("    }${if (i < _records.value.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun logAudit(action: String, details: String) {
        val timeNow = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = AuditLogEntry(
            id = "log_${UUID.randomUUID().toString().take(6)}",
            timestamp = timeNow,
            actor = "Bony Visuals [Administrator]",
            role = _currentRole.value.title,
            action = action,
            details = details
        )
        _auditLogs.update { (listOf(entry) + it).take(150) }
    }

    private fun generateCryptoHash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray())
            bytes.take(8).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "a1b2c3d4"
        }
    }

    private fun loadInitialData(orgId: String) {
        when (orgId) {
            "org_school" -> seedRegionalMaritimeUniversityData()
            "org_enterprise" -> seedTemaPlastData()
            else -> seedChristApostolicChurchData()
        }
    }

    private fun seedRegionalMaritimeUniversityData() {
        val mList = listOf(
            Member("MBR-201", "org_school", "Bony Visuals", "bony.visuals@rmu.edu.gh", "+233 24 100 0001", UserRole.ADMIN, "University Administration", "BONY-PASS:MBR-201:RMU", true, "BV", 100f, 30, studentGrade = "Chief Administrator", studentMajor = "Information Systems & Maritime Safety", academicStanding = "Director of IT"),
            Member("MBR-202", "org_school", "Capt. Kwesi Mensah", "k.mensah@rmu.edu.gh", "+233 24 222 3333", UserRole.TEACHER, "Faculty of Nautical Science", "BONY-PASS:MBR-202:RMU", true, "KM", 98f, 18, studentGrade = "Faculty", studentMajor = "Marine Navigation", academicStanding = "Dean of Studies"),
            Member("MBR-203", "org_school", "Kofi Boateng", "k.boateng@student.rmu.edu.gh", "+233 20 333 4444", UserRole.STUDENT, "Marine Engineering Level 300", "BONY-PASS:MBR-203:RMU", true, "KB", 95f, 12, studentGrade = "Level 300", studentMajor = "Marine Electrical & Automation", academicStanding = "Honor Roll"),
            Member("MBR-204", "org_school", "Abena Osei", "a.osei@student.rmu.edu.gh", "+233 27 444 5555", UserRole.STUDENT, "Ports & Shipping Level 200", "BONY-PASS:MBR-204:RMU", true, "AO", 96f, 15, studentGrade = "Level 200", studentMajor = "Maritime Logistics", academicStanding = "Honor Roll"),
            Member("MBR-205", "org_school", "Emmanuel Darko", "e.darko@student.rmu.edu.gh", "+233 55 555 6666", UserRole.STUDENT, "Nautical Science Level 100", "BONY-PASS:MBR-205:RMU", false, "ED", 74f, 1, studentGrade = "Level 100", studentMajor = "Nautical Studies", academicStanding = "Attendance Warning"),
            Member("MBR-206", "org_school", "Akosua Danquah", "a.danquah@student.rmu.edu.gh", "+233 26 666 7777", UserRole.STUDENT, "Computer Science Level 300", "BONY-PASS:MBR-206:RMU", true, "AD", 91f, 9, studentGrade = "Level 300", studentMajor = "Maritime Cybersecurity", academicStanding = "Good Standing")
        )
        _members.value = mList

        val coursesList = listOf(
            SchoolCourse("crs_1", "NAV-301", "Celestial & Radar Navigation", "Capt. Kwesi Mensah", "Sim Lab 2", "08:30 AM - 10:30 AM", 45, 95.0f, true),
            SchoolCourse("crs_2", "ENG-402", "Marine Diesel Propulsion & Turbine Systems", "Dr. E. Addo", "Engineering Hall B", "11:00 AM - 01:00 PM", 38, 92.4f, true),
            SchoolCourse("crs_3", "LOG-205", "Port Automation & Container Logistics", "Mrs. Grace Annan", "Maritime Business Hall", "01:30 PM - 03:30 PM", 30, 94.0f, true),
            SchoolCourse("crs_4", "SAF-101", "Safety of Life at Sea (SOLAS)", "Instructor Tetteh", "Swimming Bay & Pier 1", "04:00 PM - 05:30 PM", 60, 97.5f, false)
        )
        _schoolCourses.value = coursesList

        val sList = listOf(
            AttendanceSession("sess_rmu1", "org_school", "Celestial & Radar Navigation Simulation", "Nautical Science", "Today", "08:30 AM - 10:30 AM", "Sim Lab 2", 45, true),
            AttendanceSession("sess_rmu2", "org_school", "Marine Propulsion Workshop", "Marine Engineering", "Today", "11:00 AM - 01:00 PM", "Engineering Hall B", 38, false),
            AttendanceSession("sess_rmu3", "org_school", "Port Automation Lecture", "Logistics", "Today", "01:30 PM - 03:30 PM", "Business Center 1", 30, false)
        )
        _sessions.value = sList
        _activeSession.value = sList[0]

        _records.value = listOf(
            AttendanceRecord("rec_rmu1", "sess_rmu1", "Celestial & Radar Navigation", "MBR-203", "Kofi Boateng", "Marine Engineering Level 300", "08:26 AM", AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN, SyncState.SYNCED, null, "Bridge simulator clearance", "11223344"),
            AttendanceRecord("rec_rmu2", "sess_rmu1", "Celestial & Radar Navigation", "MBR-204", "Abena Osei", "Ports & Shipping Level 200", "08:29 AM", AttendanceStatus.PRESENT, VerificationMethod.FACIAL_ID, SyncState.SYNCED, 0.991f, "Facial match verified", "55667788"),
            AttendanceRecord("rec_rmu3", "sess_rmu1", "Celestial & Radar Navigation", "MBR-206", "Akosua Danquah", "Computer Science Level 300", "08:35 AM", AttendanceStatus.LATE, VerificationMethod.QR_SCAN, SyncState.SYNCED, null, "Late arrival", "9900aabb")
        )

        _announcements.value = listOf(
            Announcement("ann_rmu1", "org_school", "RMU Mandatory Cadet & Student Muster Parade", "All nautical and engineering cadets are required to punch QR pass by 08:00 AM for attendance grading.", "Bony Visuals (Administrator)", "Today, 07:30 AM", true, "All Students"),
            Announcement("ann_rmu2", "org_school", "Regional Maritime University Cloud Attendance Gate Active", "High-speed biometric checkpoints activated at Main Gate and Sim Complex.", "Bony Visuals", "Yesterday", false, "All")
        )
        _auditLogs.value = listOf(
            AuditLogEntry("log_rmu1", "08:26:14", "Bridge Sim Scanner", "Leader / Teacher / Supervisor", "ATTENDANCE_CHECKIN", "Kofi Boateng verified via QR Code Scan at RMU"),
            AuditLogEntry("log_rmu2", "08:29:40", "Portal Gate 1", "Administrator", "ATTENDANCE_CHECKIN", "Abena Osei authenticated via Face ID at RMU")
        )
    }

    private fun seedTemaPlastData() {
        val mList = listOf(
            Member("MBR-301", "org_enterprise", "Bony Visuals", "bony.visuals@temaplast.com", "+233 24 100 0001", UserRole.ADMIN, "Plant Administration", "BONY-PASS:MBR-301:TP", true, "BV", 100f, 40, jobTitle = "Operations Director & CTO", jobShift = "Executive Shift (07:30 - 17:00)", isClockedIn = true, lastClockInTime = "07:25 AM"),
            Member("MBR-302", "org_enterprise", "Yaw Asante", "y.asante@temaplast.com", "+233 20 888 9999", UserRole.TEACHER, "Extrusion Unit", "BONY-PASS:MBR-302:TP", true, "YA", 98f, 22, jobTitle = "Plant Superintendent", jobShift = "Morning Shift (07:00 - 15:30)", isClockedIn = true, lastClockInTime = "06:55 AM"),
            Member("MBR-303", "org_enterprise", "Priscilla Adomako", "p.adomako@temaplast.com", "+233 27 999 0000", UserRole.STUDENT, "Quality Control", "BONY-PASS:MBR-303:TP", true, "PA", 96f, 14, jobTitle = "Lead QC Inspector", jobShift = "Morning Shift (07:00 - 15:30)", isClockedIn = true, lastClockInTime = "07:02 AM"),
            Member("MBR-304", "org_enterprise", "Samuel Kwarteng", "s.kwarteng@temaplast.com", "+233 55 000 1111", UserRole.STUDENT, "Injection Molding", "BONY-PASS:MBR-304:TP", true, "SK", 91f, 8, jobTitle = "Molding Technician", jobShift = "Afternoon Swing Shift (15:00 - 23:30)", isClockedIn = false),
            Member("MBR-305", "org_enterprise", "Francisca Gyasi", "f.gyasi@temaplast.com", "+233 24 123 9999", UserRole.STUDENT, "Packaging & Warehouse", "BONY-PASS:MBR-305:TP", true, "FG", 93f, 10, jobTitle = "Warehouse Officer", jobShift = "Morning Shift (07:00 - 15:30)", isClockedIn = true, lastClockInTime = "06:50 AM")
        )
        _members.value = mList

        val shiftsList = listOf(
            JobShift("shf_tp1", "Morning Extrusion & Molding Shift", "07:00 AM - 03:30 PM", "Extrusion Line 1 & 2", "Yaw Asante", 25, 22, true),
            JobShift("shf_tp2", "Afternoon Blow Molding Shift", "03:00 PM - 11:30 PM", "Injection Plant", "Samuel Kwarteng", 18, 0, false),
            JobShift("shf_tp3", "Night Continuous Polymer Shift", "11:00 PM - 07:30 AM", "Raw Material Silos", "Night Superintendent", 12, 12, true)
        )
        _jobShifts.value = shiftsList

        val timecardsList = listOf(
            TimecardPunch("pnch_tp1", "MBR-301", "Bony Visuals", "Operations Director & CTO", "Morning Extrusion Shift", "07:25 AM", null, 5.0f, "Active Shift", true),
            TimecardPunch("pnch_tp2", "MBR-302", "Yaw Asante", "Plant Superintendent", "Morning Extrusion Shift", "06:55 AM", null, 5.5f, "Active Shift", true),
            TimecardPunch("pnch_tp3", "MBR-303", "Priscilla Adomako", "Lead QC Inspector", "Morning Extrusion Shift", "07:02 AM", null, 5.3f, "Active Shift", true),
            TimecardPunch("pnch_tp4", "MBR-305", "Francisca Gyasi", "Warehouse Officer", "Morning Extrusion Shift", "06:50 AM", null, 5.6f, "Active Shift", true)
        )
        _timecardPunches.value = timecardsList

        val sList = listOf(
            AttendanceSession("sess_tp1", "org_enterprise", "Morning Shift Line Clock-In & Safety Briefing", "Plant Operations", "Today", "07:00 AM - 07:45 AM", "Tema Plast Factory Gate 2", 90, true),
            AttendanceSession("sess_tp2", "org_enterprise", "Quality Assurance & Polymer Review", "Quality Control", "Today", "01:30 PM - 02:30 PM", "QC Lab Room 1", 16, false)
        )
        _sessions.value = sList
        _activeSession.value = sList[0]

        _records.value = listOf(
            AttendanceRecord("rec_tp1", "sess_tp1", "Morning Shift Line Clock-In", "MBR-301", "Bony Visuals", "Plant Administration", "07:25 AM", AttendanceStatus.PRESENT, VerificationMethod.FACIAL_ID, SyncState.SYNCED, 0.995f, "Executive check-in", "aabbccdd"),
            AttendanceRecord("rec_tp2", "sess_tp1", "Morning Shift Line Clock-In", "MBR-302", "Yaw Asante", "Extrusion Unit", "06:55 AM", AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN, SyncState.SYNCED, null, "Turnstile punch", "eeff0011"),
            AttendanceRecord("rec_tp3", "sess_tp1", "Morning Shift Line Clock-In", "MBR-303", "Priscilla Adomako", "Quality Control", "07:02 AM", AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN, SyncState.SYNCED, null, "Badge scan", "12345678")
        )

        _announcements.value = listOf(
            Announcement("ann_tp1", "org_enterprise", "Tema Plast Factory Safety & Shift Attendance Bonus", "All shift operators must clock in by 07:15 AM to qualify for the weekly production attendance incentive.", "Bony Visuals (Administrator)", "Today, 06:30 AM", true, "All Tema Plast Staff")
        )
        _auditLogs.value = listOf(
            AuditLogEntry("log_tp1", "07:25:02", "Tema Plast Main Gate Turnstile", "Administrator", "ATTENDANCE_CHECKIN", "Bony Visuals authenticated via Face ID")
        )
    }

    private fun seedChristApostolicChurchData() {
        val mList = listOf(
            Member("MBR-101", "org_church", "Bony Visuals", "bony.visuals@caci.org", "+233 24 100 0001", UserRole.ADMIN, "General Headquarters / Media", "BONY-PASS:MBR-101:CACI", true, "BV", 100f, 35),
            Member("MBR-102", "org_church", "Apostle Samuel Marfo", "s.marfo@caci.org", "+233 24 234 5678", UserRole.TEACHER, "Pastoral Board", "BONY-PASS:MBR-102:CACI", true, "SM", 99f, 28),
            Member("MBR-103", "org_church", "Deaconess Comfort Nyarko", "c.nyarko@caci.org", "+233 20 345 6789", UserRole.TEACHER, "Sanctuary Choir", "BONY-PASS:MBR-103:CACI", true, "CN", 96f, 15),
            Member("MBR-104", "org_church", "Joshua Ofori", "j.ofori@caci.org", "+233 27 456 7890", UserRole.STUDENT, "Youth Ministry", "BONY-PASS:MBR-104:CACI", true, "JO", 93f, 9),
            Member("MBR-105", "org_church", "Grace Frimpong", "g.frimpong@caci.org", "+233 55 567 8901", UserRole.STUDENT, "Sanctuary Choir", "BONY-PASS:MBR-105:CACI", true, "GF", 90f, 6),
            Member("MBR-106", "org_church", "Elder Evans Quaye", "e.quaye@caci.org", "+233 24 678 9012", UserRole.TEACHER, "Ushering Council", "BONY-PASS:MBR-106:CACI", true, "EQ", 97f, 16)
        )
        _members.value = mList

        val sList = listOf(
            AttendanceSession("sess_caci1", "org_church", "Sunday Glorious Divine Service", "Main Worship", "Today", "08:30 AM - 11:30 AM", "CACI National Cathedral Auditorium", 220, true),
            AttendanceSession("sess_caci2", "org_church", "Midweek Prayer & Miracle Service", "Prayer & Word", "Wednesday", "06:00 PM - 08:00 PM", "Auditorium Hall 2", 110, false)
        )
        _sessions.value = sList
        _activeSession.value = sList[0]

        _records.value = listOf(
            AttendanceRecord("rec_caci1", "sess_caci1", "Sunday Glorious Divine Service", "MBR-101", "Bony Visuals", "General Headquarters / Media", "08:15 AM", AttendanceStatus.PRESENT, VerificationMethod.FACIAL_ID, SyncState.SYNCED, 0.999f, "Administrator check-in", "7a8f9c1e"),
            AttendanceRecord("rec_caci2", "sess_caci1", "Sunday Glorious Divine Service", "MBR-102", "Apostle Samuel Marfo", "Pastoral Board", "08:20 AM", AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN, SyncState.SYNCED, null, "Presiding Apostle badge verified", "b4c2e1f0"),
            AttendanceRecord("rec_caci3", "sess_caci1", "Sunday Glorious Divine Service", "MBR-103", "Deaconess Comfort Nyarko", "Sanctuary Choir", "08:25 AM", AttendanceStatus.PRESENT, VerificationMethod.QR_SCAN, SyncState.SYNCED, null, "Choir rehearsal logged", "f9e8d7c6")
        )

        _announcements.value = listOf(
            Announcement("ann_caci1", "org_church", "Christ Apostolic Church International National Thanksgiving & Anointing Service", "All congregants and department workers please check in by 8:15 AM via digital QR badge or facial scanner.", "Bony Visuals (Administrator)", "Today, 07:00 AM", true)
        )

        _auditLogs.value = listOf(
            AuditLogEntry("log_caci1", "08:15:10", "Main Cathedral Entrance", "Administrator", "ATTENDANCE_CHECKIN", "Bony Visuals checked in via Facial Recognition")
        )
    }
}
