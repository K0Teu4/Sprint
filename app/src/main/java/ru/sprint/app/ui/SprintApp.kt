package ru.sprint.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.sprint.app.ReminderReceiver
import ru.sprint.app.data.TaskDao
import ru.sprint.app.data.TaskEntity
import ru.sprint.app.widget.SprintWidgetProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

private val Bg = Color(0xFF080A09)
private val Surface = Color(0xFF101311)
private val Elevated = Color(0xFF171A18)
private val TextPrimary = Color(0xFFF3F4EF)
private val TextSecondary = Color(0xFF92988F)
private val Line = Color(0xFF242925)
private val Mint = Color(0xFFBFE8D0)
private val MintDeep = Color(0xFF244936)
private val Violet = Color(0xFFC7B5FF)
private val Pink = Color(0xFFF1A8CF)
private val Danger = Color(0xFFFFA8A8)
private val Gradient = Brush.linearGradient(listOf(Mint, Violet, Pink))
private val GradientDark = Brush.linearGradient(listOf(Color(0xFF274B3A), Color(0xFF30284C), Color(0xFF4A2A40)))
private val ru = Locale("ru")
private val en = Locale.ENGLISH
private val ruWeekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val enWeekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val categoryKeys = listOf("ALL", "WORK", "HOBBY", "PERSONAL", "SHOPPING")

private class UiText(val english: Boolean) {
    fun v(ru: String, en: String) = if (english) en else ru
    fun month(date: LocalDate): String = date.month.getDisplayName(TextStyle.FULL, if (english) en else ru).replaceFirstChar { it.uppercase() }
    fun weekday(i: Int) = (if (english) enWeekdays else ruWeekdays)[i]
    fun category(key: String) = when (key) {
        "WORK" -> v("Работа", "Work")
        "HOBBY" -> v("Хобби", "Hobby")
        "SHOPPING" -> v("Покупки", "Shopping")
        else -> v("Личное", "Personal")
    }
}

@Composable private fun rememberUiText(english: Boolean): UiText = remember(english) { UiText(english) }

@Composable
fun SprintApp(dao: TaskDao, openQuickAdd: Boolean = false, onQuickAddConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val root = LocalView.current
    val scope = rememberCoroutineScope()
    val tasks by dao.observeAll().collectAsState(emptyList())
    var screen by rememberSaveable { mutableStateOf("week") }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editorOpen by remember { mutableStateOf(false) }
    var quickAddOpen by rememberSaveable { mutableStateOf(openQuickAdd) }
    var detailedDraft by remember { mutableStateOf<TaskEntity?>(null) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var subtaskParent by remember { mutableStateOf<TaskEntity?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var onboarding by remember { mutableStateOf(!prefs(context).getBoolean("onboarding", false)) }
    var notifications by rememberSaveable { mutableStateOf(prefs(context).getBoolean("notifications", true)) }
    var haptics by rememberSaveable { mutableStateOf(prefs(context).getBoolean("haptics", true)) }
    var english by rememberSaveable { mutableStateOf(prefs(context).getBoolean("english", false)) }
    var showCompleted by rememberSaveable { mutableStateOf(prefs(context).getBoolean("show_completed", true)) }
    var categoryFilter by rememberSaveable { mutableStateOf("ALL") }
    LaunchedEffect(openQuickAdd) { if (openQuickAdd) { quickAddOpen = true; onQuickAddConsumed() } }
    val ui = rememberUiText(english)
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch { runCatching { exportTasks(context, uri, tasks) }.onSuccess { snackbarHostState.showSnackbar(ui.v("Резервная копия сохранена", "Backup saved")) }.onFailure { snackbarHostState.showSnackbar(ui.v("Не удалось сохранить файл", "Could not save file")) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching { importTasks(context, uri) }.onSuccess { imported ->
                if (imported.isEmpty()) snackbarHostState.showSnackbar(ui.v("Файл не содержит задач", "The file contains no tasks"))
                else {
                    val count = restoreImportedTasks(context, dao, imported, notifications)
                    SprintWidgetProvider.refresh(context)
                    snackbarHostState.showSnackbar(ui.v("Импортировано: $count", "Imported: $count"))
                }
            }.onFailure { snackbarHostState.showSnackbar(ui.v("Не удалось прочитать файл", "Could not read file")) }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { notifications = false; prefs(context).edit().putBoolean("notifications", false).apply() }
    }

    val colors = darkColorScheme(background = Bg, surface = Surface, surfaceContainer = Surface, surfaceContainerHigh = Elevated, onBackground = TextPrimary, onSurface = TextPrimary, onSurfaceVariant = TextSecondary, outline = Line, primary = Mint, onPrimary = Color(0xFF102018), primaryContainer = MintDeep, onPrimaryContainer = Mint, secondary = Violet, tertiary = Pink, error = Danger)
    MaterialTheme(colorScheme = colors, typography = Typography().copy(
        headlineLarge = Typography().headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.25).sp),
        headlineMedium = Typography().headlineMedium.copy(fontSize = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.6).sp),
        titleLarge = Typography().titleLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp), bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp))) {
        if (onboarding) {
            Onboarding(english) { onboarding = false; prefs(context).edit().putBoolean("onboarding", true).apply(); if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            return@MaterialTheme
        }
        Scaffold(containerColor = Bg, snackbarHost = { SnackbarHost(snackbarHostState) }, bottomBar = { FloatingNav(screen, ui) { screen = it } }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(targetState = screen, transitionSpec = { (fadeIn(tween(170)) + scaleIn(initialScale = .985f, animationSpec = tween(170))) togetherWith fadeOut(tween(110)) }, label = "screen-transition") { target ->
                    when (target) {
                        "week" -> WeekScreen(tasks, selectedDate, ui, showCompleted, categoryFilter, { categoryFilter = it }, { selectedDate = it }, { d -> selectedDate = LocalDate.parse(selectedDate).plusWeeks(d.toLong()).toString() }, { quickAddOpen = true }, { searchOpen = true },
                            edit = { editing = it; subtaskParent = null; editorOpen = true },
                            addSubtask = { subtaskParent = it; editing = null; selectedDate = it.date; editorOpen = true },
                            toggle = { task -> if (haptics) root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); scope.launch { val done = !task.completed; dao.update(task.copy(completed = done, completedAt = if (done) System.currentTimeMillis() else null)); if (done) cancelReminder(context, task) else if (notifications) schedule(context, task); SprintWidgetProvider.refresh(context) } },
                            delete = { task -> scope.launch {
                                val descendants = tasks.filter { it.parentId == task.id }
                                dao.delete(task); descendants.forEach { dao.delete(it) }; cancelReminder(context, task); descendants.forEach { cancelReminder(context, it) }
                                val result = snackbarHostState.showSnackbar(ui.v("Дело удалено", "Task deleted"), ui.v("Отменить", "Undo"), duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) {
                                    val id = dao.insert(task.copy(id = 0L, seriesId = null))
                                    val restoredRoot = task.copy(id = id, seriesId = if (task.recurrence != "NONE") id else null)
                                    dao.update(restoredRoot)
                                    if (notifications) schedule(context, restoredRoot)
                                }
                                SprintWidgetProvider.refresh(context)
                            } },
                            add = { quickAddOpen = true })
                        "month" -> MonthScreen(tasks, selectedDate, ui, { selectedDate = it }, { selectedDate = it; screen = "week" })
                        "year" -> YearScreen(tasks, selectedDate, ui, { selectedDate = it })
                        else -> SettingsScreen(ui, notifications, haptics, english, showCompleted, { enabled ->
                            notifications = enabled; prefs(context).edit().putBoolean("notifications", enabled).apply()
                            if (enabled) { if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else tasks.filter { it.reminder && !it.completed }.forEach { schedule(context, it) } }
                            else tasks.forEach { cancelReminder(context, it) }
                        }, { haptics = it; prefs(context).edit().putBoolean("haptics", it).apply() }, { english = it; prefs(context).edit().putBoolean("english", it).apply(); SprintWidgetProvider.refresh(context) }, { showCompleted = it; prefs(context).edit().putBoolean("show_completed", it).apply() },
                            export = { exportLauncher.launch("Sprint-backup-${LocalDate.now()}.json") }, import = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }, resetOnboarding = { onboarding = true; prefs(context).edit().putBoolean("onboarding", false).apply() }, clear = { scope.launch { dao.deleteAll(); tasks.forEach { cancelReminder(context, it) }; SprintWidgetProvider.refresh(context); snackbarHostState.showSnackbar(ui.v("Все задачи удалены", "All tasks deleted")) } })
                    }
                }
            }
        }
        if (quickAddOpen) QuickAddSheet(ui, selectedDate, haptics, onDismiss = { quickAddOpen = false }, onDetailed = { draft -> quickAddOpen = false; editing = null; subtaskParent = null; detailedDraft = draft; editorOpen = true }) { draft ->
            scope.launch { saveNewTask(context, dao, draft, notifications); selectedDate = draft.date; quickAddOpen = false; SprintWidgetProvider.refresh(context) }
        }
        if (editorOpen) TaskEditor(existing = editing, draft = detailedDraft, parent = subtaskParent, defaultDate = selectedDate, notificationsEnabled = notifications, hapticsEnabled = haptics, ui = ui, onDismiss = { editorOpen = false; subtaskParent = null; detailedDraft = null }) { task ->
            scope.launch {
                if (task.id == 0L) {
                    saveNewTask(context, dao, task, notifications)
                } else {
                    cancelReminder(context, task)
                    if (task.parentId == null) {
                        val oldSeries = tasks.filter { it.seriesId == task.id && it.id != task.id }
                        oldSeries.forEach { dao.delete(it); cancelReminder(context, it) }
                        val saved = task.copy(seriesId = if (task.recurrence != "NONE") task.id else null)
                        dao.update(saved)
                        if (notifications) schedule(context, saved)
                        if (task.recurrence != "NONE") recurring(task).forEach { occurrence ->
                            val occurrenceId = dao.insert(occurrence.copy(recurrence = "NONE", seriesId = task.id))
                            if (notifications) schedule(context, occurrence.copy(id = occurrenceId, recurrence = "NONE", seriesId = task.id))
                        }
                    } else {
                        dao.update(task.copy(seriesId = task.seriesId, recurrence = "NONE"))
                        if (notifications) schedule(context, task)
                    }
                }
                SprintWidgetProvider.refresh(context)
            }
            selectedDate = task.date; editorOpen = false; subtaskParent = null; detailedDraft = null
        }
        if (searchOpen) SearchSheet(tasks, ui, { searchOpen = false }) { editing = it; subtaskParent = null; editorOpen = true; searchOpen = false }
    }
}

