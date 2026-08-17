import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.git.versiong)
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

    buildTypes {
        debug {
            applicationIdSuffix=".debug"
            versionNameSuffix="-debug"
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }
    }

    productFlavors {
        flavorDimensions += "env"

        create("qa"){
            applicationIdSuffix=".qa"
            versionNameSuffix="-qa"
        }

        create("staging"){
            applicationIdSuffix=".staging"
            versionNameSuffix="-staging"
        }

        create("prod"){
            versionNameSuffix="-prod"
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

project.extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants { variant ->
        if (variant.buildType == "release") {
            variant.outputs.forEach { output ->
                output.versionName.set(gitVersion.releaseVersionName)
            }
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}