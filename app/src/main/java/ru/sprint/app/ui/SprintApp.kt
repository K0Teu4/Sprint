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
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import ru.sprint.app.ReminderReceiver
import ru.sprint.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.ZoneId
import java.util.Locale

private val Canvas = Color(0xFFF8F8F5)
private val Ink = Color(0xFF151613)
private val Secondary = Color(0xFF777971)
private val Hairline = Color(0xFFE4E4DE)
private val Green = Color(0xFF31533D)
private val GreenSoft = Color(0xFFE6EEE8)
private val Red = Color(0xFFB34D4D)
private val DarkCanvas = Color(0xFF10110F)
private val DarkSurface = Color(0xFF171814)
private val DarkHairline = Color(0xFF2B2C28)
private val DarkSecondary = Color(0xFF9A9B94)
private val ru = Locale("ru")
private val week = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun SprintApp(dao: TaskDao, goalDao: GoalDao, templateDao: TemplateDao) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val tasks by dao.observeAll().collectAsState(emptyList())
    var screen by rememberSaveable { mutableStateOf("today") }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var dark by rememberSaveable { mutableStateOf(false) }
    var onboarding by remember { mutableStateOf(!context.getSharedPreferences("sprint", 0).getBoolean("onboarding", false)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val scheme = if (dark) darkColorScheme(
        background = DarkCanvas, surface = DarkSurface, surfaceContainer = Color(0xFF1A1B17),
        onBackground = Color(0xFFF3F3EC), onSurface = Color(0xFFF3F3EC), onSurfaceVariant = Color(0xFFA5A69E),
        outline = Color(0xFF30312D), primary = Color(0xFFBFD3C3), primaryContainer = Color(0xFF26372B),
        onPrimaryContainer = Color(0xFFDCEBDF), secondary = Color(0xFFBFD3C3)
    ) else lightColorScheme(
        background = Canvas, surface = Color(0xFFFCFCF9), surfaceContainer = Color(0xFFF2F2ED),
        onBackground = Ink, onSurface = Ink, onSurfaceVariant = Secondary, outline = Hairline,
        primary = Green, primaryContainer = GreenSoft, onPrimaryContainer = Green, secondary = Green
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography().copy(
            headlineLarge = Typography().headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.05).sp),
            headlineMedium = Typography().headlineMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp),
            titleLarge = Typography().titleLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15).sp),
            bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp, letterSpacing = (-0.05).sp),
            bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp)
        )
    ) {
        if (onboarding) {
            Onboarding {
                onboarding = false
                context.getSharedPreferences("sprint", 0).edit().putBoolean("onboarding", true).apply()
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return@MaterialTheme
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { BottomNav(screen) { screen = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) + androidx.compose.animation.scaleIn(initialScale = 0.985f))
                            .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(120)) + androidx.compose.animation.scaleOut(targetScale = 0.985f))
                    },
                    label = "screen-transition"
                ) { target ->
                    when (target) {
                    "today" -> TodayScreen(
                        tasks, selectedDate,
                        onDate = { selectedDate = it },
                        add = { editing = null; editorOpen = true },
                        edit = { editing = it; editorOpen = true },
                        toggle = { task -> scope.launch { dao.update(task.copy(completed = !task.completed, completedAt = if (!task.completed) System.currentTimeMillis() else null)) } },
                        delete = { task -> scope.launch { dao.delete(task) } }
                    )
                    "month" -> MonthScreen(tasks, selectedDate) { selectedDate = it; screen = "today" }
                    "overview" -> OverviewScreen(tasks)
                    else -> MoreScreen(dark, { dark = it }) { scope.launch { dao.deleteAll(); goalDao.deleteAll(); templateDao.deleteAll() } }
                    }
                }
            }
        }

        if (editorOpen) {
            TaskEditor(
                existing = editing,
                defaultDate = selectedDate,
                onDismiss = { editorOpen = false },
                onSave = { task ->
                    scope.launch {
                        val saved = if (task.id == 0L) task.copy(id = dao.insert(task)) else { dao.update(task); task }
                        schedule(context, saved)
                        if (task.id == 0L && task.recurrence != "NONE" && task.date.isNotBlank()) recurring(task).forEach { dao.insert(it) }
                    }
                    selectedDate = task.date.ifBlank { selectedDate }
                    editorOpen = false
                }
            )
        }
    }
}

