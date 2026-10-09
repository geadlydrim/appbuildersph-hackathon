import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.commutenity"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.commutenity"
        minSdk = 31
        targetSdk = 35
        ndk {
            abiFilters += "arm64-v8a"
        }
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        noCompress += "pmtiles"
    }

    sourceSets {
        getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/packAssets"))
    }
}

// Bundle the hero commute pack and the mock rider Q&A from the single source files in /data; never hand-copy them into assets.
val copyPackAssets = tasks.register<Copy>("copyPackAssets") {
    from("${rootDir}/../data/pack/hero-trip.source.json") {
        into("pack")
        rename { "hero-trip.json" }
    }
    from("${rootDir}/../data/mock/rider-qa.json") {
        into("mock")
    }
    into(layout.buildDirectory.dir("generated/packAssets"))
}

tasks.named("preBuild") {
    dependsOn(copyPackAssets)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.maplibre.android)
    implementation(libs.litertlm.android)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
