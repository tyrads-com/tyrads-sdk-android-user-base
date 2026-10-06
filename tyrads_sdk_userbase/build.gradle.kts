plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    id("maven-publish")
}

android {
    namespace = "com.tyrads.sdk.userbase"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // Avoid invokedynamic StringConcatFactory in the AAR, since R8 (ours and publishers') can't resolve it.
        freeCompilerArgs += "-Xstring-concat=inline"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // api: JsonElement (activateCampaign's return type) is part of the public surface, so
    // consumers need kotlinx-serialization-json on their compile classpath too.
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    // api: init() takes okhttp3.Interceptor (optional extra interceptors, e.g. Chucker).
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.play.services.ads.identifier)
    implementation(libs.play.services.appset)
    implementation(libs.rootbeer.lib)

    // FCM token scoped to TyrAds' own Firebase project (see push/TyradsPushToken.kt). The host app
    // needs no Firebase setup of its own.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.tyrads-com"
                artifactId = "tyrads-sdk-android-user-base"
                version = "1.0.0"
            }
        }
    }
}
