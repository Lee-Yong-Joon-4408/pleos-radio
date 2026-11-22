import java.util.Properties

// Optional: GOOGLE_CLOUD_API_KEY=... in local.properties (git-ignored) enables live captions out of the box.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pleos.radio"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.pleos.radio"
        minSdk = 28           // Pleos Connect guide: API 28+
        targetSdk = 35
        versionCode = 1
        versionName = "0.3.0"
        // On-device speech recognition (whisper.cpp) is built for arm64 (Pleos Connect hardware and emulator).
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild {
            cmake {
                // Always optimise native code: an -O0 whisper build is far too slow for live captions.
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DANDROID_STL=c++_static")
            }
        }
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"${localProps.getProperty("GOOGLE_CLOUD_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // android.car (CarUxRestrictionsManager): present on Android Automotive, guarded at runtime elsewhere.
    useLibrary("android.car")

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)

    // Real FM audio: broadcasters' official HLS simulcasts
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)

    // Station logos (loaded from the broadcasters' own image URLs, disk-cached)
    implementation(libs.coil.compose)

    // Free on-device translation for live captions (models downloaded once, then offline)
    implementation(libs.mlkit.translate)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
