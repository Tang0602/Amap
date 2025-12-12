plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.amap_sim"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.amap_sim"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // GraphHopper 可能需要 MultiDex（方法数超过 64K）
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

    // GraphHopper 需要 Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // 启用 Jetpack Compose
    buildFeatures {
        compose = true
    }

    // GraphHopper 需要排除重复的 META-INF 文件
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // ========================================
    // AndroidX 基础
    // ========================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ========================================
    // Jetpack Compose
    // ========================================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose Debug/Preview 工具
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ========================================
    // Mapsforge - 离线地图渲染
    // ========================================
    // 核心库：地图渲染引擎（纯 Java，无 Native 依赖）
    implementation(libs.bundles.mapsforge)

    // ========================================
    // GraphHopper - 离线路由规划
    // ========================================
    // 路由引擎（排除不需要的服务端依赖）
    implementation(libs.graphhopper.core) {
        // 排除服务端依赖，减小 APK 体积
        exclude(group = "org.eclipse.jetty")
        exclude(group = "com.fasterxml.jackson.dataformat", module = "jackson-dataformat-xml")
    }

    // SLF4J Android 日志实现（GraphHopper 依赖）
    implementation(libs.slf4j.android)

    // ========================================
    // Kotlin Coroutines - 异步处理
    // ========================================
    implementation(libs.bundles.coroutines)

    // ========================================
    // 测试依赖
    // ========================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
