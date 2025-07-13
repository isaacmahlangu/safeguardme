# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# SafeguardMe Security Rules

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# SafetyEvidence models
-keep class com.safeguardme.app.data.models.SafetyEvidence { *; }
-keep class com.safeguardme.app.data.models.SafetySession { *; }
-keep class com.safeguardme.app.data.models.EvidenceType { *; }

# Speech recognition
-keep class android.speech.** { *; }

# Camera2 API
-keep class android.hardware.camera2.** { *; }

# Audio recording
-keep class android.media.** { *; }

# Location services
-keep class android.location.** { *; }
-keep class com.google.android.gms.location.** { *; }

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data classes and models (avoid breaking serialization)
-keep class com.safeguardme.app.data.models.** { *; }
-keepclassmembers class com.safeguardme.app.data.models.** { *; }

# Keep navigation and screen classes
-keep class com.safeguardme.app.navigation.** { *; }
-keep class com.safeguardme.app.ui.screens.** { *; }

# Security: Keep authentication logic public methods only
-keep class com.safeguardme.app.auth.AuthRepository {
    public <methods>;
}
-keepclassmembers class com.safeguardme.app.auth.** {
    public <methods>;
}

# Keep biometric authentication classes
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Fix for kotlin.uuid missing classes
-dontwarn kotlin.uuid.**
-keep class kotlin.uuid.** { *; }

# Fix for serialization issues
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Security: Remove debug information in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep crash reporting
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# === Kotlin 2.x coroutine internals ===
-keepclassmembers class kotlin.coroutines.jvm.internal.** { *; }

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile