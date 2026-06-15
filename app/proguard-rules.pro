# Opticast ProGuard rules.
# RootEncoder uses reflection in parts of its codec/source handling; keep its classes.
-keep class com.pedro.** { *; }
-dontwarn com.pedro.**
