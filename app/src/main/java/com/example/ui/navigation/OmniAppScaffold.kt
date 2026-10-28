package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Organization
import com.example.model.UserRole
import com.example.ui.AttendanceViewModel
import com.example.ui.ScannerMode
import com.example.ui.screens.*

enum class AppDestination(val label: String, val icon: ImageVector) {
    INTRO("Intro", Icons.Default.Info),
    DASHBOARD("Overview", Icons.Default.Dashboard),
    SCANNER("Register", Icons.Default.QrCodeScanner),
    SCHOOL("School", Icons.Default.School),
    JOB("Job Shifts", Icons.Default.Work),
    MEMBERS("Roster", Icons.Default.People),
    REPORTS("Reports", Icons.Default.BarChart),
    MY_PASS("Pass", Icons.Default.Badge),
    BACKEND("Backend", Icons.Default.Dns),
    SECURITY("Security", Icons.Default.Shield),
    ALERTS("Alerts", Icons.Default.Notifications)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniAppScaffold(
    viewModel: AttendanceViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var backStack by remember { mutableStateOf(listOf(AppDestination.DASHBOARD)) }

    val currentOrg by viewModel.currentOrg.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbars when userMessage changes
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    fun navigateTo(dest: AppDestination) {
        if (dest != currentDestination) {
            backStack = backStack + dest
            currentDestination = dest
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack = backStack.dropLast(1)
        currentDestination = backStack.lastOrNull() ?: AppDestination.DASHBOARD
    }

    var showOrgDropdown by remember { mutableStateOf(false) }
    var showRoleDropdown by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.testTag("omni_app_scaffold"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "BonyVisuals ADT Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isOffline) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (isOffline) "OFFLINE" else "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOffline) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Org Selector Clickable
                            Row(
                                modifier = Modifier
                                    .clickable { showOrgDropdown = true }
                                    .testTag("open_org_selector"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = currentOrg.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        // Role Switcher Pill
                        AssistChip(
                            onClick = { showRoleDropdown = true },
                            label = { Text(currentRole.title.substringBefore(" "), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (currentRole) {
                                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                        UserRole.TEACHER -> Icons.Default.School
                                        UserRole.STUDENT -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.testTag("role_switcher_chip")
                        )

                        // App Tour & Introduction Guide
                        IconButton(
                            onClick = { navigateTo(AppDestination.INTRO) },
                            modifier = Modifier.testTag("intro_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "App Introduction & Guide"
                            )
                        }

                        // Dark mode toggle
                        IconButton(
                            onClick = onToggleDarkTheme,
                            modifier = Modifier.testTag("dark_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }

                        // Notification alert bell
                        IconButton(
                            onClick = { navigateTo(AppDestination.ALERTS) },
                            modifier = Modifier.testTag("alerts_bell_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    val urgentCount = announcements.count { it.isUrgent }
                                    if (urgentCount > 0) {
                                        Badge { Text("$urgentCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                            }
                        }

                        // Organization Dropdown Menu
                        DropdownMenu(
                            expanded = showOrgDropdown,
                            onDismissRequest = { showOrgDropdown = false }
                        ) {
                            Text(
                                text = "Switch Environment:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            viewModel.repository.sampleOrganizations.forEach { org ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(org.name, fontWeight = if (org.id == currentOrg.id) FontWeight.Bold else FontWeight.Normal)
                                            Text("${org.type.displayName} • ${org.code}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchOrganization(org)
                                        showOrgDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (org.type) {
                                                com.example.model.OrgType.SCHOOL -> Icons.Default.School
                                                com.example.model.OrgType.ENTERPRISE -> Icons.Default.Work
                                                com.example.model.OrgType.CHURCH -> Icons.Default.Church
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }
                        }

                        // Role Dropdown Menu
                        DropdownMenu(
                            expanded = showRoleDropdown,
                            onDismissRequest = { showRoleDropdown = false }
                        ) {
                            Text(
                                text = "Switch Access Role:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(role.title, fontWeight = if (role == currentRole) FontWeight.Bold else FontWeight.Normal)
                                            Text(role.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchRole(role)
                                        showRoleDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (role) {
                                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                                UserRole.TEACHER -> Icons.Default.School
                                                UserRole.STUDENT -> Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // Adaptive Bottom Navigation: Scrollable Tab Row for phones to fit all modules without clipping!
                if (!isWideScreen) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar")
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = AppDestination.values().indexOf(currentDestination).coerceAtLeast(0),
                            edgePadding = 8.dp,
                            divider = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppDestination.values().forEach { dest ->
                                Tab(
                                    selected = currentDestination == dest,
                                    onClick = { navigateTo(dest) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label, modifier = Modifier.size(20.dp)) },
                                    text = { Text(dest.label, fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Wide Screen: Navigation Rail for tablets/desktop
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier
                            .testTag("navigation_rail")
                            .fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(8.dp))
                        AppDestination.values().forEach { dest ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { navigateTo(dest) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, fontSize = 10.sp) },
                                modifier = Modifier.testTag("rail_nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                    VerticalDivider()
                }

                // Main Content View (centered and responsive)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1200.dp)
                    ) {
                        when (currentDestination) {
                            AppDestination.INTRO -> WelcomeIntroScreen(
                                viewModel = viewModel,
                                onGetStarted = { navigateTo(AppDestination.DASHBOARD) },
                                onNavigateToScanner = { navigateTo(AppDestination.SCANNER) },
                                onNavigateToBackend = { navigateTo(AppDestination.BACKEND) }
                            )

                            AppDestination.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToScanner = { mode ->
                                    viewModel.setScannerMode(mode)
                                    navigateTo(AppDestination.SCANNER)
                                },
                                onNavigateToReports = { navigateTo(AppDestination.REPORTS) },
                                onNavigateToSchool = { navigateTo(AppDestination.SCHOOL) },
                                onNavigateToJob = { navigateTo(AppDestination.JOB) },
                                onNavigateToIntro = { navigateTo(AppDestination.INTRO) }
                            )

                            AppDestination.SCANNER -> ScannerRegisterScreen(
                                viewModel = viewModel
                            )

                            AppDestination.SCHOOL -> SchoolSectionScreen(
                                viewModel = viewModel
                            )

                            AppDestination.JOB -> JobSectionScreen(
                                viewModel = viewModel
                            )

                            AppDestination.MEMBERS -> MembersScreen(
                                viewModel = viewModel
                            )

                            AppDestination.REPORTS -> ReportsScreen(
                                viewModel = viewModel
                            )

                            AppDestination.MY_PASS -> MyPassScreen(
                                viewModel = viewModel
                            )

                            AppDestination.BACKEND -> AdminBackendScreen(
                                viewModel = viewModel
                            )

                            AppDestination.SECURITY -> SecurityAuditScreen(
                                viewModel = viewModel
                            )

                            AppDestination.ALERTS -> AnnouncementsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
