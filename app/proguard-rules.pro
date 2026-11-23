# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mannanhosen.gymflow.**$$serializer { *; }
-keepclassmembers class com.mannanhosen.gymflow.** {
    *** Companion;
}
-keepclasseswithmembers class com.mannanhosen.gymflow.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep data models used with reflection/serialization
-keep class com.mannanhosen.gymflow.data.remote.dto.** { *; }