import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // PERF-3: creates the `baselineProfile` configuration the :baselineprofile
    // module feeds, and the generateBaselineProfile task.
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.example.minimallauncher"
    compileSdk = 35

    defaultConfig {
        // NOTE (PERF-6): this identifier is NOT publishable — `com.example.*` is
        // reserved by Google Play. Changing it is a product decision, not a
        // mechanical one: it makes the app a different app to the platform, so
        // existing installs are a fresh install and their DataStore settings are not
        // migrated. Pick an id on a domain you control, then decide whether the
        // migration is acceptable (see README "Releasing").
        //
        // `namespace` may stay as it is: it only names the generated R/BuildConfig
        // classes, so it does not have to follow the application id.
        applicationId = "com.example.minimallauncher"
        minSdk = 26
        targetSdk = 35

        // Version scheme: versionCode = major * 10_000 + minor * 100 + patch.
        // BuildVersionTest asserts these two stay consistent, so bumping one without
        // the other fails the test suite rather than shipping a bad update.
        versionCode = 10_000
        versionName = "1.0.0"

        // Required for any instrumented test to run at all; it was never declared,
        // so `connectedDebugAndroidTest` had nothing to execute.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Exposes VERSION_CODE / VERSION_NAME to the unit tests, which assert the
        // declared values match the documented scheme.
        buildConfig = true
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
    implementation(libs.androidx.lifecycle.runtime.compose)
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

    // QA-3: instrumented Compose UI tests. These need a device or emulator:
    //   ./gradlew connectedDebugAndroidTest
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.androidx.ui.test.manifest)

    // PERF-3: profileinstaller (which applies the generated profile at install time)
    // is added by the baselineprofile plugin and is already on the classpath
    // transitively via activity/ui, so it is not declared explicitly here.
    // The profile generator lives in :baselineprofile and runs on a device.
    "baselineProfile"(project(":baselineprofile"))
}
