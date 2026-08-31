plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.blockabsbebsh.birdy"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.blockabsbebsh.birdy"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = requireNotNull(System.getenv("ANDROID_KEYSTORE_PASSWORD"))
                keyAlias = requireNotNull(System.getenv("ANDROID_KEY_ALIAS"))
                keyPassword = requireNotNull(System.getenv("ANDROID_KEY_PASSWORD"))
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
