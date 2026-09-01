package ru.sprint.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import ru.sprint.app.ReminderReceiver
import ru.sprint.app.data.TaskDao
import ru.sprint.app.data.TaskEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val Bg = Color(0xFF0A0B0A)
private val Surface = Color(0xFF121411)
private val Elevated = Color(0xFF181B17)
private val TextPrimary = Color(0xFFF4F4ED)
private val TextSecondary = Color(0xFF979C93)
private val Line = Color(0xFF252925)
private val Mint = Color(0xFFBFE8D0)
private val MintDeep = Color(0xFF244936)
private val Violet = Color(0xFFC8B8FF)
private val Pink = Color(0xFFF2A9D0)
private val Danger = Color(0xFFFFA9A9)
private val Gradient = Brush.linearGradient(listOf(Mint, Violet, Pink))
private val GradientDark = Brush.linearGradient(listOf(Color(0xFF244936), Color(0xFF302B4F), Color(0xFF4A2D42)))
private val ru = Locale("ru")
private val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun SprintApp(dao: TaskDao) {
    val context = LocalContext.current
    val root = LocalView.current
    val scope = rememberCoroutineScope()
    val tasks by dao.observeAll().collectAsState(emptyList())
    var screen by rememberSaveable { mutableStateOf("today") }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var onboarding by remember { mutableStateOf(!prefs(context).getBoolean("onboarding", false)) }
    var notifications by rememberSaveable { mutableStateOf(prefs(context).getBoolean("notifications", true)) }
    var haptics by rememberSaveable { mutableStateOf(prefs(context).getBoolean("haptics", true)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            notifications = false
            prefs(context).edit().putBoolean("notifications", false).apply()
        }
    }

    val colors = darkColorScheme(
        background = Bg,
        surface = Surface,
        surfaceContainer = Surface,
        surfaceContainerHigh = Elevated,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        outline = Line,
        primary = Mint,
        onPrimary = Color(0xFF122018),
        primaryContainer = MintDeep,
        onPrimaryContainer = Mint,
        secondary = Violet,
        tertiary = Pink,
        error = Danger
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography().copy(
            headlineLarge = Typography().headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.25).sp),
            headlineMedium = Typography().headlineMedium.copy(fontSize = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.6).sp),
            titleLarge = Typography().titleLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
            bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp)
        )
    ) {
        if (onboarding) {
            Onboarding {
                onboarding = false
                prefs(context).edit().putBoolean("onboarding", true).apply()
                if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return@MaterialTheme
        }

        Scaffold(
            containerColor = Bg,
            bottomBar = { FloatingNav(screen) { screen = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInHorizontally(tween(240)) { it / 16 }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { -it / 20 })
                    },
                    label = "screen-transition"
                ) { target ->
                    when (target) {
                        "today" -> TodayScreen(
                            tasks = tasks,
                            selected = selectedDate,
                            onDate = { selectedDate = it },
                            add = { editing = null; editorOpen = true },
                            search = { searchOpen = true },
                            edit = { editing = it; editorOpen = true },
                            toggle = { task ->
                                if (haptics) root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                scope.launch { dao.update(task.copy(completed = !task.completed, completedAt = if (!task.completed) System.currentTimeMillis() else null)) }
                            },
                            delete = { task -> scope.launch { dao.delete(task); cancelReminder(context, task) } }
                        )
                        "month" -> MonthScreen(tasks, selectedDate, onSelect = { selectedDate = it })
                        "overview" -> OverviewScreen(tasks)
                        else -> SettingsScreen(
                            notifications = notifications,
                            haptics = haptics,
                            setNotifications = { enabled ->
                                notifications = enabled
                                prefs(context).edit().putBoolean("notifications", enabled).apply()
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else tasks.filter { it.reminder && !it.completed }.forEach { schedule(context, it) }
                                } else tasks.forEach { cancelReminder(context, it) }
                            },
                            setHaptics = { enabled ->
                                haptics = enabled
                                prefs(context).edit().putBoolean("haptics", enabled).apply()
                            },
                            clear = { scope.launch { dao.deleteAll() } }
                        )
                    }
                }
            }
        }

        if (editorOpen) {
            TaskEditor(
                existing = editing,
                defaultDate = selectedDate,
                notificationsEnabled = notifications,
                hapticsEnabled = haptics,
                onDismiss = { editorOpen = false },
                onSave = { task ->
                    scope.launch {
                        if (task.id == 0L) {
                            val id = dao.insert(task)
                            val saved = task.copy(id = id)
                            if (notifications) schedule(context, saved)
                            recurring(task).forEach { occurrence ->
                                val occurrenceId = dao.insert(occurrence)
                                if (notifications) schedule(context, occurrence.copy(id = occurrenceId))
                            }
                        } else {
                            cancelReminder(context, task)
                            dao.update(task)
                            if (notifications) schedule(context, task)
                        }
                    }
                    selectedDate = task.date
                    editorOpen = false
                }
            )
        }

        if (searchOpen) {
            SearchSheet(
                tasks = tasks,
                onDismiss = { searchOpen = false },
                edit = { editing = it; editorOpen = true; searchOpen = false }
            )
        }
    }
}

