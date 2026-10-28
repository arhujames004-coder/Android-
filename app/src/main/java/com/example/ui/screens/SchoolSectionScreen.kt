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
import com.example.model.SchoolCourse
import com.example.ui.AttendanceViewModel
import com.example.ui.theme.*

@Composable
fun SchoolSectionScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.schoolCourses.collectAsState()
    val members by viewModel.members.collectAsState()
    val records by viewModel.records.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var selectedCourseForRoster by remember { mutableStateOf<SchoolCourse?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter students
    val students = members.filter { it.role.name == "STUDENT" }
    val honorRollStudents = students.filter { it.attendanceRate >= 94f }
    val warningStudents = students.filter { it.attendanceRate < 85f }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("school_section_screen"),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddCourseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_course_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Academic Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_overview_card"),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(8.dp).size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Academic & School Module",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Class Timetables, Periods & Student Academic Standing",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Semester 1",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Academic Quick KPI Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                SchoolKpiItem("Active Courses", "${courses.size}", MaterialTheme.colorScheme.primary)
                                VerticalDivider(modifier = Modifier.height(32.dp))
                                SchoolKpiItem("Enrolled Students", "${students.size}", MaterialTheme.colorScheme.onSurface)
                                VerticalDivider(modifier = Modifier.height(32.dp))
                                SchoolKpiItem("Honor Roll (≥94%)", "${honorRollStudents.size}", StatusPresent)
                                VerticalDivider(modifier = Modifier.height(32.dp))
                                SchoolKpiItem("Attendance Alert", "${warningStudents.size}", if (warningStudents.isEmpty()) StatusPresent else StatusAbsent)
                            }
                        }
                    }
                }

                // Academic Attendance Intervention Alert if needed
                if (warningStudents.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("academic_warning_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = StatusAbsent)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Academic Attendance Intervention Needed",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "${warningStudents.joinToString { it.name }} have dropped below minimum 85% attendance required for lab exam clearance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Header: Scheduled Classes & Courses
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Timetables & Classes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${courses.size} Scheduled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Courses List
                items(courses) { course ->
                    CourseCard(
                        course = course,
                        onViewRoster = { selectedCourseForRoster = course }
                    )
                }

                // Section Header: Student Academic Standing Directory
                item {
                    Text(
                        text = "Student Academic Roster & Standing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(students) { student ->
                    StudentAcademicRow(
                        student = student,
                        onMarkQuickPresent = {
                            activeSession?.let { sess ->
                                viewModel.markManual(student, AttendanceStatus.PRESENT, "Marked from School section")
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        var code by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var instructor by remember { mutableStateOf("") }
        var room by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("09:00 AM - 10:30 AM") }
        var enrolled by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Add Academic Course / Period") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Course Code (e.g. PHY-401)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("course_code_input")
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Course Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("course_title_input")
                    )
                    OutlinedTextField(
                        value = instructor,
                        onValueChange = { instructor = it },
                        label = { Text("Instructor / Professor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Classroom / Hall") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Period Time Window") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = enrolled,
                        onValueChange = { enrolled = it },
                        label = { Text("Enrolled Students Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.isNotBlank() && title.isNotBlank()) {
                            viewModel.addSchoolCourse(
                                code = code.trim().uppercase(),
                                title = title.trim(),
                                instructor = if (instructor.isNotBlank()) instructor.trim() else "Faculty Staff",
                                room = if (room.isNotBlank()) room.trim() else "Lecture Hall 1",
                                time = time.trim(),
                                enrolled = enrolled.toIntOrNull() ?: 30
                            )
                            showAddCourseDialog = false
                        }
                    },
                    enabled = code.isNotBlank() && title.isNotBlank(),
                    modifier = Modifier.testTag("confirm_add_course_button")
                ) {
                    Text("Add Course")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Course Roster Dialog
    selectedCourseForRoster?.let { course ->
        AlertDialog(
            onDismissRequest = { selectedCourseForRoster = null },
            title = {
                Column {
                    Text("${course.courseCode} Roster", fontWeight = FontWeight.Bold)
                    Text(course.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Instructor: ${course.instructor} • Room: ${course.room}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Schedule: ${course.scheduleTime} • Rate: ${course.attendanceRate}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusPresent,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Enrolled Students (${students.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(students) { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(st.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${st.departmentGroup} • ${st.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${st.attendanceRate.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (st.attendanceRate >= 90) StatusPresent else StatusLate
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedCourseForRoster = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SchoolKpiItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CourseCard(
    course: SchoolCourse,
    onViewRoster: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_card_${course.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = course.courseCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${course.instructor} • ${course.room} • ${course.scheduleTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${course.enrolledStudentsCount} Students Enrolled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Attendance Rate: ${course.attendanceRate}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusPresent
                    )
                }
            }

            OutlinedButton(
                onClick = onViewRoster,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("view_roster_${course.id}")
            ) {
                Text("Roster", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StudentAcademicRow(
    student: Member,
    onMarkQuickPresent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_row_${student.id}"),
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
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.avatarInitials,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${student.studentGrade ?: student.departmentGroup} • ${student.studentMajor ?: "General Studies"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (student.academicStanding) {
                        "Honor Roll" -> StatusPresent.copy(alpha = 0.15f)
                        "Attendance Warning" -> StatusAbsent.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = student.academicStanding ?: "Good Standing",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (student.academicStanding) {
                            "Honor Roll" -> StatusPresent
                            "Attendance Warning" -> StatusAbsent
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onMarkQuickPresent,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Check In", fontSize = 11.sp)
                }
            }
        }
    }
}
