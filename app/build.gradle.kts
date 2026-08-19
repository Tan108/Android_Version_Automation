plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.git.versiong)
    id("com.google.gms.google-services")
}

val gitVersion = extensions.getByType<GitVersionExtension>()

android {
    namespace = "com.example.android_version_automation"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.android_version_automation"
        minSdk = 24
        targetSdk = 37
        versionCode = gitVersion.versionCode
        versionName = gitVersion.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("RELEASE_KEYSTORE_FILE")

            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }

    productFlavors {
        flavorDimensions += "env"

        create("qa") {
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
        }

        create("staging") {
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }

        create("prod") {
            versionNameSuffix = "-prod"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants { variant ->

        val flavorName = variant.productFlavors
            .joinToString("-") { it.second }
            .ifEmpty { "default" }

        val apkName = "AndroidVersionAutomation-" +
                "${gitVersion.releaseVersionName}-" +
                "${gitVersion.versionCode}-" +
                "$flavorName-" +
                "${variant.buildType}.apk"

        variant.outputs.forEach { output ->

            if (variant.buildType == "release") {
                output.versionName.set(
                    gitVersion.releaseVersionName
                )
            }

            output.outputFileName.set(apkName)
        }
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}