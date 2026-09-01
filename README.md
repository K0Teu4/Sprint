# Sprint 1.1 — UX/UI completion batch

Sprint is a dark, minimal monthly planner focused on one flow: **month → day → task → completion**.

## What changed in this batch

- Dark theme is now the only visual theme and is treated as the product identity.
- Removed task duration because it had no meaningful role in planning or analytics.
- Removed the hidden "Advanced" layer. Date, time, priority, repeat, reminder and note are visible in the task editor.
- Notes are shown directly under the task title when present.
- Priority now has an explicit effect: important tasks are ordered above normal tasks; urgent tasks receive a stronger visual marker.
- Repeat rules are explicit: daily = next 30 days, weekly = next 12 weeks, monthly = next 12 months.
- Week switching remains button/tap based; there is no swipe navigation.
- Month navigation has visible previous/next controls and a return-to-today action.
- Overview can move through previous/next weeks and return to the current week.
- Settings now expose reminders and micro-haptics instead of unused/hidden controls.
- Navigation bar is hidden by the Activity to keep the app surface clean; users can temporarily reveal system navigation with the system gesture.
- Removed unused Goal/Template database entities from the new product surface.
- Reminder scheduling is tied to the task's explicit reminder switch.
- Deleting a task also cancels its pending reminder.

## Data

Room database version 4 uses a destructive migration because this is a development build and the previous schema contained unused fields and entities. For a production release, a real migration should be introduced before publishing an update.

## Build

The project uses Gradle 8.11.1, Kotlin 2.1.0 and Jetpack Compose/Material 3. The supplied environment cannot download the Gradle distribution, so `assembleDebug` must be verified in Android Studio or another network-enabled Gradle environment.