private fun prefs(context: Context) = context.getSharedPreferences("sprint", Context.MODE_PRIVATE)

@Composable private fun Onboarding(english: Boolean, done: () -> Unit) {
    val ui = rememberUiText(english); var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf(ui.v("Месяц. День. Действие.", "Month. Day. Action."), ui.v("Планируй спокойно.", "Plan calmly."), ui.v("Быстро добавляй. Ясно выполняй.", "Add fast. Finish clearly."))
    val bodies = listOf(ui.v("Смотри на месяц целиком, выбирай день и переходи к конкретным делам.", "See the whole month, choose a day, then move straight to the work."), ui.v("Приоритеты, повторения, напоминания и заметки работают вместе — без скрытых уровней.", "Priorities, repeats, reminders and notes work together without hidden layers."), ui.v("Быстрое добавление для простых задач. Подзадачи — только там, где действительно нужны.", "Quick add for simple tasks. Subtasks only where they are actually needed."))
    Box(Modifier.fillMaxSize().background(Bg).drawBehind { drawCircle(Mint.copy(alpha = .08f), 250.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .9f, size.height * .1f)); drawCircle(Violet.copy(alpha = .065f), 280.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .82f)) }) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = done, modifier = Modifier.align(Alignment.End)) { Text(ui.v("Пропустить", "Skip"), color = TextSecondary) }
            AnimatedContent(page, label = "onboarding") { p -> Column { Box(Modifier.size(70.dp).clip(RoundedCornerShape(22.dp)).background(Gradient), Alignment.Center) { Text("S", color = Color(0xFF111412), fontSize = 34.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(28.dp)); Text(titles[p], style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(12.dp)); Text(bodies[p], color = TextPrimary.copy(alpha = .94f), fontSize = 16.sp, lineHeight = 24.sp); Spacer(Modifier.height(28.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { i -> Box(Modifier.width(if (i == p) 24.dp else 6.dp).height(6.dp).clip(CircleShape).background(if (i == p) Mint else Line)) } } } }
            GradientButton(ui.v(if (page < 2) "Дальше" else "Начать", if (page < 2) "Continue" else "Start")) { if (page < 2) page++ else done() }
        }
    }
}

@Composable private fun FloatingNav(screen: String, ui: UiText, select: (String) -> Unit) {
    val items = listOf("week" to ui.v("Неделя", "Week"), "month" to ui.v("Месяц", "Month"), "year" to ui.v("Год", "Year"), "settings" to ui.v("Настройки", "Settings"))
    Box(Modifier.fillMaxWidth().background(Bg.copy(alpha = .98f)).padding(horizontal = 14.dp, vertical = 7.dp)) { Row(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(26.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(26.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { items.forEach { (id, label) -> val selected = screen == id; val width by animateDpAsState(if (selected) 94.dp else 58.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "nav-width"); Box(Modifier.width(width).fillMaxHeight().clip(RoundedCornerShape(21.dp)).background(if (selected) Gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))).clickable { select(id) }.semantics { contentDescription = label; role = Role.Tab }, Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { NavGlyph(id, if (selected) Color(0xFF101311) else TextSecondary); AnimatedVisibility(selected, enter = fadeIn(tween(140)) + scaleIn(), exit = fadeOut(tween(90)) + scaleOut()) { Text(label, color = Color(0xFF101311), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp)) } } } } } }
}

@Composable private fun NavGlyph(id: String, color: Color) { Canvas(Modifier.size(21.dp)) { val s = 1.8.dp.toPx(); when (id) { "week" -> { drawCircle(color, 6.dp.toPx(), style = Stroke(s)); drawCircle(color, 2.dp.toPx()) }; "month" -> { drawRoundRect(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()), androidx.compose.ui.geometry.Size(16.dp.toPx(), 16.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(s)); drawLine(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 7.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 7.dp.toPx()), s) }; "year" -> { drawRoundRect(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 3.dp.toPx()), androidx.compose.ui.geometry.Size(15.dp.toPx(), 15.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(s)); drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 8.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.Offset(8.dp.toPx(), 18.dp.toPx()), s) }; else -> { drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 6.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 6.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 11.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 11.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 16.dp.toPx()), androidx.compose.ui.geometry.Offset(14.dp.toPx(), 16.dp.toPx()), s) } } } }

