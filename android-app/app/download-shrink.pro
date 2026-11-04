-dontobfuscate
-dontoptimize
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class io.github.polestarcn.companion.** { *; }

-keep class io.github.polestarcn.oppobridge.** { *; }

# OPPO SDK models are serialized across the official service boundary.
-keep class com.oplus.ocs.** { *; }
