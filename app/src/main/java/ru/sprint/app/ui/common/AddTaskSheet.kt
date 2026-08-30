package ru.sprint.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.sprint.app.domain.model.Category
import ru.sprint.app.util.DateUtils
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskSheet(
    initialDate: java.time.LocalDate = java.time.LocalDate.now(),
    initialForm: TaskFormState? = null,
    onDismiss: () -> Unit,
    onSave: (TaskFormState) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var form by remember { mutableStateOf(initialForm ?: TaskFormState(date = initialDate)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val isMonth = form.period == "month"

    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = DateUtils.startOfDayMillis(form.date))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        form = form.copy(date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = picker) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(if (initialForm == null) "Новая задача" else "Изменить задачу", style = MaterialTheme.typography.headlineMedium)
            Text("Соберите задачу за несколько секунд", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Название") },
                placeholder = { Text("Что нужно сделать?") },
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.details,
                onValueChange = { form = form.copy(details = it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                label = { Text("Комментарий") },
                placeholder = { Text("Детали, ссылка или заметка") },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(18.dp))
            FieldLabel("Тип")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("На день", form.period == "day") { form = form.copy(period = "day") }
                ChoiceChip("На месяц", form.period == "month") { form = form.copy(period = "month") }
            }

            if (!isMonth) {
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Повторять каждый день", style = MaterialTheme.typography.titleMedium)
                        Text("Задача появится в каждом дне", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = form.isRecurring, onCheckedChange = { form = form.copy(isRecurring = it) })
                }
                Spacer(Modifier.height(18.dp))
                FieldLabel("Время")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimePresets.common.filter { it.endsWith(":00") }.forEach { preset ->
                        val minutes = DateUtils.timeStringToMinutes(preset)
                        ChoiceChip(preset, form.dueTimeMinutes == minutes) {
                            form = form.copy(dueTimeMinutes = if (form.dueTimeMinutes == minutes) null else minutes)
                        }
                    }
                    ChoiceChip("Без времени", form.dueTimeMinutes == null) { form = form.copy(dueTimeMinutes = null) }
                }
                Spacer(Modifier.height(18.dp))
                FieldLabel("Дата")
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Text(DateUtils.formatDayHeader(form.date))
                }
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel("Приоритет")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Обычный", 1 to "Важно", 2 to "Срочно").forEach { (value, label) ->
                    ChoiceChip(label, form.priority == value) { form = form.copy(priority = value) }
                }
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel("Категория")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Category.values().forEach { cat ->
                    val selected = form.category == cat
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) cat.color.copy(alpha = .13f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(if (selected) 1.5.dp else 0.dp, if (selected) cat.color else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { form = form.copy(category = cat) }.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.padding(end = 7.dp).size(8.dp).clip(CircleShape).background(cat.color))
                        Text(cat.displayName, style = MaterialTheme.typography.labelLarge, color = if (selected) cat.color else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (form.isValid) onSave(form) },
                enabled = form.isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("Сохранить") }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}
