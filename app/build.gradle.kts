plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.amap_sim"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.amap_sim"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // GraphHopper 需要 MultiDex
        multiDexEnabled = true
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

    // GraphHopper 需要排除重复的 META-INF
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // AndroidX 核心
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // SplashScreen
    implementation(libs.androidx.splashscreen)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Mapsforge 离线地图
    implementation(libs.mapsforge.core)
    implementation(libs.mapsforge.map.android)
    implementation(libs.mapsforge.themes)
    implementation(libs.mapsforge.poi.android)

    // BRouter 离线路由（从 libs 目录加载）
    // 需要先编译 BRouter 并将 JAR 放入 app/libs/ 目录
    // 或使用 JitPack: implementation("com.github.abrensch:brouter:1.7.5")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // GraphHopper 离线路由（已弃用，保留作为备选）
    // 如需使用 GraphHopper，取消下面的注释并注释掉上面的 BRouter
    // implementation(libs.graphhopper.core) {
    //     exclude(group = "org.eclipse.jetty")
    //     exclude(group = "com.fasterxml.jackson.dataformat", module = "jackson-dataformat-xml")
    // }
    
    // SLF4J 日志（BRouter/GraphHopper 都需要）
    implementation(libs.slf4j.android)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}