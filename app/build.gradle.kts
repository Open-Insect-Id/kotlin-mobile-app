import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val dotenv = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

fun env(name: String): String? =
    System.getenv(name) ?: dotenv.getProperty(name)


android {
    namespace = "org.openinsectid.app"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        create("release") {
            val keystore = env("KEYSTORE_FILE")
            val storePass = env("KEYSTORE_PASSWORD")
            val alias = env("KEY_ALIAS")
            val keyPass = env("KEY_PASSWORD")

            if (
                !keystore.isNullOrBlank() &&
                !storePass.isNullOrBlank() &&
                !alias.isNullOrBlank() &&
                !keyPass.isNullOrBlank()
            ) {
                storeFile = file(keystore)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            } else {
                println("WARNING: Release signingConfig not fully configured.")
            }
        }
    }

    defaultConfig {
        applicationId = "org.openinsectid.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = "1.2.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("unminifiedRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
        }

        create("debuggableRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    tasks.register("printVersionName") {
        doLast {
            val versionName = android.defaultConfig.versionName
            println("VERSION_NAME=$versionName")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose.v300alpha04)
    implementation(libs.coil.network.okhttp)
    implementation(libs.gson)

    implementation(libs.androidx.activity.ktx)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ONNX Runtime
    implementation(libs.onnxruntime.android)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.json)

}