@Composable private fun WeekScreen(tasks: List<TaskEntity>, selected: String, ui: UiText, showCompleted: Boolean, categoryFilter: String, setCategoryFilter: (String) -> Unit, onDate: (String) -> Unit, onWeekSwipe: (Int) -> Unit, add: () -> Unit, search: () -> Unit, edit: (TaskEntity) -> Unit, addSubtask: (TaskEntity) -> Unit, toggle: (TaskEntity) -> Unit, delete: (TaskEntity) -> Unit) {
    val date = LocalDate.parse(selected); val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); val weekDates = (0..6).map { monday.plusDays(it.toLong()).toString() }
    val dayTasks = tasks.filter { it.date == selected }; val roots = dayTasks.filter { it.parentId == null }; val completed = dayTasks.count { it.completed }; val visibleRoots = if (showCompleted) roots else roots.filterNot { it.completed }; val categoryRoots = if (categoryFilter == "ALL") visibleRoots else visibleRoots.filter { it.category == categoryFilter }
    var secondaryFilter by rememberSaveable(selected) { mutableIntStateOf(0) }
    val filteredRoots = when (secondaryFilter) { 1 -> categoryRoots.filter { it.priority >= 2 }; 2 -> categoryRoots.filter { it.reminder }; else -> categoryRoots }
    val children = dayTasks.filter { it.parentId != null }.groupBy { it.parentId }
    val display = buildList { filteredRoots.sortedWith(taskComparator()).forEach { root -> add(root); if (showCompleted) addAll(children[root.id].orEmpty().sortedWith(taskComparator())) else addAll(children[root.id].orEmpty().filterNot { it.completed }.sortedWith(taskComparator())) } }
    LazyColumn(Modifier.pointerInput(monday) { var drag = 0f; detectHorizontalDragGestures(onDragEnd = { if (abs(drag) > 80f) onWeekSwipe(if (drag < 0) 1 else -1); drag = 0f }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount }) } .drawBehind { drawCircle(Mint.copy(alpha = .035f), 190.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .96f, 70.dp.toPx())); drawCircle(Violet.copy(alpha = .022f), 220.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .03f, size.height * .5f)) }, contentPadding = PaddingValues(bottom = 26.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(ui.v("НЕДЕЛЯ", "WEEK"), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp); Text("${date.dayOfMonth} ${ui.month(date)}", style = MaterialTheme.typography.headlineLarge); Text(weekRange(monday, ui), color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp)) }; if (date != LocalDate.now()) TextButton({ onDate(LocalDate.now().toString()) }) { Text(ui.v("Сегодня", "Today"), color = Mint) }; IconButton(search, modifier = Modifier.size(44.dp)) { SearchGlyph(TextSecondary) }; Spacer(Modifier.width(2.dp)); GradientIconButton(add) } }
        item { WeekStrip(monday, date, tasks, ui, onDate) }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(if (dayTasks.isEmpty()) ui.v("Свободный день", "Free day") else "${dayTasks.size - completed} ${ui.v("активных", "active")}", fontWeight = FontWeight.SemiBold, fontSize = 18.sp); Text(if (dayTasks.isEmpty()) ui.v("Добавь одно дело — остальные не обязательны.", "Add one thing — the rest can wait.") else ui.v("$completed из ${dayTasks.size} выполнено", "$completed of ${dayTasks.size} completed"), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)) }; if (dayTasks.isNotEmpty()) Box(Modifier.size(50.dp), Alignment.Center) { CircularProgressIndicator(progress = { completed.toFloat() / dayTasks.size }, modifier = Modifier.fillMaxSize(), strokeWidth = 4.dp, color = Mint, trackColor = Line); Text("${completed * 100 / dayTasks.size}%", fontSize = 9.sp, fontWeight = FontWeight.Bold) } } }
        item { CategoryChips(ui, categoryFilter, setCategoryFilter) }
        if (dayTasks.isNotEmpty()) item { Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { FilterChip(secondaryFilter == 0, { secondaryFilter = 0 }, label = { Text(ui.v("Все", "All")) }); FilterChip(secondaryFilter == 1, { secondaryFilter = 1 }, label = { Text(ui.v("Важные", "Important")) }); FilterChip(secondaryFilter == 2, { secondaryFilter = 2 }, label = { Text(ui.v("С напоминанием", "Reminders")) }) } }
        item { Spacer(Modifier.height(7.dp)) }
        if (display.isEmpty()) item { EmptyState(if (dayTasks.isEmpty()) ui.v("Пока ничего нет", "Nothing planned") else ui.v("Нет дел в этом фильтре", "No tasks in this filter"), if (dayTasks.isEmpty()) ui.v("Нажми + и введи задачу одной строкой.", "Tap + and enter a task in one line.") else ui.v("Выбери другой тип или фильтр.", "Choose another type or filter.")) }
        else items(display, key = { it.id }) { task -> TaskRow(task, ui, edit, addSubtask, toggle, delete, children[task.id].orEmpty().size) }
    }
}

@Composable
private fun WeekStrip(monday: LocalDate, selected: LocalDate, tasks: List<TaskEntity>, ui: UiText, onDate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (0..6).forEach { index ->
            val date = monday.plusDays(index.toLong())
            val selectedDay = date == selected
            val today = date == LocalDate.now()
            val hasTasks = tasks.any { it.date == date.toString() }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp)).background(if (selectedDay) GradientDark else Elevated).clickable { onDate(date.toString()) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ui.weekday(index), color = if (selectedDay) TextPrimary else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Text(date.dayOfMonth.toString(), color = if (selectedDay) TextPrimary else if (today) Mint else TextPrimary, fontSize = 15.sp, fontWeight = if (selectedDay || today) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
                Box(Modifier.padding(top = 5.dp).size(4.dp).clip(CircleShape).background(if (hasTasks) Mint else Color.Transparent))
            }
        }
    }
}

private fun taskComparator(): Comparator<TaskEntity> = compareBy<TaskEntity> { it.completed }.thenByDescending { it.priority }.thenBy { it.time ?: "99:99" }.thenBy { it.id }

@Composable private fun CategoryChips(ui: UiText, selected: String, onSelected: (String) -> Unit, includeAll: Boolean = true) { val keys = if (includeAll) categoryKeys else categoryKeys.filterNot { it == "ALL" }; Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { keys.forEach { key -> FilterChip(selected = selected == key, onClick = { onSelected(key) }, label = { Text(if (key == "ALL") ui.v("Все", "All") else ui.category(key)) }) } } }

