# Add project specific ProGuard rules here.
-keep class com.filemanager.android.network.dto.** { *; }
-keep class com.filemanager.android.storage.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
