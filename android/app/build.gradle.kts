plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.cdtc1127l.nova"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cdtc1127l.nova"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { buildConfig = true }
}