@Composable private fun TaskRow(task: TaskEntity, ui: UiText, edit: (TaskEntity) -> Unit, addSubtask: (TaskEntity) -> Unit, toggle: (TaskEntity) -> Unit, delete: (TaskEntity) -> Unit, childCount: Int) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }; val alpha by animateFloatAsState(if (task.completed) .48f else 1f, tween(220), label = "task-alpha"); val isChild = task.parentId != null; val priorityColor = when (task.priority) { 3 -> Danger; 2 -> Violet; else -> Line }
    Row(Modifier.fillMaxWidth().animateContentSize().clickable { edit(task) }.padding(start = if (isChild) 46.dp else 22.dp, end = 18.dp, top = if (isChild) 5.dp else 10.dp, bottom = if (isChild) 5.dp else 10.dp), verticalAlignment = Alignment.Top) {
        if (isChild) Box(Modifier.width(20.dp).height(36.dp).padding(end = 8.dp).drawBehind { drawLine(Line, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), 1.2.dp.toPx()); drawLine(Line, androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 1.2.dp.toPx()) })
        Box(Modifier.width(if (isChild) 34.dp else 44.dp).height(42.dp), Alignment.Center) { Box(Modifier.size(if (isChild) 19.dp else 24.dp).border(if (task.priority >= 2 && !task.completed) 2.dp else 1.6.dp, if (task.completed) Mint else priorityColor, CircleShape).clip(CircleShape).background(if (task.completed) Mint else Color.Transparent).clickable { toggle(task) }, Alignment.Center) { AnimatedVisibility(task.completed, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) { Text("✓", color = Color(0xFF111412), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
        Column(Modifier.weight(1f).graphicsLayer { this.alpha = alpha }.padding(top = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { if (!isChild && task.priority >= 2 && !task.completed) { Box(Modifier.size(if (task.priority == 3) 7.dp else 5.dp).clip(CircleShape).background(priorityColor)); Spacer(Modifier.width(7.dp)) }; Text(task.title, color = TextPrimary, fontSize = if (isChild) 13.sp else 15.sp, fontWeight = if (isChild) FontWeight.Normal else FontWeight.Medium, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            val meta = buildList { task.time?.takeIf { it.isNotBlank() }?.let { add(it) }; if (task.reminder) add(ui.v("напоминание", "reminder")); if (task.recurrence != "NONE") add(recurrenceLabel(task.recurrence, ui)); add(ui.category(task.category)) }.joinToString(" · ")
            Text(meta, color = TextSecondary, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (task.note.isNotBlank()) Text(task.note, color = TextSecondary.copy(alpha = .85f), fontSize = 11.sp, maxLines = if (isChild) 1 else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            if (!isChild && childCount > 0) Text(ui.v("Подзадачи: $childCount", "Subtasks: $childCount"), color = Violet.copy(alpha = .85f), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
        }
        IconButton({ menu = true }, modifier = Modifier.size(40.dp).semantics { contentDescription = ui.v("Действия для ${task.title}", "Actions for ${task.title}") }) { Text("•••", color = TextSecondary, fontSize = 13.sp) }
    }
    if (menu) AlertDialog(onDismissRequest = { menu = false }, title = { Text(if (isChild) ui.v("Подзадача", "Subtask") else ui.v("Действия", "Actions")) }, text = { Column { if (!isChild) TextButton({ menu = false; addSubtask(task) }, modifier = Modifier.fillMaxWidth()) { Text(ui.v("Добавить подзадачу", "Add subtask"), modifier = Modifier.fillMaxWidth()) }; TextButton({ menu = false; edit(task) }, modifier = Modifier.fillMaxWidth()) { Text(ui.v("Изменить", "Edit"), modifier = Modifier.fillMaxWidth()) }; if (task.date.isNotBlank()) TextButton({ menu = false; addToCalendar(context, task) }, modifier = Modifier.fillMaxWidth()) { Text(ui.v("Добавить в календарь", "Add to calendar"), modifier = Modifier.fillMaxWidth()) }; TextButton({ menu = false; delete(task) }, modifier = Modifier.fillMaxWidth()) { Text(ui.v("Удалить", "Delete"), color = Danger, modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { TextButton({ menu = false }) { Text(ui.v("Закрыть", "Close")) } })
}

@Composable private fun QuickAddSheet(ui: UiText, defaultDate: String, haptics: Boolean, onDismiss: () -> Unit, onDetailed: (TaskEntity) -> Unit, onSave: (TaskEntity) -> Unit) {
    val view = LocalView.current; var input by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("PERSONAL") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface, tonalElevation = 0.dp, dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 26.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(ui.v("Быстрое добавление", "Quick add"), style = MaterialTheme.typography.headlineMedium); Text(ui.v("Одна строка — и готово.", "One line and you're done."), color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) }; TextButton(onDismiss) { Text(ui.v("Закрыть", "Close"), color = TextSecondary) } }
            Spacer(Modifier.height(14.dp)); OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3, placeholder = { Text(ui.v("Например: Позвонить завтра в 18:00", "Example: Call tomorrow at 18:00")) }, shape = RoundedCornerShape(17.dp), colors = fieldColors())
            Spacer(Modifier.height(12.dp)); Text(ui.v("Тип", "Type"), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); CategoryChips(ui, category, { category = it }, includeAll = false)
            Spacer(Modifier.height(12.dp)); val p = parseQuickTask(input, defaultDate, category); if (input.isNotBlank()) { Text(ui.v("Будет создано", "Will create"), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold); Text(p.title, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp)); Text("${prettyDate(p.date, ui)}${p.time?.let { " · $it" } ?: ""} · ${ui.category(p.category)}${if (p.priority > 1) " · ${ui.v("важно", "priority")}" else ""}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
            Spacer(Modifier.height(18.dp)); TextButton({ if (p.title.isNotBlank()) onDetailed(p) }, modifier = Modifier.fillMaxWidth()) { Text(ui.v("Открыть полное редактирование", "Open full editor"), color = Mint) }; Spacer(Modifier.height(4.dp)); GradientButton(ui.v("Добавить", "Add")) { if (p.title.isNotBlank()) { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onSave(p) } }
        }
    }
}

private fun parseQuickTask(raw: String, defaultDate: String, forcedCategory: String): TaskEntity {
    var text = raw.trim(); var date = runCatching { LocalDate.parse(defaultDate) }.getOrElse { LocalDate.now() }; var time: String? = null; var priority = 1; var recurrence = "NONE"; var category = forcedCategory
    Regex("(?i)(?:\\b(?:в|at)\\s*)?\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(text)?.let { time = "${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}"; text = text.replace(it.value, " ") }
    val lower = text.lowercase(ru)
    when { "послезавтра" in lower || "day after tomorrow" in lower -> { date = date.plusDays(2); text = text.replace(Regex("(?i)послезавтра|day after tomorrow"), " ") }; "завтра" in lower || "tomorrow" in lower -> { date = date.plusDays(1); text = text.replace(Regex("(?i)завтра|tomorrow"), " ") }; "сегодня" in lower || "today" in lower -> { text = text.replace(Regex("(?i)сегодня|today"), " ") } }
    val weekdays = listOf("понедельник" to DayOfWeek.MONDAY, "вторник" to DayOfWeek.TUESDAY, "среда" to DayOfWeek.WEDNESDAY, "четверг" to DayOfWeek.THURSDAY, "пятница" to DayOfWeek.FRIDAY, "суббота" to DayOfWeek.SATURDAY, "воскресенье" to DayOfWeek.SUNDAY, "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY, "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY, "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY, "sunday" to DayOfWeek.SUNDAY)
    weekdays.firstOrNull { lower.contains(it.first) }?.let { pair -> date = date.with(TemporalAdjusters.nextOrSame(pair.second)); text = text.replace(Regex("(?i)\\b(?:в|во|on)\\s+${Regex.escape(pair.first)}\\b"), " "); text = text.replace(pair.first, " ", ignoreCase = true) }
    if (Regex("(?i)срочно|urgent|asap").containsMatchIn(text)) { priority = 3; text = text.replace(Regex("(?i)срочно|urgent|asap"), " ") } else if (Regex("(?i)важно|important").containsMatchIn(text)) { priority = 2; text = text.replace(Regex("(?i)важно|important"), " ") }
    when { Regex("(?i)каждый день|ежедневно|every day|daily").containsMatchIn(text) -> { recurrence = "DAILY"; text = text.replace(Regex("(?i)каждый день|ежедневно|every day|daily"), " ") }; Regex("(?i)каждую неделю|еженедельно|every week|weekly").containsMatchIn(text) -> { recurrence = "WEEKLY"; text = text.replace(Regex("(?i)каждую неделю|еженедельно|every week|weekly"), " ") }; Regex("(?i)каждый месяц|ежемесячно|every month|monthly").containsMatchIn(text) -> { recurrence = "MONTHLY"; text = text.replace(Regex("(?i)каждый месяц|ежемесячно|every month|monthly"), " ") } }
    val detected = when { Regex("(?i)\\bработа\\b|\\bwork\\b").containsMatchIn(text) -> "WORK"; Regex("(?i)\\bхобби\\b|\\bhobby\\b").containsMatchIn(text) -> "HOBBY"; Regex("(?i)\\bпокупки\\b|\\bshopping\\b").containsMatchIn(text) -> "SHOPPING"; Regex("(?i)\\bличное\\b|\\bpersonal\\b").containsMatchIn(text) -> "PERSONAL"; else -> null }
    if (detected != null) { category = detected; text = text.replace(Regex("(?i)\\bработа\\b|\\bwork\\b|\\bхобби\\b|\\bhobby\\b|\\bпокупки\\b|\\bshopping\\b|\\bличное\\b|\\bpersonal\\b"), " ") }
    return TaskEntity(title = text.replace(Regex("\\s+"), " ").trim().ifBlank { raw.trim() }, date = date.toString(), time = time, priority = priority, recurrence = recurrence, category = category)
}

@Composable private fun MonthScreen(tasks: List<TaskEntity>, selected: String, ui: UiText, onSelect: (String) -> Unit, onOpenWeek: (String) -> Unit) {
    var month by rememberSaveable { mutableStateOf(LocalDate.parse(selected).withDayOfMonth(1).toString()) }; val first = LocalDate.parse(month); val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); val days = (0 until 42).map { start.plusDays(it.toLong()) }; val today = LocalDate.now(); val monthTasks = tasks.filter { runCatching { LocalDate.parse(it.date).let { d -> d.month == first.month && d.year == first.year } }.getOrDefault(false) }; val planned = monthTasks.size; val active = monthTasks.count { !it.completed }; val done = monthTasks.count { it.completed }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).pointerInput(first) { var drag = 0f; detectHorizontalDragGestures(onDragEnd = { if (abs(drag) > 80f) month = first.plusMonths(if (drag < 0) 1 else -1).toString(); drag = 0f }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount }) }.padding(bottom = 20.dp).drawBehind { drawCircle(Violet.copy(alpha = .035f), 230.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .95f, 90.dp.toPx())); drawCircle(Mint.copy(alpha = .018f), 200.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .05f, size.height * .45f)) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(ui.v("КАЛЕНДАРЬ", "CALENDAR"), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp); Text("${ui.month(first)} ${first.year}", style = MaterialTheme.typography.headlineLarge) }; if (first.year != today.year || first.month != today.month) TextButton({ month = today.withDayOfMonth(1).toString(); onSelect(today.toString()) }) { Text(ui.v("Сегодня", "Today"), color = Mint) }; IconButton({ month = first.minusMonths(1).toString() }) { Text("‹", fontSize = 31.sp) }; IconButton({ month = first.plusMonths(1).toString() }) { Text("›", fontSize = 31.sp) } }
        Row(Modifier.padding(horizontal = 22.dp)) { (0..6).forEach { Text(ui.weekday(it), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }; Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 18.dp)) { days.chunked(7).forEach { row -> Row(Modifier.fillMaxWidth()) { row.forEach { date -> val inMonth = date.month == first.month && date.year == first.year; val isToday = date == today; val selectedDay = date.toString() == selected; val activeDay = tasks.any { it.date == date.toString() && !it.completed }; val doneDay = tasks.any { it.date == date.toString() && it.completed }; val brush = when { selectedDay -> GradientDark; isToday -> Brush.linearGradient(listOf(Mint.copy(alpha = .16f), Mint.copy(alpha = .05f))); else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) }; Column(Modifier.weight(1f).height(72.dp).padding(3.dp).clip(RoundedCornerShape(16.dp)).background(brush).clickable(enabled = inMonth) { onSelect(date.toString()) }.padding(8.dp)) { Text(date.dayOfMonth.toString(), color = if (!inMonth) TextSecondary.copy(alpha = .25f) else if (selectedDay) TextPrimary else if (isToday) Mint else TextPrimary, fontWeight = if (selectedDay || isToday) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp); Spacer(Modifier.weight(1f)); if (activeDay || doneDay) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { if (activeDay) Box(Modifier.size(5.dp).clip(CircleShape).background(if (selectedDay) Mint else Mint.copy(alpha = .85f))); if (doneDay) Box(Modifier.size(5.dp).clip(CircleShape).background(TextSecondary.copy(alpha = .45f))) } } } } } }
        Spacer(Modifier.height(16.dp)); Row(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MonthMetric(planned.toString(), ui.v("запланировано", "planned"), Mint, Modifier.weight(1f)); MonthMetric(active.toString(), ui.v("активных", "active"), Violet, Modifier.weight(1f)); MonthMetric(done.toString(), ui.v("выполнено", "done"), TextSecondary, Modifier.weight(1f)) }
        if (planned > 0) { val p = done.toFloat() / planned; Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(ui.v("Прогресс месяца", "Month progress"), color = TextSecondary, fontSize = 11.sp); Text("${(p * 100).toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }; Spacer(Modifier.height(7.dp)); Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Line)) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(Gradient)) } } }
        val selectedDate = runCatching { LocalDate.parse(selected) }.getOrNull(); if (selectedDate != null && selectedDate.month == first.month && selectedDate.year == first.year) { val selectedTasks = monthTasks.filter { it.date == selected }.sortedWith(taskComparator()); Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${selectedDate.dayOfMonth} ${ui.month(selectedDate)}", style = MaterialTheme.typography.titleLarge); Text(if (selectedTasks.isEmpty()) ui.v("Свободный день", "Free day") else "${selectedTasks.size} ${ui.v("дел", "tasks")}", color = TextSecondary, fontSize = 11.sp) }; TextButton({ onOpenWeek(selected) }) { Text(ui.v("Открыть неделю", "Open week"), color = Mint) } }; selectedTasks.take(4).forEach { task -> Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).clip(CircleShape).background(if (task.completed) TextSecondary else if (task.priority == 3) Danger else if (task.priority == 2) Violet else Mint)); Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp).weight(1f), color = if (task.completed) TextSecondary else TextPrimary); task.time?.let { Text(it, color = TextSecondary, fontSize = 10.sp) } } } } }
    }
}

@Composable private fun MonthMetric(value: String, label: String, color: Color, modifier: Modifier = Modifier) { Column(modifier.clip(RoundedCornerShape(17.dp)).background(Elevated).padding(13.dp)) { Text(value, fontSize = 19.sp, fontWeight = FontWeight.SemiBold); Text(label, color = color, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp)) } }

@Composable private fun YearScreen(tasks: List<TaskEntity>, selected: String, ui: UiText, onSelect: (String) -> Unit) { var year by rememberSaveable { mutableIntStateOf(LocalDate.parse(selected).year) }; val today = LocalDate.now(); LazyColumn(contentPadding = PaddingValues(bottom = 30.dp), modifier = Modifier.drawBehind { drawCircle(Violet.copy(alpha = .025f), 250.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .92f, 90.dp.toPx())) }) { item { Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(ui.v("ГОД", "YEAR"), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp); Text(year.toString(), style = MaterialTheme.typography.headlineLarge) }; if (year != today.year) TextButton({ year = today.year }) { Text(ui.v("Сегодня", "Today"), color = Mint) }; IconButton({ year-- }) { Text("‹", fontSize = 31.sp) }; IconButton({ year++ }) { Text("›", fontSize = 31.sp) } } }; items((1..12).toList()) { m -> YearMonthBlock(year, m, tasks, selected, ui, onSelect) } } }
@Composable private fun YearMonthBlock(year: Int, monthNumber: Int, tasks: List<TaskEntity>, selected: String, ui: UiText, onSelect: (String) -> Unit) { val first = LocalDate.of(year, monthNumber, 1); val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); val count = first.lengthOfMonth(); val weeks = ((start.until(first.plusDays(count.toLong() - 1)).days + 1 + 6) / 7); val days = (0 until weeks * 7).map { start.plusDays(it.toLong()) }; val monthTasks = tasks.filter { it.date.startsWith("%04d-%02d-".format(year, monthNumber)) }; Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(ui.month(first), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); Text("${monthTasks.size} ${ui.v("дел", "tasks")}", color = TextSecondary, fontSize = 10.sp) }; Spacer(Modifier.height(9.dp)); Row(Modifier.fillMaxWidth()) { (0..6).forEach { Text(ui.weekday(it).take(2), color = TextSecondary, fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }; days.chunked(7).forEach { row -> Row(Modifier.fillMaxWidth()) { row.forEach { date -> val inMonth = date.monthValue == monthNumber; val selectedDay = date.toString() == selected; val hasTask = tasks.any { it.date == date.toString() }; val isToday = date == LocalDate.now(); Box(Modifier.weight(1f).height(27.dp).padding(1.dp).clip(RoundedCornerShape(6.dp)).background(if (selectedDay) GradientDark else Color.Transparent).clickable(enabled = inMonth) { onSelect(date.toString()) }, Alignment.Center) { Text(date.dayOfMonth.toString(), color = if (!inMonth) TextSecondary.copy(alpha = .2f) else if (selectedDay) TextPrimary else if (isToday) Mint else TextSecondary, fontSize = 9.sp, fontWeight = if (isToday || selectedDay) FontWeight.Bold else FontWeight.Normal); if (hasTask && inMonth && !selectedDay) Box(Modifier.align(Alignment.BottomCenter).offset(y = (-2).dp).size(3.dp).clip(CircleShape).background(Mint)) } } } } } }

