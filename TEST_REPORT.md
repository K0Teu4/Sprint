# Sprint 1.3 verification report

Date: 2026-09-02

## Automated/static checks

- ZIP integrity: PASS
- XML parsing for manifest/layout/widget/theme/resources: PASS
- Kotlin delimiter balance for all Kotlin sources: PASS
- Kotlin parser/compiler syntax diagnostics: no syntax/expecting/unclosed diagnostics; dependency resolution is unavailable in the standalone compiler environment
- Gradle build: BLOCKED before compilation because Gradle 8.11.1 cannot be downloaded in the offline environment

## Functional logic scenarios reviewed

1. Quick Add: title/date/time/priority/recurrence/category extraction reviewed for RU/EN forms.
2. Quick Add with `завтра в 18:00` / `tomorrow at 18:00`: prepositions are removed from the final title.
3. Weekday Quick Add with `в понедельник` / `on Monday`: weekday and preposition are removed from the title.
4. Recurrence: base task keeps recurrence; generated occurrences have recurrence NONE and share seriesId with the base.
5. Editing recurring root: future occurrences are removed and regenerated, preventing duplicate series.
6. Deleting recurring root: entire series is removed; deleting an individual occurrence removes only that occurrence.
7. Undo: direct subtasks restore under the original parent; recurring relationships are remapped to the restored base ID.
8. Import/export: stable IDs, parentId and seriesId are serialized; imported hierarchy is restored.
9. Widget: root opens the app; `+` opens Quick Add; empty rows are hidden; up to four active/root tasks are shown.
10. Reminder: completed tasks cancel their pending reminder; reboot/timezone/time-change receiver reschedules future reminders.
11. Calendar export: uses Android ACTION_INSERT / CalendarContract event extras.
12. Task editor: selected time can be explicitly removed.

## Remaining device-only checks

- Android launcher widget placement/resizing on several launchers.
- Real-device horizontal swipe vs vertical scroll arbitration.
- Notification delivery under Doze and OEM battery restrictions.
- Calendar app availability and event insertion UX.
- Android 12–15/16+ behavior across OEMs.
