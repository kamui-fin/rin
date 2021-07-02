-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# Change here com.yourcompany.yourpackage
-keep,includedescriptorclasses class com.kamui.rin.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.kamui.rin.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.kamui.rin.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}