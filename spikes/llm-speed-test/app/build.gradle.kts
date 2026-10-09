plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ph.commutenity.spike"
    compileSdk = 36

    defaultConfig {
        applicationId = "ph.commutenity.spike"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "spike"
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        // Release-like timing: not debuggable. Signed with the debug key so it installs.
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
}
