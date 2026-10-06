import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Machine-specific values such as the App Distribution key path. Never committed.
val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}

android {
    namespace = "com.droidnova.fliptomute"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.droidnova.fliptomute"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "2.0.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            // Tester builds, as in Secret Calculator: ./gradlew assembleDebug appDistributionUploadDebug
            // The service account key stays outside the repository; its path is read from local.properties.
            firebaseAppDistribution {
                localProperties.getProperty("firebaseAppDistribution.serviceCredentialsFile")
                    ?.let { serviceCredentialsFile = it }
                groups = localProperties.getProperty("firebaseAppDistribution.groups", "me-flip-to-mute")
                artifactType = "APK"
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true

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
    testOptions {
        // Robolectric tests read the app's own resources, as in Secret Calculator
        unitTests.isIncludeAndroidResources = true
        // Lets Robolectric capture Compose screenshots (ComponentSnapshots)
        unitTests.all { it.systemProperty("robolectric.pixelCopyRenderMode", "hardware") }
    }
    buildFeatures {
        buildConfig = false
        compose = true
    }
    packaging {
        jniLibs {
            // Keep native libraries uncompressed so AGP can page-align them for
            // devices that use 16 KB memory pages.
            useLegacyPackaging = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    // Keep transitive SDKs from selecting the obsolete Fragment 1.1.x runtime.
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    // Health check (M1-06). Declared explicitly; it used to arrive only through the ads SDK, at 2.7.0.
    implementation(libs.androidx.work.runtime.ktx)
    // Dependency injection, as in Secret Calculator (M2)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // AppCompatActivity shell, as in Secret Calculator (M2-07)
    implementation(libs.androidx.appcompat)
    // Play In-App Review, as in Secret Calculator (M6-07)
    implementation(libs.play.review.ktx)
    // The Lite SDK provides the same client API while loading the ads runtime
    // from Google Play services instead of packaging native runtime binaries.
    implementation(libs.google.mobile.ads.lite)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    // Compose UI tests and off-device screenshots under Robolectric
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

}
