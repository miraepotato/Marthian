plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.marthianclean"
    compileSdk = 36

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
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose Material3 (Compose components)
    implementation("androidx.compose.material3:material3")

    // ✅ Material3 Android resources (Theme.Material3.* 제공)
    implementation("androidx.compose.material3:material3-android:1.4.0")

    // ✅ (정리 단계 안전장치) Material Components resources
    // - 기존 themes.xml/styles.xml 어딘가에 Theme.MaterialComponents.* 참조가 남아있어도 빌드가 멈추지 않게 함
    implementation("com.google.android.material:material:1.11.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Google Maps SDK + Maps Compose
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.maps.android:maps-compose:8.0.0")
    implementation("androidx.compose.material:material-icons-extended")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