@Composable
private fun Onboarding(done: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Месяц без шума", "Один день за раз", "Планируй меньше")
    val bodies = listOf(
        "Sprint показывает месяц целиком, но не заставляет управлять им как таблицей.",
        "Выбираешь день — видишь только его задачи. Ничего лишнего не конкурирует за внимание.",
        "Хороший план не заполнен до краёв. Он оставляет место для реальной жизни."
    )
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = done, modifier = Modifier.align(Alignment.End)) { Text("Пропустить", color = Secondary) }
        Column {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Green), contentAlignment = Alignment.Center) {
                Text("S", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(26.dp))
            Text(titles[page], style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text(bodies[page], color = Secondary, fontSize = 16.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { Box(Modifier.width(if (it == page) 22.dp else 6.dp).height(6.dp).clip(CircleShape).background(if (it == page) Green else Hairline)) }
            }
        }
        Button(
            onClick = { if (page < 2) page++ else done() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp)
        ) { Text(if (page < 2) "Дальше" else "Начать") }
    }
}

@Composable
private fun BottomNav(screen: String, select: (String) -> Unit) {
    val items = listOf("today" to "Сегодня", "month" to "Месяц", "overview" to "Обзор", "more" to "Ещё")
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { (id, label) ->
                val selected = screen == id
                val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, androidx.compose.animation.core.tween(240), label = "nav-bg")
                val content by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, androidx.compose.animation.core.tween(180), label = "nav-content")
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                        .semantics { contentDescription = label; role = Role.Tab }
                        .clickable { select(id) }.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NavGlyph(id, content)
                        Spacer(Modifier.height(2.dp))
                        Text(label, color = content, fontSize = 9.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavGlyph(id: String, color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        when (id) {
            "today" -> {
                drawCircle(color = color, radius = 5.5.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawCircle(color = color, radius = 1.7.dp.toPx())
            }
            "month" -> {
                drawRoundRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 14.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawLine(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 6.dp.toPx()), androidx.compose.ui.geometry.Offset(16.dp.toPx(), 6.dp.toPx()), strokeWidth = stroke)
            }
            "overview" -> {
                drawLine(color, androidx.compose.ui.geometry.Offset(2.dp.toPx(), 13.dp.toPx()), androidx.compose.ui.geometry.Offset(6.dp.toPx(), 9.dp.toPx()), strokeWidth = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(6.dp.toPx(), 9.dp.toPx()), androidx.compose.ui.geometry.Offset(10.dp.toPx(), 11.dp.toPx()), strokeWidth = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(10.dp.toPx(), 11.dp.toPx()), androidx.compose.ui.geometry.Offset(16.dp.toPx(), 4.dp.toPx()), strokeWidth = stroke)
            }
            else -> {
                drawCircle(color = color, radius = 1.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 9.dp.toPx()))
                drawCircle(color = color, radius = 1.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()))
                drawCircle(color = color, radius = 1.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(14.dp.toPx(), 9.dp.toPx()))
            }
        }
    }
}

