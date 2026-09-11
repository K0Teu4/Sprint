package ru.sprint.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sprint.app.data.db.entity.TaskEntity
import ru.sprint.app.ui.common.TaskItem

@Composable
fun SearchScreen(
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilters by remember { mutableStateOf(setOf<String>()) }

    val filteredTasks = remember(tasks, searchQuery, selectedFilters) {
        tasks.filter { task ->
            (searchQuery.isEmpty() || task.title.contains(searchQuery, ignoreCase = true)) &&
            (selectedFilters.isEmpty() || selectedFilters.contains(task.type))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Поле поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(ru.sprint.app.R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Фильтры
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("Работа", "Хобби", "Личное", "Покупки")) { filter ->
                FilterChip(
                    selected = selectedFilters.contains(filter),
                    onClick = {
                        selectedFilters = if (selectedFilters.contains(filter)) {
                            selectedFilters - filter
                        } else {
                            selectedFilters + filter
                        }
                    },
                    label = { Text(filter) }
                )
            }
        }

        // Результаты поиска
        if (filteredTasks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Задач не найдено")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredTasks) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}
