# RehearsAll ProGuard Rules

# Room — keep entity classes
-keep class com.rehearsall.data.db.entity.** { *; }

# Hilt — keep generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Media3 — keep service and session
-keep class androidx.media3.session.** { *; }
-keep class com.rehearsall.playback.RehearsAllPlaybackService { *; }

# Timber — remove debug logs in release
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# Kotlin serialization — keep metadata
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
