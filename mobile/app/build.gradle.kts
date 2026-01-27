plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cartshareapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cartshareapp"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    // ---------- Core & Lifecycle ----------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.splashscreen)

    // ---------- Compose (Managed by BOM) ----------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ---------- Auth & Credentials ----------
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // CHANGED: Removed .ktx to match the new TOML
    implementation(libs.firebase.auth)

    // ---------- Navigation & Hilt ----------
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ---------- Room Database ----------
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ---------- Networking & Storage ----------
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.material)

    // ---------- Firebase (BoM Managed) ----------
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.kotlinx.coroutines.play.services)

    // ---------- Testing ----------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}