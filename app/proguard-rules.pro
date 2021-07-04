-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.kamui.rin.**$$serializer { *; }
-keepclassmembers class com.kamui.rin.* {
    *** Companion;
}
-keepclasseswithmembers class com.kamui.rin.* {
    kotlinx.serialization.KSerializer serializer(...);
}