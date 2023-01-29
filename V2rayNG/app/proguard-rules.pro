#=======================<Picasso>===========================
-dontwarn com.squareup.okhttp.**
-dontwarn org.apache.http.**
-dontwarn android.support.v7.**
-dontwarn android.support.v4.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-keepattributes *Annotation,Signature
-dontwarn com.github.siyamed.**
-keep class com.github.siyamed.shapeimageview.**{ *; }
#-keep class android.support.v7.widget.SearchView { *; }
-keepattributes *Annotation*

#-keep class com.facebook.crypto.** { *; }
#-keep class com.facebook.jni.** { *; }
#-keepclassmembers class com.facebook.cipher.jni.** { *; }
#
#-dontwarn com.facebook.**
#========================<Volley>==========================
-keep class com.android.volley.** { *; }
-keep class org.apache.commons.logging.**

-keepattributes *Annotation*

-dontwarn org.apache.**

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##---------------End: proguard configuration for Gson  ----------


#========================<nevisa lib>==========================
-keep class co.nevisa.commonlib.admob.models.models.** { *; }
-keep class co.nevisa.commonlib.firebase.models.firebase.models.** { *; }

#========================<App>==========================
#-keep class co.dev.** { *; }
-keep class co.dev.models.** { *; }
-keep class com.v2ray.ang.** { *; }
#-keep class com.v2ray.ang.ui.MainRecyclerAdapter.** { *; }
#-keep class com.v2ray.ang.dto.V2rayConfig.** { *; }
#-keep class com.v2ray.ang.dto.EConfigType.** { *; }
#-keep class com.v2ray.ang.dto.ERoutingMode.** { *; }
#-keep class com.v2ray.ang.viewmodel.** { *; }
-keep class rx.** { *; }
#-keep class android.widget.** { *; }
#
#-dontobfuscate
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
#-keepattributes *Annotation*

#-keep class libv2ray.** { *; }
#-keep class go.Seq.** { *; }


#========================<mmkv>==========================
# Keep all native methods, their classes and any classes in their descriptors
-keepclasseswithmembers,includedescriptorclasses class com.tencent.mmkv.** {
    native <methods>;
    long nativeHandle;
    private static *** onMMKVCRCCheckFail(***);
    private static *** onMMKVFileLengthError(***);
    private static *** mmkvLogImp(...);
    private static *** onContentChangedByOuterProcess(***);
}
#========================<Flurry>==========================
# Required to preserve the Flurry SDK
-keep class com.flurry.** { *; }
-dontwarn com.flurry.**
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepclasseswithmembers class * {
   public <init>(android.content.Context, android.util.AttributeSet, int);
 }

 # Google Play Services library
 -keep class * extends java.util.ListResourceBundle {
   protected Object[][] getContents();
}

 -keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
  public static final *** NULL;
 }

 -keepnames @com.google.android.gms.common.annotation.KeepName class *
 -keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
  }

 -keepnames class * implements android.os.Parcelable {
  public static final ** CREATOR;
 }

 -dontwarn java.util.concurrent.Flow*