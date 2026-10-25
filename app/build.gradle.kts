plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

room {
    // Exported schemas are committed so every migration can be verified against the exact previous version.
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.bluebird.messaging"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bluebird.messaging"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Optimized exactly like release, but signed with this computer's local debug key so it can be installed
        // for real-world performance testing. Not for distribution: real release signing is set up separately.
        create("preview") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric needs real resources to render Compose screenshots on the JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Applies the baseline profiles Compose and other libraries ship, so sideloaded builds start and scroll faster.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Backdrop blur for iOS-style frosted glass bars (GPU blur on Android 12+, translucent fallback below).
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.glass)

    // Local persistence.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    // JVM screenshot tests (Robolectric + Roborazzi): render real composables without an emulator.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// DatabaseSchemaTest reads the exported schemas from disk; make sure a schema change always re-runs the tests.
tasks.withType<Test>().configureEach {
    inputs.dir("schemas").withPathSensitivity(PathSensitivity.RELATIVE)
}
