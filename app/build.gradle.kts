plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.marthianclean"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.marthianclean"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // ===== Compose BOM =====
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // ===== Compose Core =====
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")

    // ===== Debug =====
    debugImplementation("androidx.compose.ui:ui-tooling")
}