private fun prefs(context: Context) = context.getSharedPreferences("sprint", Context.MODE_PRIVATE)

@Composable
private fun Onboarding(done: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Месяц. День. Действие.", "Планируй спокойно.", "Sprint остаётся лёгким.")
    val bodies = listOf(
        "Смотри на месяц целиком, выбирай день и сразу переходи к делам.",
        "Приоритеты, повторения, напоминания и заметки работают вместе — без скрытых уровней.",
        "Красивый интерфейс не должен мешать планировать. Всё важное рядом и ничего лишнего."
    )
    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(Mint.copy(alpha = .09f), radius = 230.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .9f, size.height * .12f))
            drawCircle(Violet.copy(alpha = .07f), radius = 260.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .05f, size.height * .82f))
        })
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = done, modifier = Modifier.align(Alignment.End)) { Text("Пропустить", color = TextSecondary) }
            AnimatedContent(targetState = page, label = "onboarding") { p ->
                Column {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(21.dp)).background(Gradient), contentAlignment = Alignment.Center) {
                        Text("S", color = Color(0xFF111412), fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(26.dp))
                    Text(titles[p], style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(bodies[p], color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
                    Spacer(Modifier.height(26.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { i -> Box(Modifier.width(if (i == p) 24.dp else 6.dp).height(6.dp).clip(CircleShape).background(if (i == p) Mint else Line)) }
                    }
                }
            }
            GradientButton(if (page < 2) "Дальше" else "Начать") { if (page < 2) page++ else done() }
        }
    }
}

