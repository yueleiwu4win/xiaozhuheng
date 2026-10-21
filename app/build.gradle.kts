// 可选的原生 Compose 截图验收；常规编译不加载或下载此工具。
buildscript {
    if (project.hasProperty("visualQa")) {
        repositories { mavenCentral(); google() }
        dependencies { classpath("app.cash.paparazzi:paparazzi-gradle-plugin:1.3.5") }
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

if (project.hasProperty("visualQa")) {
    apply(plugin = "app.cash.paparazzi")
}

android {
    namespace = "com.example.bookkeeping"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bookkeeping"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "1.1.7"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // 用正式运行模式体验启动性能，沿用本地调试签名以支持覆盖现有安装。
        create("preview") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += "release"
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

    if (project.hasProperty("visualQa")) {
        sourceSets.getByName("test").java.srcDir("src/visualQa/java")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// 交付名称固定为用户指定的品牌与版本，后续也可单独执行此任务打包。
tasks.register<Copy>("packageXiaozhuheng") {
    dependsOn("assemblePreview")
    from(layout.buildDirectory.file("outputs/apk/preview/app-preview.apk"))
    into(layout.buildDirectory.dir("outputs/distribution"))
    rename { "xiaozhuheng-v${android.defaultConfig.versionName}.apk" }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
