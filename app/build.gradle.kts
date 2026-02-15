plugins {
    id("com.android.application")
    // 충돌 방지: 추가적인 kotlin.android 선언 없이 compose 플러그인만 유지합니다.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.marthianclean"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.marthianclean"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 에러를 일으키던 kotlinOptions { ... } 블록을 삭제했습니다.
    // 최신 그래들은 compileOptions의 Java 버전을 따라갑니다.
}

dependencies {
    // ===== Compose BOM =====
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // ===== Compose UI =====
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")

    // ===== Material Design 3 =====
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-android:1.4.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material-icons-extended")

    // ===== Navigation =====
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ===== Google Maps & Naver Map =====
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.naver.maps:map-sdk:3.21.0")
    implementation("io.github.fornewid:naver-map-compose:1.9.0")

    // ===== Lifecycle & ViewModel =====
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ===== 네트워크 (Retrofit2) - 401 에러 해결을 위한 핵심 =====
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== Debug =====
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}