@Composable
private fun FloatingNav(screen: String, select: (String) -> Unit) {
    val items = listOf("today" to "Сегодня", "month" to "Месяц", "overview" to "Обзор", "settings" to "Настройки")
    Box(Modifier.fillMaxWidth().background(Bg.copy(alpha = .98f)).padding(horizontal = 14.dp, vertical = 7.dp)) {
        Row(
            Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(26.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(26.dp)).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { (id, label) ->
                val selected = screen == id
                val bg = if (selected) Gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                val width by animateDpAsState(if (selected) 92.dp else 60.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "nav-width")
                val scale by animateFloatAsState(if (selected) 1f else .96f, spring(stiffness = Spring.StiffnessMedium), label = "nav-scale")
                Box(
                    Modifier.width(width).fillMaxHeight().scale(scale).offset(y = if (selected) (-3).dp else 0.dp).clip(RoundedCornerShape(21.dp)).background(bg)
                        .clickable { select(id) }.semantics { contentDescription = label; role = Role.Tab },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        NavGlyph(id, if (selected) Color(0xFF111412) else TextSecondary)
                        AnimatedVisibility(selected, enter = fadeIn(tween(160)) + scaleIn(), exit = fadeOut(tween(100)) + scaleOut()) {
                            Text(label, color = Color(0xFF111412), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavGlyph(id: String, color: Color) {
    Canvas(Modifier.size(21.dp)) {
        val s = 1.8.dp.toPx()
        when (id) {
            "today" -> { drawCircle(color, 6.dp.toPx(), style = Stroke(s)); drawCircle(color, 2.dp.toPx()) }
            "month" -> { drawRoundRect(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()), androidx.compose.ui.geometry.Size(16.dp.toPx(), 16.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(s)); drawLine(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 7.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 7.dp.toPx()), s) }
            "overview" -> { drawLine(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 15.dp.toPx()), androidx.compose.ui.geometry.Offset(7.dp.toPx(), 10.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(7.dp.toPx(), 10.dp.toPx()), androidx.compose.ui.geometry.Offset(11.dp.toPx(), 12.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(11.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 4.dp.toPx()), s) }
            else -> { drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 6.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 6.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 11.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 11.dp.toPx()), s); drawLine(color, androidx.compose.ui.geometry.Offset(3.dp.toPx(), 16.dp.toPx()), androidx.compose.ui.geometry.Offset(14.dp.toPx(), 16.dp.toPx()), s) }
        }
    }
}

@Composable
private fun TodayScreen(
    tasks: List<TaskEntity>, selected: String, onDate: (String) -> Unit, add: () -> Unit, search: () -> Unit,
    edit: (TaskEntity) -> Unit, toggle: (TaskEntity) -> Unit, delete: (TaskEntity) -> Unit
) {
    val date = LocalDate.parse(selected)
    val dayTasks = tasks.filter { it.date == selected }.sortedWith(compareBy<TaskEntity> { it.completed }.thenByDescending { it.priority }.thenBy { it.time ?: "99:99" }.thenBy { it.id })
    val completed = dayTasks.count { it.completed }
    val progress = if (dayTasks.isEmpty()) 0f else completed.toFloat() / dayTasks.size
    var filter by rememberSaveable(selected) { mutableStateOf(0) }
    val visible = when (filter) {
        1 -> dayTasks.filter { it.priority >= 2 }
        2 -> dayTasks.filter { it.reminder }
        else -> dayTasks
    }
    LazyColumn(Modifier.drawBehind {
        drawCircle(Mint.copy(alpha = .035f), radius = 180.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .95f, 80.dp.toPx()))
        drawCircle(Violet.copy(alpha = .025f), radius = 220.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .05f, size.height * .55f))
    }, contentPadding = PaddingValues(bottom = 26.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(date.month.getDisplayName(TextStyle.FULL, ru).uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                    Text("${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, ru)}", style = MaterialTheme.typography.headlineLarge)
                }
                IconButton(onClick = search, modifier = Modifier.size(44.dp)) { SearchGlyph(TextSecondary) }
                Spacer(Modifier.width(4.dp))
                GradientIconButton(onClick = add)
            }
        }
        item { WeekStrip(date, tasks, onDate) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (dayTasks.isEmpty()) "Свободный день" else "$completed из ${dayTasks.size}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(if (dayTasks.isEmpty()) "Оставь место для жизни или добавь одно важное дело." else if (completed == dayTasks.size) "День закрыт" else "дел выполнено", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                if (dayTasks.isNotEmpty()) {
                    Box(Modifier.size(50.dp).drawBehind { drawCircle(Mint.copy(alpha = .06f)) }, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 4.dp, color = Mint, trackColor = Line)
                        Text("${(progress * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (dayTasks.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = filter == 0, onClick = { filter = 0 }, label = { Text("Все") })
                FilterChip(selected = filter == 1, onClick = { filter = 1 }, label = { Text("Важные") })
                FilterChip(selected = filter == 2, onClick = { filter = 2 }, label = { Text("С напоминанием") })
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        if (visible.isEmpty()) item {
            EmptyState(if (dayTasks.isEmpty()) "Сегодня пусто" else "Нет задач в этом фильтре", if (dayTasks.isEmpty()) "Добавь одно дело — остальные не обязательны." else "Сбрось фильтр, чтобы увидеть все дела.")
        } else items(visible, key = { it.id }) { TaskRow(it, edit, toggle, delete) }
    }
}

@Composable
private fun WeekStrip(center: LocalDate, tasks: List<TaskEntity>, onDate: (String) -> Unit) {
    val monday = center.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(7) { index ->
            val date = monday.plusDays(index.toLong())
            val selected = date == center
            val count = tasks.count { it.date == date.toString() && !it.completed }
            val brush = if (selected) Gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            Box(Modifier.weight(1f).height(74.dp).clip(RoundedCornerShape(19.dp)).background(brush).clickable { onDate(date.toString()) }.semantics { contentDescription = "${weekdays[index]} ${date.dayOfMonth}"; role = Role.Button }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(weekdays[index], fontSize = 9.sp, color = if (selected) Color(0xFF37413A) else TextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text(date.dayOfMonth.toString(), color = if (selected) Color(0xFF111412) else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.size(if (count > 0) 4.dp else 2.dp).clip(CircleShape).background(if (selected) Color(0xFF37413A) else Mint))
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, edit: (TaskEntity) -> Unit, toggle: (TaskEntity) -> Unit, delete: (TaskEntity) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (task.completed) .54f else 1f, tween(220), label = "task-alpha")
    val priorityColor = when (task.priority) { 3 -> Danger; 2 -> Violet; else -> Line }
    Row(Modifier.fillMaxWidth().animateContentSize().clickable { edit(task) }.padding(horizontal = 22.dp, vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).border(1.6.dp, if (task.completed) Mint else priorityColor, CircleShape).clip(CircleShape).background(if (task.completed) Mint else Color.Transparent).clickable { toggle(task) }, contentAlignment = Alignment.Center) {
                AnimatedVisibility(task.completed, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) { Text("✓", color = Color(0xFF111412), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Column(Modifier.weight(1f).graphicsLayer { this.alpha = alpha }.padding(top = 3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.priority >= 2 && !task.completed) {
                    Text(if (task.priority == 3) "!!" else "!", color = priorityColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                }
                Text(task.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val meta = buildList {
                task.time?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (task.reminder) add("напоминание")
                if (task.recurrence != "NONE") add(recurrenceLabel(task.recurrence))
            }.joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            if (task.note.isNotBlank()) Text(task.note, color = TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
        }
        TextButton(onClick = { confirm = true }, contentPadding = PaddingValues(horizontal = 3.dp)) { Text("•••", color = TextSecondary, fontSize = 14.sp) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Удалить дело?") }, text = { Text("Задача и её напоминание будут удалены.", color = TextSecondary) }, confirmButton = { TextButton({ confirm = false; delete(task) }) { Text("Удалить", color = Danger) } }, dismissButton = { TextButton({ confirm = false }) { Text("Отмена") } })
}

@Composable
private fun MonthScreen(tasks: List<TaskEntity>, selected: String, onSelect: (String) -> Unit) {
    var month by rememberSaveable { mutableStateOf(LocalDate.parse(selected).withDayOfMonth(1).toString()) }
    val first = LocalDate.parse(month)
    val start = first.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val days = (0 until 42).map { start.plusDays(it.toLong()) }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp).drawBehind {
        drawCircle(Violet.copy(alpha = .035f), radius = 220.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .92f, 90.dp.toPx()))
        drawCircle(Mint.copy(alpha = .02f), radius = 190.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .45f))
    }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("КАЛЕНДАРЬ", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                AnimatedContent(targetState = first, label = "month-title") { d -> Text("${d.month.getDisplayName(TextStyle.FULL, ru).replaceFirstChar { it.uppercase() }} ${d.year}", style = MaterialTheme.typography.headlineLarge) }
            }
            if (first.year != today.year || first.month != today.month) TextButton(onClick = { month = today.withDayOfMonth(1).toString(); onSelect(today.toString()) }) { Text("Сегодня", color = Mint) }
            IconButton(onClick = { month = first.minusMonths(1).toString() }) { Text("‹", fontSize = 31.sp, color = TextPrimary) }
            IconButton(onClick = { month = first.plusMonths(1).toString() }) { Text("›", fontSize = 31.sp, color = TextPrimary) }
        }
        Row(Modifier.padding(horizontal = 22.dp)) { weekdays.forEach { Text(it, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 18.dp)) {
            days.chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { date ->
                        val inMonth = date.month == first.month && date.year == first.year
                        val isToday = date == today
                        val selectedDay = date.toString() == selected
                        val active = tasks.count { it.date == date.toString() && !it.completed }
                        val done = tasks.count { it.date == date.toString() && it.completed }
                        val brush = if (selectedDay) GradientDark else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        Column(Modifier.weight(1f).height(72.dp).padding(3.dp).clip(RoundedCornerShape(17.dp)).background(brush).clickable { onSelect(date.toString()) }.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(date.dayOfMonth.toString(), color = if (!inMonth) TextSecondary.copy(alpha = .3f) else if (isToday) Mint else TextPrimary, fontWeight = if (isToday || selectedDay) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                if (isToday) { Spacer(Modifier.width(5.dp)); Box(Modifier.size(5.dp).clip(CircleShape).background(Mint)) }
                            }
                            Spacer(Modifier.weight(1f))
                            if (active + done > 0) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(minOf(active, 3)) { Box(Modifier.size(4.dp).clip(CircleShape).background(if (selectedDay) Mint else Mint.copy(alpha = .75f))) }
                                repeat(minOf(done, 2)) { Box(Modifier.size(4.dp).clip(CircleShape).background(TextSecondary.copy(alpha = .45f))) }
                            }
                        }
                    }
                }
            }
        }
        val monthTasks = tasks.filter { runCatching { val d = LocalDate.parse(it.date); d.month == first.month && d.year == first.year }.getOrDefault(false) }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${monthTasks.count { !it.completed }} активных", fontWeight = FontWeight.SemiBold)
                Text("${monthTasks.count { it.completed }} выполнено", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (monthTasks.isNotEmpty()) {
                val p = monthTasks.count { it.completed }.toFloat() / monthTasks.size
                Box(Modifier.width(120.dp).height(8.dp).clip(RoundedCornerShape(8.dp)).background(Line)) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(8.dp)).background(Gradient)) }
            }
        }
    }
}

@Composable
private fun OverviewScreen(tasks: List<TaskEntity>) {
    var weekStart by rememberSaveable { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()) }
    val start = LocalDate.parse(weekStart)
    val current = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val values = (0..6).map { d -> tasks.count { it.date == start.plusDays(d.toLong()).toString() && it.completed } }
    val planned = (0..6).sumOf { d -> tasks.count { it.date == start.plusDays(d.toLong()).toString() } }
    val done = values.sum()
    val percent = if (planned == 0) 0 else done * 100 / planned
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp).drawBehind {
        drawCircle(Pink.copy(alpha = .025f), radius = 210.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .92f, 80.dp.toPx()))
    }) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text("АНАЛИТИКА", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Ритм", style = MaterialTheme.typography.headlineLarge)
        }
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { weekStart = start.minusWeeks(1).toString() }) { Text("‹", fontSize = 30.sp) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(weekRange(start), fontWeight = FontWeight.SemiBold)
                if (start == current) Text("текущая неделя", color = Mint, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            }
            IconButton(onClick = { weekStart = start.plusWeeks(1).toString() }) { Text("›", fontSize = 30.sp) }
        }
        if (start != current) TextButton(onClick = { weekStart = current.toString() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Вернуться к сегодня", color = Mint) }
        Row(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.Bottom) {
            Text("${percent}%", fontSize = 58.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2.5).sp)
            Spacer(Modifier.width(10.dp)); Text("выполнено", color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        }
        Box(Modifier.fillMaxWidth().height(230.dp).padding(horizontal = 28.dp)) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
                values.forEachIndexed { i, value ->
                    val height = (145f * value / max).coerceAtLeast(if (value == 0) 5f else 18f)
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(12.dp)).background(if (value > 0) Gradient else Line))
                        Spacer(Modifier.height(9.dp)); Text(weekdays[i], color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Line)
        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("$planned", "запланировано")
                StatPill("$done", "готово")
            }
            Spacer(Modifier.height(22.dp))
            Text("Что видно", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(overviewText(planned, done, values), color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SettingsScreen(notifications: Boolean, haptics: Boolean, setNotifications: (Boolean) -> Unit, setHaptics: (Boolean) -> Unit, clear: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text("ПРИЛОЖЕНИЕ", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Настройки", style = MaterialTheme.typography.headlineLarge)
        }
        SettingBlock {
            SettingRow("Тёмная тема", "Основное оформление Sprint", Switch(checked = true, onCheckedChange = null, enabled = false))
        }
        SettingBlock(title = "Напоминания") {
            SettingRow("Уведомления", if (notifications) "Напоминания о задачах со временем" else "Напоминания выключены", Switch(checked = notifications, onCheckedChange = setNotifications))
        }
        SettingBlock(title = "Отклик") {
            SettingRow("Микровибрация", "Лёгкий отклик при выборе и завершении", Switch(checked = haptics, onCheckedChange = setHaptics))
        }
        SettingBlock(title = "Данные") {
            Text("Удалить все задачи", color = Danger, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().clickable { confirm = true }.padding(horizontal = 22.dp, vertical = 18.dp))
        }
        SettingBlock(title = "Sprint") {
            Text("Минималистичный планировщик месяца", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
            Text("Все задачи и напоминания хранятся на устройстве. Интерфейс остаётся тёмным по умолчанию.", color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
            Spacer(Modifier.height(14.dp))
        }
    }
    if (confirm) Dialog(onDismissRequest = { confirm = false }) {
        Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Elevated)) {
            Column(Modifier.padding(22.dp)) {
                Text("Удалить все задачи?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp)); Text("Все записи будут удалены без возможности восстановления.", color = TextSecondary)
                Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ confirm = false }) { Text("Отмена") }
                    TextButton({ clear(); confirm = false }) { Text("Удалить", color = Danger) }
                }
            }
        }
    }
}

