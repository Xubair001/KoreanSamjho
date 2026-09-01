# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.koreansamjho.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.koreansamjho.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Room generated
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**
