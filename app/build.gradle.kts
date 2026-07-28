plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.shiji.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shiji.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Default: use debug keystore so assembleRelease works out-of-the-box.
        // Replace with your own keystore for Play Store publishing.
        create("release") {
            storeFile = file("../shiji-release.jks")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String? ?: "shiji"
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Fall back to debug signing if no release keystore is configured.
            signingConfig = if (signingConfigs.getByName("release").storeFile?.exists() == true) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:ai"))
    implementation(project(":core:camera"))
    implementation(project(":core:voice"))

    // Feature modules
    implementation(project(":feature:diet"))
    implementation(project(":feature:health"))
    implementation(project(":feature:settings"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Room
    implementation(libs.bundles.room)

    // Network (AI client provided here, adapters live in core:ai)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // CameraX (in-app food capture)
    implementation(libs.bundles.camerax)

    // Image loading (captured photo preview)
    implementation(libs.coil.compose)

    // Navigation
    implementation(libs.compose.navigation)

    // Hilt
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)

    // Room
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
