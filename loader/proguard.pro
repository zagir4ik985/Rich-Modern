-injars build/libs/zaga-loader-1.0.0-all.jar
-outjars build/libs/zaga-loader-1.0.0-obf.jar

-dontwarn
-dontoptimize
-obfuscation

-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,Deprecated

-keep public class zaga.Main {
    public static void main(java.lang.String[]);
}

-keep class com.google.gson.** { *; }

-keep class zaga.api.ApiClient {
    public <methods>;
}
-keep class zaga.api.ApiClient$* { *; }

-repackageclasses z
-allowaccessmodification
-overloadaggressively
