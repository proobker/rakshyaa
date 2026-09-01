# Proguard rules for Rakshyaa release builds.
# Release minification is currently disabled; these rules are for safety if
# minifyEnabled is turned on later.

# kotlinx-serialization needs reflection-based keep rules when minifying.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rakshyaa.rakshyaa.**$$serializer { *; }
-keepclassmembers class com.rakshyaa.rakshyaa.** {
    *** Companion;
}
-keepclasseswithmembers class com.rakshyaa.rakshyaa.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
