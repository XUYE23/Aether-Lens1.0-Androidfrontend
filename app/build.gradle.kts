plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aether.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aether.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // 科大讯飞 SparkChain SDK（语音听写）
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Preferences DataStore — 本地持久化用户昵称和头像 URI
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil — 图片异步加载
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Material Icons Extended（ArrowBack / AutoMirrored）
    implementation("androidx.compose.material:material-icons-extended")

    // Gson — API 配置列表序列化
    implementation("com.google.code.gson:gson:2.10.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Google Fonts for Compose (Fraunces, Inter, Noto Serif/Sans SC, JetBrains Mono)
    implementation("androidx.compose.ui:ui-text-google-fonts")

    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.test:core:1.5.0")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