@Composable
private fun PageHeader(eyebrow: String, title: String, action: String = "+", onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), color = Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.35.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (action.isNotBlank()) {
            val pressScale = remember { androidx.compose.animation.core.Animatable(1f) }
            Surface(
                onClick = onAction,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp).scale(pressScale.value)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 25.sp, fontWeight = FontWeight.Light, modifier = Modifier.semantics { contentDescription = "Добавить задачу" })
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    tasks: List<TaskEntity>, selected: String, onDate: (String) -> Unit,
    add: () -> Unit, edit: (TaskEntity) -> Unit, toggle: (TaskEntity) -> Unit, delete: (TaskEntity) -> Unit
) {
    val date = LocalDate.parse(selected)
    val day = tasks.filter { it.date == selected }.sortedWith(compareBy<TaskEntity> { it.completed }.thenBy { it.time ?: "99:99" }.thenByDescending { it.priority })
    val completed = day.count { it.completed }
    val minutes = day.filterNot { it.completed }.sumOf { it.duration.coerceAtLeast(0) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            PageHeader(
                if (date == LocalDate.now()) "Сегодня" else date.format(DateTimeFormatter.ofPattern("d MMMM", ru)),
                if (date == LocalDate.now()) "Сегодня" else date.dayOfMonth.toString() + " " + date.month.getDisplayName(TextStyle.FULL, ru),
                onAction = add
            )
        }
        item { WeekStrip(date, tasks, onDate) }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                if (day.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$completed из ${day.size}", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp)); Text("выполнено", color = Secondary, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f)); Text(if (minutes == 0) "Готово" else "~${formatMinutes(minutes)}", color = Secondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / day.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)),
                        color = Green, trackColor = Hairline
                    )
                } else {
                    Text("Свободный день", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp)); Text("Оставь его таким или добавь одно важное дело.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
        if (day.isEmpty()) item { TextButton(onClick = add, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Добавить действие", color = Green) } }
        else items(day, key = { it.id }) { TaskRow(it, edit, toggle, delete) }
    }
}

@Composable
private fun WeekStrip(center: LocalDate, tasks: List<TaskEntity>, onDate: (String) -> Unit) {
    val monday = center.minusDays((center.dayOfWeek.value - 1).toLong())
    val view = LocalView.current
    Row(
        Modifier.padding(horizontal = 22.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (0..6).forEach { index ->
            val date = monday.plusDays(index.toLong())
            val selected = date == center
            val count = tasks.count { it.date == date.toString() && !it.completed }
            val bg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                androidx.compose.animation.core.tween(220), label = "week-bg"
            )
            val fg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                androidx.compose.animation.core.tween(180), label = "week-fg"
            )
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(bg)
                    .semantics { contentDescription = "${week[index]} ${date.dayOfMonth}"; role = Role.Button }
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDate(date.toString())
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(week[index], fontSize = 9.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(date.dayOfMonth.toString(), fontWeight = FontWeight.SemiBold, color = fg)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(if (count > 0) 4.dp else 2.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.onPrimary else if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, edit: (TaskEntity) -> Unit, toggle: (TaskEntity) -> Unit, delete: (TaskEntity) -> Unit) {
    val view = LocalView.current
    val checkColor by animateColorAsState(if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, androidx.compose.animation.core.tween(180), label = "check-color")
    val titleColor by animateColorAsState(if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, androidx.compose.animation.core.tween(180), label = "title-color")
    val rowAlpha by animateFloatAsState(if (task.completed) .58f else 1f, androidx.compose.animation.core.tween(180), label = "row-alpha")
    Row(
        Modifier.fillMaxWidth().animateContentSize().clickable { edit(task) }.padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).semantics { contentDescription = if (task.completed) "Отметить как невыполненное" else "Отметить как выполненное"; role = Role.Checkbox },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(21.dp).border(1.5.dp, checkColor, CircleShape).clip(CircleShape)
                    .background(if (task.completed) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        toggle(task)
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(task.completed, enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = .6f), exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = .6f)) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.weight(1f).graphicsLayer { alpha = rowAlpha }) {
            Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis, color = titleColor, fontWeight = FontWeight.Medium, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
            val meta = buildList {
                task.time?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (task.duration > 0) add(formatMinutes(task.duration))
                task.category.takeIf { it != "Личное" }?.let { add(it) }
            }
            androidx.compose.animation.AnimatedVisibility(meta.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Text(meta.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (task.priority >= 3) Text("!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = { delete(task) }, modifier = Modifier.size(40.dp)) {
            Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .38f), thickness = 0.6.dp, modifier = Modifier.padding(start = 66.dp, end = 22.dp))
}

@Composable
private fun MonthScreen(tasks: List<TaskEntity>, selected: String, onSelect: (String) -> Unit) {
    var month by rememberSaveable { mutableStateOf(LocalDate.parse(selected).withDayOfMonth(1).toString()) }
    val first = LocalDate.parse(month)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val days = (0 until 42).map { start.plusDays(it.toLong()) }
    val monthTasks = tasks.filter { it.date.startsWith("%04d-%02d".format(first.year, first.monthValue)) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("МЕСЯЦ", color = Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    AnimatedContent(
                        targetState = first.month,
                        transitionSpec = { (slideInVertically { it / 3 } + fadeIn()).togetherWith(slideOutVertically { -it / 3 } + fadeOut()) },
                        label = "month-title"
                    ) { m -> Text(m.getDisplayName(TextStyle.FULL, ru).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineLarge) }
                }
                IconButton({ month = first.minusMonths(1).toString() }) { Text("‹", color = Secondary, fontSize = 25.sp) }
                IconButton({ month = first.plusMonths(1).toString() }) { Text("›", color = Secondary, fontSize = 25.sp) }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 22.dp).fillMaxWidth()) { week.forEach { Text(it, Modifier.weight(1f), color = Secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
        }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                days.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { date ->
                            val inMonth = date.month == first.month
                            val isToday = date == LocalDate.now()
                            val isSelected = date.toString() == selected
                            val dayTasks = tasks.filter { it.date == date.toString() }
                            val dayBg by animateColorAsState(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                animationSpec = androidx.compose.animation.core.tween(180),
                                label = "calendar-day"
                            )
                            val dayScale by animateFloatAsState(if (isSelected) 1f else .97f, spring(stiffness = Spring.StiffnessMedium), label = "calendar-scale")
                            Column(
                                Modifier.weight(1f).height(62.dp).padding(2.dp).scale(dayScale).clip(RoundedCornerShape(12.dp))
                                    .background(dayBg)
                                    .semantics { contentDescription = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, ru)}"; role = Role.Button }
                                    .clickable(enabled = inMonth) { onSelect(date.toString()) }.padding(6.dp)
                            ) {
                                Text(date.dayOfMonth.toString(), fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (inMonth) MaterialTheme.colorScheme.onSurface else Hairline)
                                Spacer(Modifier.height(6.dp))
                                if (dayTasks.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(minOf(dayTasks.size, 4)) { Box(Modifier.size(4.dp).clip(CircleShape).background(if (dayTasks.getOrNull(it)?.completed == true) Green else Secondary)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            val total = monthTasks.size; val done = monthTasks.count { it.completed }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                Text("${first.month.getDisplayName(TextStyle.FULL, ru).replaceFirstChar { it.uppercase() }} · $done из $total", color = Secondary, fontSize = 12.sp)
                if (total > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { done.toFloat() / total }, Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)), color = Green, trackColor = Hairline) }
            }
        }
    }
}

@Composable
private fun OverviewScreen(tasks: List<TaskEntity>) {
    val today = LocalDate.now()
    val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = last7.map { date -> tasks.count { it.date == date.toString() && it.completed } }
    val total = tasks.size
    val done = tasks.count { it.completed }
    val rate = if (total == 0) 0 else done * 100 / total
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageHeader("Обзор", "Ритм", action = "") {} }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                Text("$rate%", fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
                Text(if (total == 0) "Пока нечего оценивать" else "задач выполнено", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
                Text("Последние 7 дней", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    values.forEachIndexed { i, value ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val barHeight by animateDpAsState((5 + value.coerceAtMost(8) * 9).dp, spring(stiffness = Spring.StiffnessLow), label = "overview-bar")
                            Box(Modifier.width(28.dp).height(86.dp), contentAlignment = Alignment.BottomCenter) {
                                Box(Modifier.width(22.dp).height(barHeight).clip(RoundedCornerShape(5.dp)).background(if (value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                            }
                            Spacer(Modifier.height(6.dp)); Text(week[last7[i].dayOfWeek.value - 1], fontSize = 9.sp, color = Secondary)
                        }
                    }
                }
            }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f), thickness = 0.7.dp) }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text("Что видно", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text(analyticsText(tasks, values), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun MoreScreen(dark: Boolean, setDark: (Boolean) -> Unit, clear: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageHeader("Настройки", "Ещё", action = "") {} }
        item { SettingRow("Тёмная тема", "Спокойное оформление вечером") { Switch(dark, setDark) } }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f), thickness = 0.7.dp) }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text("Sprint", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp)); Text("Минималистичный планировщик месяца. Данные хранятся на устройстве.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f), thickness = 0.7.dp) }
        item { TextButton({ confirm = true }, Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { Text("Удалить все данные", color = MaterialTheme.colorScheme.error) } }
    }
    if (confirm) Dialog(onDismissRequest = { confirm = false }) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(22.dp)) {
                Text("Удалить всё?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(7.dp)); Text("Все задачи, цели и шаблоны будут удалены.", color = Secondary)
                Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ confirm = false }) { Text("Отмена") }
                    TextButton({ clear(); confirm = false }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Spacer(Modifier.height(3.dp)); Text(subtitle, color = Secondary, fontSize = 11.sp) }
        control()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditor(existing: TaskEntity?, defaultDate: String, onDismiss: () -> Unit, onSave: (TaskEntity) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.date?.ifBlank { defaultDate } ?: defaultDate) }
    var time by remember { mutableStateOf(existing?.time ?: "") }
    var duration by remember { mutableStateOf((existing?.duration ?: 30).toString()) }
    var priority by remember { mutableIntStateOf(existing?.priority ?: 1) }
    var repeat by remember { mutableStateOf(existing?.recurrence ?: "NONE") }
    var advanced by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "Новое действие" else "Изменить", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onDismiss) { Text("Закрыть", color = Secondary) }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Что нужно сделать?") }, shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = .7f))
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ showDatePicker(context, date) { date = it } }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(prettyDate(date)) }
                OutlinedButton({ showTimePicker(context, time) { time = it } }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text(time.ifBlank { "Время" }) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Длительность, мин") }, shape = RoundedCornerShape(16.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            TextButton({ advanced = !advanced }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                Text(if (advanced) "Скрыть детали" else "Дополнительно", color = Green)
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = advanced,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 }
            ) {
                Column {
                    OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().height(104.dp), label = { Text("Заметка") }, shape = RoundedCornerShape(16.dp), minLines = 3, maxLines = 4)
                    Spacer(Modifier.height(14.dp)); Text("Приоритет", fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Spacer(Modifier.height(6.dp))
                    Chips(listOf("Обычный", "Важный", "Срочный"), priority - 1) { priority = it + 1 }
                    Spacer(Modifier.height(14.dp)); Text("Повтор", fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Spacer(Modifier.height(6.dp))
                    Chips(listOf("Нет", "Ежедневно", "Еженедельно", "Ежемесячно"), listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").indexOf(repeat)) { repeat = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")[it] }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { if (title.isNotBlank()) { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onSave(TaskEntity(existing?.id ?: 0L, title.trim(), note.trim(), date, time.takeIf { it.isNotBlank() }, duration.toIntOrNull() ?: 30, priority, existing?.category ?: "Личное", repeat, existing?.completed ?: false, existing?.energy ?: 1, existing?.goal ?: "", existing?.createdAt ?: System.currentTimeMillis(), existing?.completedAt)) } },
                enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)
            ) { Text(if (existing == null) "Добавить" else "Сохранить") }
        }
    }
}

