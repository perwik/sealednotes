plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.sealednotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sealednotes"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    // Compose BOM — manages all Compose library versions
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Compose core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & navigation
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Room database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // SQLCipher for database encryption
    implementation("net.zetetic:sqlcipher-android:4.6.1@aar")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // QR code generation (for encryption key export)
    implementation("com.google.zxing:core:3.5.3")

    // QR code scanning (for encryption key import)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")

    // DocumentFile for SAF folder traversal (Takeout folder import)
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