@Composable private fun SettingsScreen(ui: UiText, notifications: Boolean, haptics: Boolean, english: Boolean, showCompleted: Boolean, setNotifications: (Boolean) -> Unit, setHaptics: (Boolean) -> Unit, setEnglish: (Boolean) -> Unit, setShowCompleted: (Boolean) -> Unit, export: () -> Unit, import: () -> Unit, resetOnboarding: () -> Unit, clear: () -> Unit) { var confirm by remember { mutableStateOf(false) }; LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) { item { Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) { Text(ui.v("НАСТРОЙКИ", "SETTINGS"), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp); Text(ui.v("Настройки", "Settings"), style = MaterialTheme.typography.headlineLarge); Text(ui.v("Минимум параметров. Только то, что меняет поведение.", "Few settings. Only what changes behavior."), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } }; item { SettingBlock(ui.v("Язык", "Language")) { SettingRow(ui.v("Интерфейс", "Interface"), if (english) "English" else "Русский") { LanguageSwitch(english, setEnglish) } } }; item { SettingBlock(ui.v("Напоминания", "Reminders")) { SettingRow(ui.v("Уведомления", "Notifications"), if (notifications) ui.v("Напоминания о задачах", "Task reminders") else ui.v("Выключены", "Off")) { Switch(notifications, setNotifications) } } }; item { SettingBlock(ui.v("Поведение", "Behavior")) { SettingRow(ui.v("Микровибрация", "Haptic feedback"), ui.v("Лёгкий отклик при действиях", "Light feedback for actions")) { Switch(haptics, setHaptics) }; SettingRow(ui.v("Показывать выполненные", "Show completed"), ui.v("В списке недели", "In the week list")) { Switch(showCompleted, setShowCompleted) } } }; item { SettingBlock(ui.v("Данные", "Data")) { SettingAction(ui.v("Экспортировать резервную копию", "Export backup"), ui.v("JSON-файл со всеми задачами", "JSON file with all tasks"), export); SettingAction(ui.v("Импортировать задачи", "Import tasks"), ui.v("Добавит задачи из JSON к текущим", "Adds tasks from a JSON backup"), import); Text(ui.v("Удалить все задачи", "Delete all tasks"), color = Danger, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clickable { confirm = true }.padding(horizontal = 22.dp, vertical = 18.dp)) } }; item { SettingBlock(ui.v("Приложение", "App")) { SettingAction(ui.v("Показать знакомство заново", "Show onboarding again"), ui.v("Повторно открыть первый экран", "Open the introduction again"), resetOnboarding); Text("Sprint 1.3", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 15.dp)) } } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(ui.v("Удалить все задачи?", "Delete all tasks?")) }, text = { Text(ui.v("Это действие нельзя отменить.", "This action cannot be undone."), color = TextSecondary) }, confirmButton = { TextButton({ confirm = false; clear() }) { Text(ui.v("Удалить всё", "Delete all"), color = Danger) } }, dismissButton = { TextButton({ confirm = false }) { Text(ui.v("Отмена", "Cancel")) } }) }
@Composable private fun SettingAction(title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) }; Text("›", color = TextSecondary, fontSize = 22.sp) } }
@Composable private fun LanguageSwitch(english: Boolean, setEnglish: (Boolean) -> Unit) { Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Elevated).padding(3.dp)) { listOf(false to "RU", true to "EN").forEach { (value, label) -> Box(Modifier.clip(RoundedCornerShape(9.dp)).background(if (english == value) Gradient else Color.Transparent).clickable { setEnglish(value) }.padding(horizontal = 12.dp, vertical = 7.dp)) { Text(label, color = if (english == value) Color(0xFF101311) else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }
@Composable private fun SettingBlock(title: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 5.dp)) { Text(title.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)); content(); HorizontalDivider(color = Line) } }
@Composable private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp)) }; control() } }

