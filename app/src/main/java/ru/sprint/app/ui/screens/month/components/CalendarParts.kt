package ru.sprint.app.ui.screens.month.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.sprint.app.ui.screens.month.DayCell

@Composable
fun MonthCalendarGrid(days: List<DayCell>, modifier: Modifier = Modifier) {
    val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekdays.forEachIndexed { index, day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index >= 5) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(
                Modifier.fillMaxWidth().padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { cell ->
                    DayCellView(cell, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCellView(cell: DayCell, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val countAlpha = when (cell.taskCount) {
        0 -> 0f
        1 -> 0.16f
        2 -> 0.28f
        else -> 0.42f
    }
    val muted = !cell.isCurrentMonth
    Box(
        modifier = modifier
            .aspectRatio(1.08f)
            .clip(RoundedCornerShape(13.dp))
            .background(if (countAlpha > 0f) accent.copy(alpha = countAlpha) else Color.Transparent)
            .then(
                if (cell.isToday) Modifier.border(1.5.dp, accent, RoundedCornerShape(13.dp))
                else Modifier
            )
            .padding(5.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    muted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    cell.isToday -> accent
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (cell.taskCount > 0 && !muted) {
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}
