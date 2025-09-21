package com.example.projectalpha.ui.screen.habits

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectalpha.viewmodel.HabitsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

private val CELL_SPACING_MONTHLY = 3.dp

@Composable
fun HabitContributionGraphMonthly(
    habitId: Int,
    habitName: String,
    habitsViewModel: HabitsViewModel
) {
    val displayMonth: YearMonth by habitsViewModel.currentDisplayGraphMonth.collectAsState()
    val completionDatesSet: Set<LocalDate> by habitsViewModel.getHabitCompletionHistoryForMonth(habitId)
        .collectAsState(initial = emptySet())

    val firstDayOfMonth = displayMonth.atDay(1)
    val lastDayOfMonth = displayMonth.atEndOfMonth()
    val firstDayOfWeekSystem = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    val daysToSubtractForGridStart = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeekSystem.value + 7) % 7
    val firstCellDate = firstDayOfMonth.minusDays(daysToSubtractForGridStart.toLong())

    val daysToAddForGridEnd = (firstDayOfWeekSystem.value + 6 - lastDayOfMonth.dayOfWeek.value + 7) % 7
    val lastCellDateInGrid = lastDayOfMonth.plusDays(daysToAddForGridEnd.toLong())
    val numWeeks = ChronoUnit.WEEKS.between(firstCellDate, lastCellDateInGrid).toInt() + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp), // Overall padding for the graph component
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { habitsViewModel.showPreviousMonthForGraph() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = { habitsViewModel.showNextMonthForGraph() },
                enabled = displayMonth.isBefore(YearMonth.now())
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = if (displayMonth.isBefore(YearMonth.now())) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Day of Week Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CELL_SPACING_MONTHLY)
        ) {
            val weekDays = (0..6).map { firstDayOfWeekSystem.plus(it.toLong()) }
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))


        // Grid of Days
        Column(verticalArrangement = Arrangement.spacedBy(CELL_SPACING_MONTHLY)) {
            (0 until numWeeks).forEach { weekIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(CELL_SPACING_MONTHLY)) {
                    (0..6).forEach { dayIndexInWeek ->
                        val currentDate = firstCellDate.plusWeeks(weekIndex.toLong()).plusDays(dayIndexInWeek.toLong())
                        if (!currentDate.isAfter(lastCellDateInGrid)) {
                            val belongsToThisMonth = YearMonth.from(currentDate) == displayMonth
                            MonthlyDayCell(
                                date = currentDate,
                                isCompleted = completionDatesSet.contains(currentDate),
                                isCurrentMonth = belongsToThisMonth,
                                isToday = currentDate.isEqual(LocalDate.now())
                            )
                        } else {
                            Spacer(Modifier.weight(1f).aspectRatio(1f)) // Maintain grid structure
                        }
                    }
                }
            }
        }

        if (displayMonth != YearMonth.now()) {
            TextButton(
                onClick = { habitsViewModel.resetGraphMonthToCurrent() },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text("Go to Current Month")
            }
        }
    }
}

@Composable
private fun MonthlyDayCell(
    date: LocalDate,
    isCompleted: Boolean,
    isCurrentMonth: Boolean,
    isToday: Boolean
) {
    val cellColor = when {
        !isCurrentMonth -> MaterialTheme.colorScheme.surfaceColorAtElevation(0.5.dp).copy(alpha = 0.5f)
        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = .8f)
        isToday && isCurrentMonth -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val borderColor = if (isToday && isCurrentMonth) MaterialTheme.colorScheme.secondary else Color.Transparent
    val textColor = when {
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        isToday && isCurrentMonth -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(cellColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = isCurrentMonth) {},
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 9.sp,
            color = textColor,
            fontWeight = if (isToday && isCurrentMonth) FontWeight.Bold else FontWeight.Normal
        )
    }
}
