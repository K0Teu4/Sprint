# Sprint — минималистичный планировщик

Android-проект на Kotlin + Jetpack Compose + Room.

## Текущий релиз
- Version 1.3 / versionCode 3
- Неделя / Месяц / Год / Настройки
- Переключение недели и месяца свайпом
- RU / EN
- Приоритеты: обычный / важный / срочный
- Типы задач: работа / хобби / личное / покупки
- Фильтрация недели по типу
- Одноуровневые подзадачи с отдельной визуальной иерархией
- Быстрое добавление одной строкой: дата, время, приоритет, повторение и тип
- Полный редактор для заметок и расширенных параметров
- Напоминания + восстановление после перезагрузки / изменения времени
- Добавление задачи в системный календарь через стандартный Calendar Intent
- JSON backup / restore
- Минималистичный Android App Widget «Сегодня»
- Dark-first визуальная система с аккуратными градиентами и micro-interactions

## Принцип UX
Sprint не пытается копировать power-user планировщики. Основная идея — быстро увидеть день, быстро добавить дело и не перегружать интерфейс.

Подзадачи ограничены одним уровнем: они визуально вложены в родительскую задачу и не конкурируют с ней по визуальному весу.


## Final product pass — 1.3

- Quick Add opens directly from the home-screen widget.
- Recurring tasks use a series identifier; editing a recurring root rebuilds its future occurrences instead of duplicating them.
- Deleting a recurring root removes its generated occurrences; deleting a single occurrence removes only that occurrence.
- Undo restores the deleted task hierarchy and recurring series relationships.
- JSON backup includes stable IDs, parent relationships, and recurrence series information. Import restores one-level subtask hierarchy.
- Task editor can explicitly remove a previously selected time.
- Widget hides empty rows and exposes accessible task descriptions.
- Widget provider is exported as required for launcher delivery.
- Inexact alarms are intentionally used for reminders; exact alarms would require additional special-access handling on Android 12+.

### Verification

Static verification completed for XML parsing, archive integrity, source delimiter balance, and pure date/quick-add scenarios. Full Android compilation is environment-blocked because the Gradle 8.11.1 distribution is not available locally and outbound network access is disabled.
