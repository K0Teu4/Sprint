package ru.sprint.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.sprint.app.data.TaskEntity
import java.time.LocalDate

private fun task(
    id: Long = 0L, title: String = "Тест", date: String = "2026-09-04",
    time: String? = null, priority: Int = 1, recurrence: String = "NONE", completed: Boolean = false
) = TaskEntity(id = id, title = title, note = "", date = date, time = time, priority = priority,
    recurrence = recurrence, reminder = false, completed = completed, createdAt = 0L,
    completedAt = null, category = "PERSONAL", parentId = null, seriesId = null)

class ParseQuickTaskTest {
    private val defaultDate = "2026-09-04" // пятница
    private fun parse(raw: String) = parseQuickTask(raw, defaultDate, "PERSONAL")

    @Test fun `простой текст без метаданных`() {
        val t = parse("Позвонить маме")
        assertEquals("Позвонить маме", t.title); assertEquals(defaultDate, t.date)
        assertEquals(null, t.time); assertEquals(1, t.priority)
        assertEquals("NONE", t.recurrence); assertEquals("PERSONAL", t.category)
    }
    @Test fun `время извлекается из текста`() {
        val t = parse("Позвонить в 18:00")
        assertEquals("18:00", t.time); assertEquals("Позвонить", t.title)
    }
    @Test fun `завтра плюс один день`() { assertEquals("2026-09-05", parse("завтра отчет").date) }
    @Test fun `послезавтра плюс два дня`() { assertEquals("2026-09-06", parse("послезавтра визит").date) }
    @Test fun `сегодня не меняет дату`() { assertEquals(defaultDate, parse("сегодня уборка").date) }
    @Test fun `день недели после пятницы`() { assertEquals("2026-09-07", parse("в понедельник созвон").date) }
    @Test fun `срочно это приоритет 3`() { assertEquals(3, parse("срочно оплатить счет").priority) }
    @Test fun `важно это приоритет 2`() { assertEquals(2, parse("важно подготовить доклад").priority) }
    @Test fun `каждый день это DAILY`() { assertEquals("DAILY", parse("зарядка каждый день").recurrence) }
    @Test fun `каждую неделю это WEEKLY`() { assertEquals("WEEKLY", parse("итог каждую неделю").recurrence) }
    @Test fun `категория определяется и убирается из заголовка`() {
        val t = parse("сделать отчет работа")
        assertEquals("WORK", t.category); assertFalse(t.title.contains("работа"))
    }
    @Test fun `английские tomorrow и urgent`() {
        val t = parseQuickTask("call client tomorrow urgent", defaultDate, "PERSONAL")
        assertEquals("2026-09-05", t.date); assertEquals(3, t.priority)
    }
    @Test fun `пустой ввод не падает`() { assertEquals("", parse("").title) }
}

class TaskComparatorTest {
    @Test fun `невыполненные выше выполненных`() {
        assertEquals(listOf(2L, 1L), listOf(task(1, completed = true), task(2)).sortedWith(taskComparator()).map { it.id })
    }
    @Test fun `высокий приоритет выше`() {
        assertEquals(listOf(2L, 1L), listOf(task(1, priority = 1), task(2, priority = 3)).sortedWith(taskComparator()).map { it.id })
    }
    @Test fun `раннее время выше а без времени внизу`() {
        assertEquals(listOf(3L, 2L, 1L), listOf(task(1), task(2, time = "18:00"), task(3, time = "09:00")).sortedWith(taskComparator()).map { it.id })
    }
    @Test fun `при равенстве по id`() {
        assertEquals(listOf(3L, 5L), listOf(task(5), task(3)).sortedWith(taskComparator()).map { it.id })
    }
}

class RecurringTest {
    private val base = task()
    @Test fun `ежедневно 30 начиная с завтра`() {
        val l = recurring(base.copy(recurrence = "DAILY"))
        assertEquals(30, l.size); assertEquals("2026-09-05", l.first().date); assertEquals("2026-10-04", l.last().date)
        assertTrue(l.all { it.recurrence == "NONE" && !it.completed })
    }
    @Test fun `еженедельно 12 по семь дней`() {
        val l = recurring(base.copy(recurrence = "WEEKLY"))
        assertEquals(12, l.size); assertEquals("2026-09-11", l.first().date)
    }
    @Test fun `ежемесячно 12`() {
        val l = recurring(base.copy(recurrence = "MONTHLY"))
        assertEquals(12, l.size); assertEquals("2026-10-04", l.first().date)
    }
    @Test fun `NONE без повторений`() { assertTrue(recurring(base).isEmpty()) }
}

class UiTextAndDatesTest {
    private val ru = UiText(false); private val en = UiText(true)
    private val d = LocalDate.of(2026, 9, 4)
    @Test fun `месяц именительный с заглавной`() { assertEquals("Сентябрь", ru.month(d)) }
    @Test fun `месяц родительный строчный`() { assertEquals("сентября", ru.monthGenitive(d)) }
    @Test fun `месяц английский`() { assertEquals("September", en.month(d)) }
    @Test fun `v выбирает язык`() { assertEquals("Дата", ru.v("Дата", "Date")); assertEquals("Date", en.v("Дата", "Date")) }
    @Test fun `категории`() { assertEquals("Работа", ru.category("WORK")); assertEquals("Personal", en.category("PERSONAL")) }
    @Test fun `prettyDate в родительном падеже`() { assertEquals("4 сентября", prettyDate("2026-09-04", ru)) }
    @Test fun `prettyDate с битой датой не падает`() { assertEquals("Дата", prettyDate("не дата", ru)) }
    @Test fun `weekRange внутри месяца`() { assertEquals("1–7 сентября", weekRange(LocalDate.of(2026, 9, 1), ru)) }
    @Test fun `weekRange через границу месяца`() { assertEquals("31 августа — 6 сентября", weekRange(LocalDate.of(2026, 8, 31), ru)) }
    @Test fun `подписи повторений`() {
        assertEquals("ежедневно", recurrenceLabel("DAILY", ru)); assertEquals("", recurrenceLabel("NONE", ru))
        assertTrue(recurrenceDescription("WEEKLY", ru).isNotBlank())
    }
}