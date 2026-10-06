plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    // Only so the example looks like a typical publisher app that has its own default Firebase app.
    // The SDK never reads it: it fetches its FCM token from TyrAds' own Firebase project.
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.tyrads.sdk.userbase.example"
    compileSdk = 35

    defaultConfig {
        // Staging identifier: internal-only branch, never merged into main. The publisher-facing
        // example lives on `main` with the production applicationId instead.
        applicationId = "com.example.androiduserbase.stag"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    buildTypes {
        release {
            // Debug keystore so QA can install release builds directly — not for Play Store.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Consumes the published JitPack artifact, exactly like a publisher would (mirrors the RN
    // example depending on the published npm package rather than the local source).
    implementation("com.github.tyrads-com:tyrads-sdk-android-user-base:v1.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