@Composable
private fun SettingBlock(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = if (title == null) 0.dp else 8.dp, bottom = 8.dp)) {
        if (title != null) Text(title.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp))
        content()
        HorizontalDivider(color = Line)
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp)) }
        control()
    }
}

@Composable
private fun TaskEditor(existing: TaskEntity?, defaultDate: String, notificationsEnabled: Boolean, hapticsEnabled: Boolean, onDismiss: () -> Unit, onSave: (TaskEntity) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.date?.ifBlank { defaultDate } ?: defaultDate) }
    var time by remember { mutableStateOf(existing?.time ?: "") }
    var priority by remember { mutableIntStateOf(existing?.priority ?: 1) }
    var repeat by remember { mutableStateOf(existing?.recurrence ?: "NONE") }
    var reminder by remember { mutableStateOf(existing?.reminder ?: false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface, tonalElevation = 0.dp, dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 26.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (existing == null) "Новое дело" else "Изменить дело", style = MaterialTheme.typography.headlineMedium)
                    Text("Все параметры видны сразу", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                }
                TextButton(onClick = onDismiss) { Text("Закрыть", color = TextSecondary) }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Что нужно сделать?") }, shape = RoundedCornerShape(17.dp), colors = fieldColors())
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ showDatePicker(context, date) { date = it } }, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp)) { Text(prettyDate(date)) }
                OutlinedButton({ showTimePicker(context, time) { time = it } }, Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp)) { Text(time.ifBlank { "Без времени" }) }
            }
            Spacer(Modifier.height(15.dp))
            Text("Приоритет", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Chips(listOf("Обычный", "Важный", "Срочный"), priority - 1) { priority = it + 1 }
            Text("Влияет на порядок дел: срочные выше важных, важные выше обычных.", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))
            Text("Повторение", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Chips(listOf("Нет", "Каждый день", "Каждую неделю", "Каждый месяц"), listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").indexOf(repeat)) { repeat = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")[it] }
            if (repeat != "NONE") Text(recurrenceDescription(repeat), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            SettingRow("Напоминание", if (notificationsEnabled && time.isNotBlank()) "В ${time}" else "Нужны дата и время") { Switch(checked = reminder && notificationsEnabled && time.isNotBlank(), onCheckedChange = { if (notificationsEnabled && time.isNotBlank()) reminder = it }) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().heightIn(min = 92.dp), label = { Text("Заметка") }, placeholder = { Text("Детали, ссылка или короткая мысль") }, minLines = 3, maxLines = 5, shape = RoundedCornerShape(17.dp), colors = fieldColors())
            Spacer(Modifier.height(18.dp))
            GradientButton(if (existing == null) "Добавить дело" else "Сохранить") {
                if (title.isNotBlank()) {
                    if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSave(TaskEntity(existing?.id ?: 0L, title.trim(), note.trim(), date, time.takeIf { it.isNotBlank() }, priority, repeat, reminder && notificationsEnabled && time.isNotBlank(), existing?.completed ?: false, existing?.createdAt ?: System.currentTimeMillis(), existing?.completedAt))
                }
            }
        }
    }
}

