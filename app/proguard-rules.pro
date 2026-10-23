# Bouncy Castle discovers algorithm mappings and SPI implementations by class name.
# Without these rules R8 strips EdEC/DH/MD5 and SSHJ falls back to Conscrypt keys
# that it cannot encode on some Android versions (SSHJ issue #1032).
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }

# Optional desktop LDAP certificate stores are never used for SSH public-key authentication.
-dontwarn javax.naming.**

# WindowManager probes optional vendor extensions at runtime on devices that provide them.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
