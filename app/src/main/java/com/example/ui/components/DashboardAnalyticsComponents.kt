package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun DashboardHeroCard(
    org: Organization,
    activeSession: AttendanceSession?,
    attendeesCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Visual Hero Banner Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_attendance_hero_1793061383261),
                    contentDescription = "Attendance System Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x660F172A))
                )

                // Organization tag & code
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = org.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x99000000)
                    ) {
                        Text(
                            text = org.code,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Active session pill
                activeSession?.let { sess ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0F172A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusPresent)
                            )
                            Text(
                                text = "Live: ${sess.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Stats row
            val expected = activeSession?.expectedAttendees ?: org.totalMembers
            val percentage = if (expected > 0) ((attendeesCount.toFloat() / expected) * 100).toInt() else 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = "Present Today",
                    value = attendeesCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                StatItem(
                    label = "Expected",
                    value = expected.toString(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                StatItem(
                    label = "Turnout Rate",
                    value = "$percentage%",
                    color = if (percentage >= 80) StatusPresent else StatusLate
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusBreakdownBar(
    records: List<AttendanceRecord>,
    modifier: Modifier = Modifier
) {
    val total = records.size.coerceAtLeast(1)
    val present = records.count { it.status == AttendanceStatus.PRESENT }
    val late = records.count { it.status == AttendanceStatus.LATE }
    val excused = records.count { it.status == AttendanceStatus.EXCUSED }
    val absent = records.count { it.status == AttendanceStatus.ABSENT }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_breakdown_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Attendance Composition",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${records.size} logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // Multi-segment progress bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                val w = size.width
                val h = size.height

                val presentW = (present.toFloat() / total) * w
                val lateW = (late.toFloat() / total) * w
                val excusedW = (excused.toFloat() / total) * w
                val absentW = (absent.toFloat() / total) * w

                var curX = 0f
                if (presentW > 0) {
                    drawRect(StatusPresent, Offset(curX, 0f), Size(presentW, h))
                    curX += presentW
                }
                if (lateW > 0) {
                    drawRect(StatusLate, Offset(curX, 0f), Size(lateW, h))
                    curX += lateW
                }
                if (excusedW > 0) {
                    drawRect(StatusExcused, Offset(curX, 0f), Size(excusedW, h))
                    curX += excusedW
                }
                if (absentW > 0) {
                    drawRect(StatusAbsent, Offset(curX, 0f), Size(absentW, h))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem("Present ($present)", StatusPresent)
                LegendItem("Late ($late)", StatusLate)
                LegendItem("Excused ($excused)", StatusExcused)
                LegendItem("Absent ($absent)", StatusAbsent)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WeeklyTrendsBarChart(
    modifier: Modifier = Modifier
) {
    val weeklyData = listOf(
        Pair("Mon", 85f),
        Pair("Tue", 92f),
        Pair("Wed", 78f),
        Pair("Thu", 94f),
        Pair("Fri", 88f),
        Pair("Sat", 65f),
        Pair("Sun", 98f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_trends_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Engagement Velocity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time presence metrics across sessions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Avg 86%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bar chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val w = size.width
                val h = size.height
                val barCount = weeklyData.size
                val slotWidth = w / barCount
                val barWidth = slotWidth * 0.45f
                val maxVal = 100f

                // Draw 80% threshold line
                val targetY = h - ((80f / maxVal) * h)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.25f),
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    strokeWidth = 1.dp.toPx()
                )

                weeklyData.forEachIndexed { index, pair ->
                    val barHeight = (pair.second / maxVal) * (h - 20.dp.toPx())
                    val x = (index * slotWidth) + (slotWidth - barWidth) / 2
                    val y = h - barHeight - 16.dp.toPx()

                    val isTopDay = pair.second >= 94f
                    val barColor = if (isTopDay) Color(0xFF10B981) else Color(0xFF0D9488).copy(alpha = 0.75f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // X-Axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weeklyData.forEach { pair ->
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