@Composable
private fun SearchSheet(tasks: List<TaskEntity>, onDismiss: () -> Unit, edit: (TaskEntity) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, tasks) { if (query.isBlank()) tasks.take(12) else tasks.filter { it.title.contains(query, true) || it.note.contains(query, true) }.take(30) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface, dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp)) {
            Text("Поиск", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Найти дело или заметку") }, shape = RoundedCornerShape(17.dp), colors = fieldColors())
            Spacer(Modifier.height(10.dp))
            if (results.isEmpty()) EmptyState("Ничего не найдено", "Попробуй другое слово или фразу.")
            else LazyColumn(Modifier.heightIn(max = 430.dp)) { items(results, key = { it.id }) { task -> SearchResult(task) { edit(task) } } }
        }
    }
}

@Composable
private fun SearchResult(task: TaskEntity, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 11.dp)) {
        Text(task.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${prettyDate(task.date)}${task.time?.let { " · $it" } ?: ""}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
        if (task.note.isNotBlank()) Text(task.note, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(17.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth().height(54.dp).drawBehind { drawRoundRect(Gradient, cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx())) }) {
        Box(contentAlignment = Alignment.Center) { Text(text, color = Color(0xFF111412), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun GradientIconButton(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(52.dp).drawBehind { drawCircle(Gradient) }) {
        Box(contentAlignment = Alignment.Center) { Text("+", color = Color(0xFF111412), fontSize = 28.sp, fontWeight = FontWeight.Light) }
    }
}

@Composable
private fun SearchGlyph(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        drawCircle(color, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()), style = Stroke(1.8.dp.toPx()))
        drawLine(color, androidx.compose.ui.geometry.Offset(12.5.dp.toPx(), 12.5.dp.toPx()), androidx.compose.ui.geometry.Offset(18.dp.toPx(), 18.dp.toPx()), 1.8.dp.toPx())
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Gradient))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Elevated).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Chips(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label -> FilterChip(selected = selected == index, onClick = { onSelected(index) }, label = { Text(label) }) }
    }
}