@Composable
private fun Chips(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label -> FilterChip(selected = selected == index, onClick = { onSelected(index) }, label = { Text(label) }) }
    }
}

private fun showDatePicker(context: Context, current: String, onDate: (String) -> Unit) {
    val d = runCatching { LocalDate.parse(current) }.getOrElse { LocalDate.now() }
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

private fun showTimePicker(context: Context, current: String, onTime: (String) -> Unit) {
    val parts = current.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(context, { _, h, m -> onTime("%02d:%02d".format(h, m)) }, hour, minute, true).show()
}

private fun prettyDate(value: String): String = runCatching {
    val d = LocalDate.parse(value); "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, ru).replaceFirstChar { it.uppercase() }}"
}.getOrElse { "Дата" }

private fun formatMinutes(m: Int) = if (m < 60) "$m мин" else if (m % 60 == 0) "${m / 60} ч" else "${m / 60} ч ${m % 60} мин"

private fun analyticsText(tasks: List<TaskEntity>, values: List<Int>): String {
    if (tasks.isEmpty()) return "Добавь несколько действий. Здесь появится спокойная картина твоего ритма — без лишних показателей."
    val planned = values.sum()
    return when {
        planned == 0 -> "Последние семь дней пока пусты. Это нормально: сначала создай небольшой план."
        planned >= 12 -> "Ритм активный. Если задач становится слишком много, сокращай план, а не пытайся ускориться."
        planned >= 5 -> "Ритм ровный. Лучше сохранять небольшой объём, чем заполнять каждый свободный час."
        else -> "Неделя лёгкая. Используй свободное место для отдыха или одного действительно важного дела."
    }
}

private fun recurring(task: TaskEntity): List<TaskEntity> {
    val base = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return emptyList()
    val count = when (task.recurrence) { "DAILY" -> 30; "WEEKLY" -> 12; "MONTHLY" -> 6; else -> 0 }
    return (1..count).map { i ->
        val date = when (task.recurrence) { "DAILY" -> base.plusDays(i.toLong()); "WEEKLY" -> base.plusWeeks(i.toLong()); "MONTHLY" -> base.plusMonths(i.toLong()); else -> base }
        task.copy(id = 0L, date = date.toString(), completed = false, completedAt = null)
    }
}

private fun schedule(context: Context, task: TaskEntity) {
    val value = task.time ?: return
    val parts = value.split(":")
    if (parts.size != 2 || task.date.isBlank()) return
    val date = runCatching { LocalDate.parse(task.date) }.getOrNull() ?: return
    val millis = runCatching { date.atTime(parts[0].toInt(), parts[1].toInt()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() ?: return
    if (millis <= System.currentTimeMillis()) return
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("title", task.title)
    val pending = PendingIntent.getBroadcast(context, task.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    runCatching { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending) }
}
