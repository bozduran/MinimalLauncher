import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.minimallauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.minimallauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Release signing is configured from keystore.properties, which is
        // gitignored. When it is absent (contributors, CI without secrets) the
        // release build still runs so R8/shrinking can be verified — it just
        // produces an unsigned artifact.
        create("release") {
            val keystoreProperties = rootProject.file("keystore.properties")
            if (keystoreProperties.exists()) {
                val properties = Properties().apply {
                    keystoreProperties.inputStream().use { load(it) }
                }
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // A launcher's cold start is its headline performance metric, so the
            // release build is minified and resource-shrunk rather than shipping the
            // Compose runtime and all 38 palettes untrimmed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    lint {
        // Fail on errors, and promote the checks this backlog is actively fixing so
        // they cannot regress. Dependency-version notices stay advisory: upgrading
        // AGP/Compose is a separate, deliberate change (PERF-6 and friends).
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        error += setOf(
            "HardcodedText",
            "UnusedResources",
            "ObsoleteSdkInt",
            "MissingTranslation",
            "MonochromeLauncherIcon",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.datastore.preferences.core)
}
