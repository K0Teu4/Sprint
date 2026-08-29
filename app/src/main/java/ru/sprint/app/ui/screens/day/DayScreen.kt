package ru.sprint.app.ui.screens.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.sprint.app.SprintApplication
import ru.sprint.app.data.db.entity.TaskEntity
import ru.sprint.app.ui.common.AddTaskSheet
import ru.sprint.app.ui.screens.day.components.TaskRow
import ru.sprint.app.ui.screens.day.components.WeekDateSelector
import ru.sprint.app.util.DateUtils
import java.time.YearMonth

@Composable
fun DayScreen(viewModel: DayViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    val scope = rememberCoroutineScope()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SprintApplication
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { msg ->
            if (snackbar.showSnackbar(msg, actionLabel = "Отменить") == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, shape = CircleShape) { Icon(Icons.Filled.Add, "Новая задача") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("План дня", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(state.headerText, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        if (state.totalCount == 0) "Свободный день" else "${state.doneCount} из ${state.totalCount} выполнено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.totalCount > 0) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::prevWeek) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Предыдущая неделя") }
                WeekDateSelector(state.weekDates, state.selectedDate, viewModel::selectDate, Modifier.weight(1f))
                IconButton(onClick = viewModel::nextWeek) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Следующая неделя") }
            }
            if (state.tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Здесь пока пусто", style = MaterialTheme.typography.titleLarge)
                        Text("Добавьте первую задачу на этот день", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskRow(task, { viewModel.toggleDone(task, !task.isDone) }, { editing = task }, { viewModel.deleteTask(task) })
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        val edit = editing
        AddTaskSheet(
            initialDate = state.selectedDate,
            initialForm = edit?.toForm(),
            onDismiss = { showAdd = false; editing = null },
            onSave = { form ->
                if (edit != null) viewModel.updateTask(edit.id, form)
                else scope.launch {
                    val ym = YearMonth.from(form.date)
                    app.repository.addTask(TaskEntity(
                        title = form.title.trim(), details = form.details,
                        date = DateUtils.startOfDayMillis(form.date), dueTime = form.dueTimeMinutes,
                        isRecurring = form.isRecurring, period = form.period,
                        monthYear = ym.year * 100 + ym.monthValue,
                        priority = form.priority, category = form.category.key
                    ))
                }
                showAdd = false; editing = null
            }
        )
    }
}
