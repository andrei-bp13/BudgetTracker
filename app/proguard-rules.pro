# Keep Gson-serialized backup models and their fields from obfuscation
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.pardonmyspending.app.data.model.** { *; }
-keepclassmembers enum com.pardonmyspending.app.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Gson TypeToken subclasses
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
