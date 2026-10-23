# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.madoui.fatoura.** {
    *** Companion;
}
-keepclasseswithmembers class com.madoui.fatoura.** {
    kotlinx.serialization.KSerializer serializer(...);
}
