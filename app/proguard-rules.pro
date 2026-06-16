# Opticast ProGuard/R8 rules.

# RootEncoder uses reflection in parts of its codec/source handling; keep its classes.
-keep class com.pedro.** { *; }
-dontwarn com.pedro.**

# Tink (via androidx.security:security-crypto) references compile-only annotations
# that aren't on the runtime classpath. Safe to ignore.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
