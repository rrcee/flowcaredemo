plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.application")
}
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework { baseName = "FlowCare"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            implementation("io.ktor:ktor-client-core:3.1.2")
            implementation("io.ktor:ktor-client-websockets:3.1.2")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("io.ktor:ktor-client-okhttp:3.1.2")
        }
        iosMain.dependencies { implementation("io.ktor:ktor-client-darwin:3.1.2") }
    }
}
android {
    namespace = "com.flowcare"
    compileSdk = 35
    defaultConfig { applicationId = "com.flowcare.demo"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "0.2.0" }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

compose.resources { packageOfResClass = "com.flowcare.resources" }