@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Mint, unfocusedBorderColor = Line, cursorColor = Mint, focusedLabelColor = Mint)

private fun showDatePicker(context: Context, current: String, onDate: (String) -> Unit) {
    val d = runCatching { LocalDate.parse(current) }.getOrElse { LocalDate.now() }
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

private fun showTimePicker(context: Context, current: String, onTime: (String) -> Unit) {
    val parts = current.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(context, { _, hour, minute -> onTime("%02d:%02d".format(hour, minute)) }, h, m, true).show()
}

private fun prettyDate(value: String): String = runCatching { val d = LocalDate.parse(value); "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, ru).replaceFirstChar { it.uppercase() }}" }.getOrElse { "Дата" }
private fun recurrenceLabel(value: String) = when (value) { "DAILY" -> "ежедневно"; "WEEKLY" -> "еженедельно"; "MONTHLY" -> "ежемесячно"; else -> "" }
private fun recurrenceDescription(value: String) = when (value) { "DAILY" -> "Создаётся на ближайшие 30 дней."; "WEEKLY" -> "Создаётся на ближайшие 12 недель."; "MONTHLY" -> "Создаётся на ближайшие 12 месяцев."; else -> "" }

private fun recurring(task: TaskEntity): List<TaskEntity> {
    val base = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return emptyList()
    val count = when (task.recurrence) { "DAILY" -> 30; "WEEKLY" -> 12; "MONTHLY" -> 12; else -> 0 }
    return (1..count).map { i ->
        val date = when (task.recurrence) { "DAILY" -> base.plusDays(i.toLong()); "WEEKLY" -> base.plusWeeks(i.toLong()); "MONTHLY" -> base.plusMonths(i.toLong()); else -> base }
        task.copy(id = 0L, date = date.toString(), completed = false, completedAt = null)
    }
}

private fun schedule(context: Context, task: TaskEntity) {
    if (!task.reminder || task.time.isNullOrBlank() || task.date.isBlank()) return
    val parts = task.time.split(":")
    val date = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return
    val millis = runCatching { LocalDateTime.of(date, java.time.LocalTime.of(parts[0].toInt(), parts[1].toInt())).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() ?: return
    if (millis <= System.currentTimeMillis()) return
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("title", task.title)
    val pending = PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending) }
}

