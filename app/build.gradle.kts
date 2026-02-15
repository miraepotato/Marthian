import java.util.Properties

plugins {
    id("com.android.application")
    // 충돌 방지: 추가적인 kotlin.android 선언 없이 compose 플러그인만 유지합니다.
    id("org.jetbrains.kotlin.plugin.compose")
}

// ✅ local.properties 직접 로드
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}
val ncpId = localProps.getProperty("NCP_MAPS_CLIENT_ID") ?: ""
val ncpSecret = localProps.getProperty("NCP_MAPS_CLIENT_SECRET") ?: ""

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

        // ✅ local.properties -> BuildConfig
        buildConfigField("String", "NCP_MAPS_CLIENT_ID", "\"$ncpId\"")
        buildConfigField("String", "NCP_MAPS_CLIENT_SECRET", "\"$ncpSecret\"")

        // ✅ Manifest placeholder (Naver Map meta-data)
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = ncpId
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

    // BuildConfig를 쓰려면 켜줘야 합니다(AGP 버전에 따라 기본이 꺼져있을 수 있음)
    buildFeatures {
        buildConfig = true
    }
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

    // ===== 네트워크 (Retrofit2) =====
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== Unit Test (방법 B) =====
    testImplementation("junit:junit:4.13.2")

    // ===== Android Instrumentation Test (방법 B) =====
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose UI Test (androidTest)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // ===== Debug =====
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
