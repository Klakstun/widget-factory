# Add project specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.widgetfactory.app.**$$serializer { *; }
-keepclassmembers class com.widgetfactory.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.widgetfactory.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