private fun cancelReminder(context: Context, task: TaskEntity) {
    val intent = Intent(context, ReminderReceiver::class.java)
    val pending = PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
    pending?.cancel()
}

private fun weekRange(start: LocalDate): String {
    val end = start.plusDays(6)
    return if (start.month == end.month) "${start.dayOfMonth}–${end.dayOfMonth} ${start.month.getDisplayName(TextStyle.FULL, ru)}" else "${start.dayOfMonth} ${start.month.getDisplayName(TextStyle.FULL, ru)} — ${end.dayOfMonth} ${end.month.getDisplayName(TextStyle.FULL, ru)}"
}

private fun overviewText(planned: Int, done: Int, values: List<Int>): String = when {
    planned == 0 -> "За эту неделю нет задач. Здесь появится картина выполнения, когда появятся реальные дела."
    done == planned -> "Все запланированные дела выполнены. Ритм ровный — не обязательно добавлять больше."
    done == 0 -> "Пока ничего не выполнено. Начни с одного важного дела и не перегружай день."
    values.count { it > 0 } >= 5 -> "Выполнение распределено по неделе. Нагрузка выглядит равномерно."
    else -> "Часть задач выполнена. Видно, что работа сосредоточена в отдельных днях — свободные дни можно оставить свободными."
}
