# Keep Room entities and DAOs
-keep class ru.sprint.app.data.TaskEntity { *; }
-keep class ru.sprint.app.data.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Kotlin metadata for reflection
-keepclassmembers class **$WhenMappings { <fields>; }

# Keep JSON serialization
-keep class org.json.** { *; }
-dontwarn org.json.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}