@Composable private fun TaskEditor(existing: TaskEntity?, draft: TaskEntity?, parent: TaskEntity?, defaultDate: String, notificationsEnabled: Boolean, hapticsEnabled: Boolean, ui: UiText, onDismiss: () -> Unit, onSave: (TaskEntity) -> Unit) {
    val context = LocalContext.current; val view = LocalView.current; val initialDate = parent?.date ?: existing?.date?.ifBlank { defaultDate } ?: draft?.date ?: defaultDate; var title by remember { mutableStateOf(existing?.title ?: draft?.title ?: "") }; var note by remember { mutableStateOf(existing?.note ?: draft?.note ?: "") }; var date by remember { mutableStateOf(initialDate) }; var time by remember { mutableStateOf(existing?.time ?: draft?.time ?: "") }; var priority by remember { mutableIntStateOf(existing?.priority ?: draft?.priority ?: 1) }; var repeat by remember { mutableStateOf(if (parent != null) "NONE" else existing?.recurrence ?: draft?.recurrence ?: "NONE") }; var reminder by remember { mutableStateOf(existing?.reminder ?: draft?.reminder ?: false) }; var category by remember { mutableStateOf(existing?.category ?: draft?.category ?: parent?.category ?: "PERSONAL") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface, tonalElevation = 0.dp, dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 26.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(if (parent != null) ui.v("Новая подзадача", "New subtask") else if (existing == null) ui.v("Новое дело", "New task") else ui.v("Изменить дело", "Edit task"), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); TextButton(onDismiss) { Text(ui.v("Закрыть", "Close"), color = TextSecondary) } }
        if (parent != null) Text(ui.v("Внутри: ${parent.title}", "Inside: ${parent.title}"), color = Violet, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(14.dp)); OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(ui.v("Что нужно сделать?", "What needs to be done?")) }, shape = RoundedCornerShape(17.dp), colors = fieldColors())
        Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { OutlinedButton({ showDatePicker(context, date, ui.english) { date = it } }, modifier = Modifier.weight(1f).height(50.dp), RoundedCornerShape(15.dp)) { Text(prettyDate(date, ui), maxLines = 1, overflow = TextOverflow.Ellipsis) }; Column(Modifier.weight(1f)) { OutlinedButton({ showTimePicker(context, time, ui.english) { time = it } }, modifier = Modifier.fillMaxWidth().height(50.dp), RoundedCornerShape(15.dp)) { Text(time.ifBlank { ui.v("Без времени", "No time") }) }; if (time.isNotBlank()) TextButton({ time = "" }, modifier = Modifier.align(Alignment.End).height(30.dp)) { Text(ui.v("Убрать время", "Remove time"), color = TextSecondary, fontSize = 10.sp) } } }
        Spacer(Modifier.height(15.dp)); Text(ui.v("Тип", "Type"), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); CategoryChips(ui, category, { category = it }, includeAll = false)
        Spacer(Modifier.height(13.dp)); Text(ui.v("Приоритет", "Priority"), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); Chips(listOf(ui.v("Обычный", "Normal"), ui.v("Важный", "Important"), ui.v("Срочный", "Urgent")), priority - 1) { priority = it + 1 }; Text(ui.v("Срочные выше важных.", "Urgent comes before important."), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        if (parent == null) { Spacer(Modifier.height(14.dp)); Text(ui.v("Повторение", "Repeat"), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); Chips(listOf(ui.v("Нет", "None"), ui.v("Каждый день", "Daily"), ui.v("Каждую неделю", "Weekly"), ui.v("Каждый месяц", "Monthly")), listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").indexOf(repeat)) { repeat = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")[it] }; if (repeat != "NONE") Text(recurrenceDescription(repeat, ui), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(12.dp)); SettingRow(ui.v("Напоминание", "Reminder"), if (notificationsEnabled && time.isNotBlank()) "${ui.v("В", "At")} $time" else ui.v("Нужны дата и время", "Date and time required")) { Switch(checked = reminder && notificationsEnabled && time.isNotBlank(), onCheckedChange = { if (notificationsEnabled && time.isNotBlank()) reminder = it }) }
        Spacer(Modifier.height(8.dp)); OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp), label = { Text(ui.v("Заметка", "Note")) }, placeholder = { Text(ui.v("Детали, ссылка или короткая мысль", "Details, link or a short thought")) }, minLines = 3, maxLines = 5, shape = RoundedCornerShape(17.dp), colors = fieldColors())
        Spacer(Modifier.height(18.dp)); GradientButton(if (existing == null) ui.v("Добавить дело", "Add task") else ui.v("Сохранить", "Save")) { if (title.isNotBlank()) { if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onSave(TaskEntity(id = existing?.id ?: 0L, title = title.trim(), note = note.trim(), date = date, time = time.takeIf { it.isNotBlank() }, priority = priority, recurrence = repeat, reminder = reminder && notificationsEnabled && time.isNotBlank(), completed = existing?.completed ?: false, createdAt = existing?.createdAt ?: System.currentTimeMillis(), completedAt = existing?.completedAt, category = category, parentId = existing?.parentId ?: parent?.id, seriesId = existing?.seriesId)) } }
    } }
}

