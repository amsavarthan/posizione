-keepattributes Signature
-keepclassmembers class com.amsavarthan.posizione.**{
*;
}
-keep class org.ocpsoft.prettytime.i18n.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$**{
**[] $VALUES;
public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder
-keep class androidx.appcompat.widget.**{
*;
}
