import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

val ncpId = localProps.getProperty("NCP_MAPS_CLIENT_ID") ?: ""
val ncpSecret = localProps.getProperty("NCP_MAPS_CLIENT_SECRET") ?: ""

// ✅ 네이버 OpenAPI(지역검색) 키
val naverSearchId = localProps.getProperty("NAVER_SEARCH_CLIENT_ID") ?: ""
val naverSearchSecret = localProps.getProperty("NAVER_SEARCH_CLIENT_SECRET") ?: ""

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

        // ✅ NCP 지도/지오코딩용
        buildConfigField("String", "NCP_MAPS_CLIENT_ID", "\"$ncpId\"")
        buildConfigField("String", "NCP_MAPS_CLIENT_SECRET", "\"$ncpSecret\"")
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = ncpId

        // ✅ 네이버 OpenAPI(지역검색)용
        buildConfigField("String", "NAVER_SEARCH_CLIENT_ID", "\"$naverSearchId\"")
        buildConfigField("String", "NAVER_SEARCH_CLIENT_SECRET", "\"$naverSearchSecret\"")
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-android:1.4.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.naver.maps:map-sdk:3.21.0")
    implementation("io.github.fornewid:naver-map-compose:1.9.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== Test =====
    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
