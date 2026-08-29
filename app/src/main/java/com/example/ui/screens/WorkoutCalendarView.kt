package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkoutWithDetails
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.PeachLight
import com.example.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutCalendarView(
    workouts: List<WorkoutWithDetails>,
    selectedDate: String?, // Format: "yyyy-MM-dd"
    onSelectDate: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthTitleFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    // Map workouts by "yyyy-MM-dd"
    val workoutsByDate = remember(workouts) {
        workouts.groupBy { item ->
            val ts = item.workout.startedAt.toLongOrNull() ?: 0L
            if (ts > 0) dayKeyFormat.format(Date(ts)) else ""
        }
    }

    // Days in current month grid
    val calendarDays = remember(currentCalendarMonth.get(Calendar.YEAR), currentCalendarMonth.get(Calendar.MONTH)) {
        val cal = currentCalendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        // Find day of week for 1st of month (1=Sun, 2=Mon... in Java Calendar)
        // Convert to Monday = 0, Sunday = 6
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        
        val daysList = mutableListOf<CalendarDay>()
        
        // Previous month filler days
        val prevMonthCal = cal.clone() as Calendar
        prevMonthCal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
        for (i in 0 until firstDayOfWeek) {
            val dateStr = dayKeyFormat.format(prevMonthCal.time)
            daysList.add(
                CalendarDay(
                    dayOfMonth = prevMonthCal.get(Calendar.DAY_OF_MONTH),
                    dateStr = dateStr,
                    isCurrentMonth = false
                )
            )
            prevMonthCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Current month days
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDaysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = dayKeyFormat.format(cal.time)
            daysList.add(
                CalendarDay(
                    dayOfMonth = day,
                    dateStr = dateStr,
                    isCurrentMonth = true
                )
            )
        }

        // Trailing days to fill 5 or 6 weeks (multiples of 7)
        val remaining = (7 - (daysList.size % 7)) % 7
        val nextMonthCal = cal.clone() as Calendar
        nextMonthCal.add(Calendar.DAY_OF_MONTH, 1)
        for (i in 0 until remaining) {
            val dateStr = dayKeyFormat.format(nextMonthCal.time)
            daysList.add(
                CalendarDay(
                    dayOfMonth = nextMonthCal.get(Calendar.DAY_OF_MONTH),
                    dateStr = dateStr,
                    isCurrentMonth = false
                )
            )
            nextMonthCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        daysList
    }

    val todayStr = remember { dayKeyFormat.format(Date()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        val newCal = currentCalendarMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        currentCalendarMonth = newCal
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = TextDark)
                }

                Text(
                    text = monthTitleFormat.format(currentCalendarMonth.time),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )

                IconButton(
                    onClick = {
                        val newCal = currentCalendarMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        currentCalendarMonth = newCal
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of Week Names
            val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar Grid
            val rows = calendarDays.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { day ->
                            val hasWorkout = (workoutsByDate[day.dateStr]?.isNotEmpty() == true)
                            val isSelected = (selectedDate == day.dateStr)
                            val isToday = (day.dateStr == todayStr)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> CoralPrimary
                                            hasWorkout -> Color(0xFFFFF1EB)
                                            isToday -> Color(0xFFF1F5F9)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                        color = if (isToday && !isSelected) CoralPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            onSelectDate(null)
                                        } else {
                                            onSelectDate(day.dateStr)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${day.dayOfMonth}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || hasWorkout || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            !day.isCurrentMonth -> Color(0xFFCBD5E1)
                                            hasWorkout -> CoralPrimary
                                            else -> TextDark
                                        }
                                    )

                                    if (hasWorkout) {
                                        Text(
                                            text = "🐾",
                                            fontSize = 9.sp,
                                            lineHeight = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Filter indicator pill if selected
            AnimatedVisibility(visible = selectedDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF7ED))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filtering by date: $selectedDate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CoralPrimary
                        )
                    }

                    IconButton(
                        onClick = { onSelectDate(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear Filter",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class CalendarDay(
    val dayOfMonth: Int,
    val dateStr: String,
    val isCurrentMonth: Boolean
)
