-keep class com.tyrads.sdk.userbase.** { *; }
-keepclassmembers class com.tyrads.sdk.userbase.** { *; }
-keepnames class com.tyrads.sdk.userbase.**
-keepattributes *Annotation*
-dontobfuscate

# kotlinx.serialization
-keepattributes InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
