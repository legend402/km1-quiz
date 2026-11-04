plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.km1quiz.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.km1quiz.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 个人安装：release 用 debug 签名（可直接安装）。上架商店请替换为自己的 keystore。
            signingConfig = signingConfigs.getByName("debug")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // JVM 单测中提供 org.json（Android 框架的 org.json 在本地测试不可用）
    testImplementation("org.json:json:20240303")
}

// 题库与图片从仓库 data/ 复制进 assets：数据不进 git、不在安卓工程里重复存放
val copyQuizData by tasks.registering(Copy::class) {
    from(rootDir.parentFile.resolve("data").resolve("bank.json"))
    from(rootDir.parentFile.resolve("data").resolve("images")) { into("images") }
    into(layout.projectDirectory.dir("src/main/assets"))
}

tasks.named("preBuild") { dependsOn(copyQuizData) }

// 单测工作目录统一为 android/，BankTest 用相对路径 ../data/bank.json 读真实题库
tasks.withType<Test>().configureEach {
    workingDir = rootProject.projectDir
}
