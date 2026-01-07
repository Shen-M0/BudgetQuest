// [修正] Import 必須放在檔案的最第一行 (plugins 之前)
import java.io.FileInputStream
import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    id("com.google.gms.google-services") // 啟用插件
}

// [注意] 這裡原本多餘的 import 已經刪除，因為最上方已經匯入過了

android {
    namespace = "com.example.budgetquest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.budgetquest"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 1. 載入 local.properties 檔案
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // 2. 讀取 Google Maps Key
        val mapsApiKey = localProperties.getProperty("GOOGLE_MAPS_API_KEY") ?: ""
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$mapsApiKey\"")

        // 3. 讀取 Exchange Rate Key (使用不同的變數名稱避免衝突)
        val exchangeApiKey = localProperties.getProperty("EXCHANGE_RATE_API_KEY") ?: ""
        buildConfigField("String", "EXCHANGE_RATE_API_KEY", "\"$exchangeApiKey\"")

        // --- [結束] API Key 讀取邏輯 ---

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // [重要] 必須設為 true 才能使用 BuildConfig
        buildConfig = true
    }
}

dependencies {
    // ... (維持不變)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // 或最新版本
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.0.0") // [新增] Google Sign-In

    // Retrofit (網路請求) & Gson (JSON 解析)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")


}