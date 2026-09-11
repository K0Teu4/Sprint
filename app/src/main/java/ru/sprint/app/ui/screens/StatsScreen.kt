package ru.sprint.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

data class DailyStats(val day: DayOfWeek, val completed: Int, val total: Int)

@Composable
fun StatsScreen(
    weeklyStats: List<DailyStats>,
    modifier: Modifier = Modifier
) {
    val totalCompleted = weeklyStats.sumOf { it.completed }
    val totalTasks = weeklyStats.sumOf { it.total }
    val completionRate = if (totalTasks > 0) (totalCompleted.toFloat() / totalTasks * 100) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Выполнено на этой неделе: /",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Процент выполнения: %",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "По дням недели:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weeklyStats) { stats ->
                val progress = if (stats.total > 0) stats.completed.toFloat() / stats.total else 0f
                Column {
                    Text(
                        text = ": /",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
        }
    }
}
