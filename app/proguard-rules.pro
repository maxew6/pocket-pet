# Add project specific ProGuard rules here.
# Room, Hilt, and Compose all ship their own consumer-proguard-rules inside their AARs and are
# applied automatically by R8 — nothing project-specific is needed for them here.

# kotlinx.serialization: keep the generated serializer() companion for every @Serializable class
# reached from core:data's weather DTOs, since it's looked up reflectively at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class com.pocketpet.core.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.pocketpet.core.data.remote.**
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Pocket Pet's own model layer intact — small, and worth being defensive about since these
# cross Room/DataStore/JSON boundaries by field name and enum constant name.
-keep class com.pocketpet.core.model.** { *; }
-keep class com.pocketpet.core.database.entity.** { *; }

# AccessibilityService / NotificationListenerService subclasses are referenced only from the
# manifest, not from code — make sure R8 doesn't strip them as "unreachable".
-keep class com.pocketpet.service.overlay.OverlayService { *; }
-keep class com.pocketpet.service.overlay.accessibility.PetAccessibilityService { *; }
-keep class com.pocketpet.service.overlay.notification.PetNotificationListenerService { *; }
