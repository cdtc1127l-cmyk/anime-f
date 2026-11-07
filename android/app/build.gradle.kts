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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            // Sign the release variant with the standard debug keystore so the CI APK is directly installable.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
