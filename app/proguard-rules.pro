# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep line numbers for Crashlytics/Play Console debugging
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# Safely keep field names in all the models so Firebase .getValue(Class) doesn't break
-keepclassmembers class com.rmads.maker.** {
    <fields>;
    <init>();
}

# Keep the actual JSON Model classes if Gson is used
-keep class com.rmads.maker.*Model { *; }
-keep class com.rmads.maker.*Item { *; }
-keep class com.rmads.maker.*Data { *; }

# Keep Firebase/Gson specifics
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}