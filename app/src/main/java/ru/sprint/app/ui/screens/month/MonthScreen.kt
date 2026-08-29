package ru.sprint.app.ui.screens.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.sprint.app.SprintApplication
import ru.sprint.app.data.db.entity.TaskEntity
import ru.sprint.app.domain.model.Category
import ru.sprint.app.ui.common.AddTaskSheet
import ru.sprint.app.ui.screens.month.components.MonthCalendarGrid
import ru.sprint.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthScreen(viewModel: MonthViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SprintApplication
    var showAdd by remember { mutableStateOf(false) }

    val title = state.yearMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
        .replaceFirstChar { it.uppercase() }
    val percent = if (state.totalTasks == 0) 0 else (state.doneTasks * 100 / state.totalTasks).coerceIn(0, 100)
    val today = LocalDate.now()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, "Новая задача") }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(padding).verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Мой месяц", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        if (state.yearMonth == YearMonth.from(today)) "Сегодня — ${today.dayOfMonth} ${today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("ru"))}"
                        else "План на ${state.yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("ru"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = viewModel::prevMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Предыдущий месяц") }
                IconButton(onClick = viewModel::nextMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Следующий месяц") }
            }

            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Прогресс месяца", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${state.doneTasks} выполнено из ${state.totalTasks}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("$percent%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                }
            }

            SectionTitle("Календарь")
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                MonthCalendarGrid(state.days, Modifier.padding(14.dp))
            }

            SectionTitle("Фокус месяца", "${state.monthlyTasks.size} задач")
            if (state.monthlyTasks.isEmpty()) {
                EmptyMonthCard(onAdd = { showAdd = true })
            } else {
                state.monthlyTasks.forEach { task ->
                    MonthlyTaskCard(
                        task = task,
                        onToggle = { viewModel.toggleMonthly(task.id, !task.isDone) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
            Spacer(Modifier.height(88.dp))
        }
    }

    if (showAdd) {
        AddTaskSheet(
            initialDate = today,
            onDismiss = { showAdd = false },
            onSave = { form ->
                scope.launch {
                    val ym = if (form.period == "month") state.yearMonth else YearMonth.from(form.date)
                    app.repository.addTask(
                        TaskEntity(
                            title = form.title.trim(), details = form.details,
                            date = DateUtils.startOfDayMillis(form.date), dueTime = form.dueTimeMinutes,
                            isRecurring = form.isRecurring, period = form.period,
                            monthYear = ym.year * 100 + ym.monthValue,
                            priority = form.priority, category = form.category.key
                        )
                    )
                }
                showAdd = false
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun EmptyMonthCard(onAdd: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.DateRange, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Добавьте большую цель", style = MaterialTheme.typography.titleMedium)
                Text("Например: прочитать 2 книги за месяц", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onAdd) { Text("Добавить") }
        }
    }
}

@Composable
private fun MonthlyTaskCard(task: TaskUi, onToggle: () -> Unit, onDelete: () -> Unit) {
    val cat = Category.fromKey(task.category)
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp, 42.dp).clip(RoundedCornerShape(4.dp)).background(cat.color))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(cat.displayName, style = MaterialTheme.typography.labelSmall, color = cat.color)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (task.isDone) cat.color else MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                    if (task.isDone) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