@Composable private fun SearchSheet(tasks: List<TaskEntity>, ui: UiText, onDismiss: () -> Unit, edit: (TaskEntity) -> Unit) { var query by rememberSaveable { mutableStateOf("") }; val results = remember(query, tasks) { if (query.isBlank()) tasks.take(12) else tasks.filter { it.title.contains(query, true) || it.note.contains(query, true) || it.category.contains(query, true) }.take(30) }; ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface, dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }) { Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp)) { Text(ui.v("Поиск", "Search"), style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(12.dp)); OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(ui.v("Найти дело или заметку", "Find a task or note")) }, shape = RoundedCornerShape(17.dp), colors = fieldColors()); Spacer(Modifier.height(10.dp)); if (results.isEmpty()) EmptyState(ui.v("Ничего не найдено", "Nothing found"), ui.v("Попробуй другое слово или фразу.", "Try another word or phrase.")) else LazyColumn(Modifier.heightIn(max = 430.dp)) { items(results, key = { it.id }) { task -> Column(Modifier.fillMaxWidth().clickable { edit(task) }.padding(vertical = 11.dp)) { Text(task.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${prettyDate(task.date, ui)}${task.time?.let { " · $it" } ?: ""} · ${ui.category(task.category)}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)); if (task.note.isNotBlank()) Text(task.note, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp)) } } } } } }

@Composable private fun GradientButton(text: String, onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp).drawBehind { drawRoundRect(Gradient, cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx())) }, shape = RoundedCornerShape(17.dp), color = Color.Transparent) { Box(contentAlignment = Alignment.Center) { Text(text, color = Color(0xFF111412), fontWeight = FontWeight.Bold) } } }
@Composable private fun GradientIconButton(onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.size(52.dp).drawBehind { drawCircle(Gradient) }, shape = CircleShape, color = Color.Transparent) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color(0xFF111412), fontSize = 28.sp, fontWeight = FontWeight.Light) } } }
@Composable private fun SearchGlyph(color: Color) { Canvas(Modifier.size(20.dp)) { drawCircle(color, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()), style = Stroke(1.8.dp.toPx())); drawLine(color, androidx.compose.ui.geometry.Offset(12.5.dp.toPx(), 12.5.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 18.dp.toPx()), 1.8.dp.toPx()) } }
@Composable private fun EmptyState(title: String, body: String) { Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp)) { Box(Modifier.size(10.dp).clip(CircleShape).background(Gradient)); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.titleLarge); Text(body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 5.dp)) } }
@Composable private fun Chips(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { labels.forEachIndexed { index, label -> FilterChip(selected = selected == index, onClick = { onSelected(index) }, label = { Text(label) }) } } }
@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Mint, unfocusedBorderColor = Line, cursorColor = Mint, focusedLabelColor = Mint)

private suspend fun restoreImportedTasks(context: Context, dao: TaskDao, imported: List<TaskEntity>, notifications: Boolean): Int {
    val oldToNew = mutableMapOf<Long, Long>()
    var count = 0
    val roots = imported.filter { it.parentId == null }.sortedWith(compareBy<TaskEntity> { if (it.recurrence != "NONE") 0 else 1 }.thenBy { it.date }.thenBy { it.id })
    for (source in roots) {
        val newId = dao.insert(source.copy(id = 0L, parentId = null, seriesId = null))
        oldToNew[source.id] = newId
        val restored = source.copy(id = newId, parentId = null, seriesId = if (source.recurrence != "NONE") newId else source.seriesId?.let { oldToNew[it] })
        dao.update(restored)
        if (notifications) schedule(context, restored)
        count++
    }
    imported.filter { it.parentId != null }.forEach { source ->
        val parent = oldToNew[source.parentId] ?: source.parentId ?: return@forEach
        val series = source.seriesId?.let { oldToNew[it] ?: it }
        val child = source.copy(id = 0L, parentId = parent, seriesId = series, recurrence = "NONE")
        val newId = dao.insert(child)
        if (notifications) schedule(context, child.copy(id = newId))
        count++
    }
    return count
}

private suspend fun restoreDeletedTasks(context: Context, dao: TaskDao, removed: List<TaskEntity>, notifications: Boolean) {
    val oldToNew = mutableMapOf<Long, Long>()
    val roots = removed.filter { it.parentId == null }.sortedWith(compareBy<TaskEntity> { if (it.recurrence != "NONE") 0 else 1 }.thenBy { it.date }.thenBy { it.id })
    roots.forEach { source ->
        val newId = dao.insert(source.copy(id = 0L, seriesId = null))
        oldToNew[source.id] = newId
        val restored = source.copy(id = newId, seriesId = if (source.recurrence != "NONE") newId else source.seriesId?.let { oldToNew[it] ?: it })
        dao.update(restored)
        if (notifications) schedule(context, restored)
    }
    removed.filter { it.parentId != null }.forEach { source ->
        val parent = oldToNew[source.parentId] ?: source.parentId ?: return@forEach
        val series = source.seriesId?.let { oldToNew[it] ?: it }
        val restored = source.copy(id = 0L, parentId = parent, seriesId = series, recurrence = "NONE")
        val newId = dao.insert(restored)
        if (notifications) schedule(context, restored.copy(id = newId))
    }
}

