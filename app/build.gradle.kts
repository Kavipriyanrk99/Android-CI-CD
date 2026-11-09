import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProps = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")

    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

val ciBuildNumber = System.getenv("CI_BUILD_NUMBER")?.toInt() ?: 1

android {
    namespace = "com.example.cicd"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.cicd"
        minSdk = 24
        targetSdk = 36
        versionCode = ciBuildNumber
        versionName = "$ciBuildNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = (System.getenv("KEYSTORE_PATH") ?: keystoreProps["KEYSTORE_PATH"])?.let {
                file(it)
            }
            storePassword = (System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProps["KEYSTORE_PASSWORD"])?.toString()
            keyAlias = (System.getenv("KEY_ALIAS")
                ?: keystoreProps["KEY_ALIAS"])?.toString()
            keyPassword = (System.getenv("KEY_PASSWORD")
                ?: keystoreProps["KEY_PASSWORD"])?.toString()
        }
    }

    // build types
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }

        // AGP < 9.3
        release {
            isDebuggable = false

            // minification, shrinking and obfuscation
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
                .takeIf {
                    it.storeFile != null
                            && it.storePassword != null
                            && it.keyAlias != null
                            && it.keyPassword != null
                }
        }

        // AGP >= 9.3
//        release {
//            isDebuggable = false
//
//            minification, shrinking and obfuscation
//            optimization {
//                enable = true
//            }
//        }
    }

    // product flavors
    productFlavors {
        flavorDimensions += listOf("country", "environments")

        create("dev") {
            dimension = "environments"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            buildConfigField(
                "String", "API_BASE_URL", "\"https://dev-api.example.com\""
            )
        }

        create("prod") {
            dimension = "environments"

            buildConfigField(
                "String", "API_BASE_URL", "\"https://api.example.com\""
            )
        }

        create("india") {
            dimension = "country"
//            applicationIdSuffix = ".in"

            // Firebase App Distribution strictly relies on the internal Android Application ID (package name), Version Code,
            // and Version Name to identify and organize releases.
            versionNameSuffix = "-in"

            buildConfigField(
                "String", "API_BASE_URL", "\"https://api.example.in\""
            )
        }

        create("usa") {
            dimension = "country"
//            applicationIdSuffix = ".us"

            // Firebase App Distribution strictly relies on the internal Android Application ID (package name), Version Code,
            // and Version Name to identify and organize releases.
            versionNameSuffix = "-us"

            buildConfigField(
                "String", "API_BASE_URL", "\"https://api.example.us\""
            )
        }

        // NOTE:
        // Flavor dimension priority follows the order in flavorDimensions.
        // The first dimension has the highest priority.
        //
        // Here, "country" takes precedence over "environments".
        // Since both dimensions define API_BASE_URL, the country flavor wins.
        //
        // Result:
        // indiaDev  -> https://api.example.in
        // usaDev    -> https://api.example.us
        // indiaProd -> https://api.example.in
        // usaProd   -> https://api.example.us
        //
        // Environment-specific API_BASE_URL values are overridden
        // by country-specific API_BASE_URL values.
    }
}

// build variant filter
androidComponents {
    beforeVariants { variantBuilder ->
        // exclude prod flavor with debug build
        val excludeVariant = (variantBuilder.productFlavors.contains("environments" to "prod")
                && variantBuilder.buildType == "debug")
                || (variantBuilder.productFlavors.contains("environments" to "dev")
                && variantBuilder.buildType == "release")

        if (excludeVariant)
            variantBuilder.enable = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}