private suspend fun saveNewTask(context: Context, dao: TaskDao, task: TaskEntity, notifications: Boolean) {
    val baseId = dao.insert(task.copy(seriesId = null))
    val base = task.copy(id = baseId, seriesId = if (task.recurrence != "NONE") baseId else null)
    dao.update(base)
    if (notifications) schedule(context, base)
    if (task.parentId == null && task.recurrence != "NONE") {
        recurring(task).forEach { occurrence ->
            val occurrenceId = dao.insert(occurrence.copy(recurrence = "NONE", seriesId = baseId))
            if (notifications) schedule(context, occurrence.copy(id = occurrenceId, recurrence = "NONE", seriesId = baseId))
        }
    }
}
private suspend fun exportTasks(context: Context, uri: Uri, tasks: List<TaskEntity>) = withContext(Dispatchers.IO) { val root = JSONObject().put("version", 2).put("exportedAt", System.currentTimeMillis()); val array = JSONArray(); tasks.forEach { task -> array.put(JSONObject().apply { put("id", task.id); put("title", task.title); put("note", task.note); put("date", task.date); put("time", task.time ?: JSONObject.NULL); put("priority", task.priority); put("recurrence", task.recurrence); put("reminder", task.reminder); put("completed", task.completed); put("createdAt", task.createdAt); put("completedAt", task.completedAt ?: JSONObject.NULL); put("category", task.category); put("parentId", task.parentId ?: JSONObject.NULL); put("seriesId", task.seriesId ?: JSONObject.NULL) }) }; root.put("tasks", array); context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer -> requireNotNull(writer) { "Unable to open output" }; writer.write(root.toString(2)) } }
private suspend fun importTasks(context: Context, uri: Uri): List<TaskEntity> = withContext(Dispatchers.IO) { val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { it?.readText() } ?: error("Unable to open input"); val array = JSONObject(text).optJSONArray("tasks") ?: JSONArray(); buildList { for (i in 0 until array.length()) { val o = array.getJSONObject(i); val title = o.optString("title").trim(); val date = o.optString("date").trim(); if (title.isBlank() || runCatching { LocalDate.parse(date) }.isFailure) continue; add(TaskEntity(id = o.optLong("id", 0L), title = title, note = o.optString("note"), date = date, time = if (o.isNull("time")) null else o.optString("time").takeIf { it.isNotBlank() }, priority = o.optInt("priority", 1).coerceIn(1, 3), recurrence = o.optString("recurrence", "NONE").let { if (it in listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")) it else "NONE" }, reminder = o.optBoolean("reminder", false), completed = o.optBoolean("completed", false), createdAt = o.optLong("createdAt", System.currentTimeMillis()), completedAt = if (o.isNull("completedAt")) null else o.optLong("completedAt"), category = o.optString("category", "PERSONAL").let { if (it in listOf("WORK", "HOBBY", "PERSONAL", "SHOPPING")) it else "PERSONAL" }, parentId = if (o.isNull("parentId")) null else o.optLong("parentId"), seriesId = if (o.isNull("seriesId")) null else o.optLong("seriesId"))) } } }
private fun localizedContext(context: Context, english: Boolean): Context { val config = android.content.res.Configuration(context.resources.configuration); config.setLocale(if (english) en else ru); return context.createConfigurationContext(config) }
private fun showDatePicker(context: Context, current: String, english: Boolean, onDate: (String) -> Unit) { val d = runCatching { LocalDate.parse(current) }.getOrElse { LocalDate.now() }; val local = localizedContext(context, english); DatePickerDialog(local, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).show() }
private fun showTimePicker(context: Context, current: String, english: Boolean, onTime: (String) -> Unit) { val parts = current.split(":"); val h = parts.getOrNull(0)?.toIntOrNull() ?: 9; val m = parts.getOrNull(1)?.toIntOrNull() ?: 0; TimePickerDialog(localizedContext(context, english), { _, hour, minute -> onTime("%02d:%02d".format(hour, minute)) }, h, m, true).show() }
private fun prettyDate(value: String, ui: UiText): String = runCatching { val d = LocalDate.parse(value); "${d.dayOfMonth} ${ui.month(d)}" }.getOrElse { ui.v("Дата", "Date") }
private fun recurrenceLabel(value: String, ui: UiText) = when (value) { "DAILY" -> ui.v("ежедневно", "daily"); "WEEKLY" -> ui.v("еженедельно", "weekly"); "MONTHLY" -> ui.v("ежемесячно", "monthly"); else -> "" }
private fun recurrenceDescription(value: String, ui: UiText) = when (value) { "DAILY" -> ui.v("Создаётся на ближайшие 30 дней.", "Creates the next 30 days."); "WEEKLY" -> ui.v("Создаётся на ближайшие 12 недель.", "Creates the next 12 weeks."); "MONTHLY" -> ui.v("Создаётся на ближайшие 12 месяцев.", "Creates the next 12 months."); else -> "" }
private fun recurring(task: TaskEntity): List<TaskEntity> { val base = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return emptyList(); val count = when (task.recurrence) { "DAILY" -> 30; "WEEKLY" -> 12; "MONTHLY" -> 12; else -> 0 }; return (1..count).map { i -> val date = when (task.recurrence) { "DAILY" -> base.plusDays(i.toLong()); "WEEKLY" -> base.plusWeeks(i.toLong()); "MONTHLY" -> base.plusMonths(i.toLong()); else -> base }; task.copy(id = 0L, date = date.toString(), recurrence = "NONE", completed = false, completedAt = null, seriesId = null) } }
private fun weekRange(start: LocalDate, ui: UiText): String { val end = start.plusDays(6); return if (start.month == end.month) "${start.dayOfMonth}–${end.dayOfMonth} ${ui.month(start)}" else "${start.dayOfMonth} ${ui.month(start)} — ${end.dayOfMonth} ${ui.month(end)}" }
private fun schedule(context: Context, task: TaskEntity) { if (!task.reminder || task.time.isNullOrBlank() || task.date.isBlank()) return; val parts = task.time.split(":"); val date = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return; val millis = runCatching { LocalDateTime.of(date, LocalTime.of(parts[0].toInt(), parts[1].toInt())).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() ?: return; if (millis <= System.currentTimeMillis()) return; if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return; val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager; val intent = Intent(context, ReminderReceiver::class.java).putExtra("title", task.title).putExtra("task_id", task.id); val pending = PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending) } }
fun scheduleReminderFromSystem(context: Context, task: TaskEntity) = schedule(context, task)
private fun cancelReminder(context: Context, task: TaskEntity) { val intent = Intent(context, ReminderReceiver::class.java); val pending = PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE); pending?.cancel() }

private fun addToCalendar(context: Context, task: TaskEntity) {
    val date = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, task.title)
        putExtra(CalendarContract.Events.DESCRIPTION, task.note)
        if (task.time.isNullOrBlank()) {
            putExtra(CalendarContract.Events.ALL_DAY, true)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        } else {
            val parts = task.time.split(":"); val start = LocalDateTime.of(date, LocalTime.of(parts[0].toInt(), parts[1].toInt())).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start); putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 60 * 60 * 1000L)
        }
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
