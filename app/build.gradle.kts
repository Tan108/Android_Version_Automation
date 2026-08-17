import java.util.Properties
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 1. Define ValueSource interface for Git parameter passing
interface GitExecParameters : ValueSourceParameters {
    val gitArgs: Property<Array<String>>
    val workingDir: Property<File>
}

// 2. Define ValueSource class to execute Git safely for Configuration Cache
abstract class GitValueSource @Inject constructor(
    private val execOperations: ExecOperations
) : ValueSource<String, GitExecParameters> {

    override fun obtain(): String? {
        val args = parameters.gitArgs.orNull ?: return null
        val dir = parameters.workingDir.orNull ?: return null
        return try {
            val stdout = ByteArrayOutputStream()
            val result = execOperations.exec {
                commandLine(*args)
                workingDir(dir)
                standardOutput = stdout
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                stdout.toString().trim().takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

// 3. Helper function wrapping ValueSource provider execution
fun git(vararg args: String): String? {
    return providers.of(GitValueSource::class.java) {
        parameters.gitArgs.set(arrayOf(*args))
        parameters.workingDir.set(rootProject.projectDir)
    }.orNull
}

fun semVerToVersionCode(version: String): Int? {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(version) ?: return null
    val (major, minor, patch) = match.destructured
    return major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
}

val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val major = versionProps["MAJOR"]?.toString()?.toIntOrNull() ?: 1
val minor = versionProps["MINOR"]?.toString()?.toIntOrNull() ?: 0
val patch = versionProps["PATCH"]?.toString()?.toIntOrNull() ?: 0

val fallbackVersionName = "$major.$minor.$patch"
val fallbackVersionCode = major * 10000 + minor * 100 + patch

val latestTag = git("describe", "--tags", "--abbrev=0")
val sha = git("rev-parse", "--short", "HEAD") ?: "nogit"
val commitsSinceTag = latestTag?.let { tag ->
    git("rev-list", "$tag..HEAD", "--count")?.toIntOrNull() ?: 0
} ?: 0

val baseTagVersion = latestTag?.removePrefix("v")
val baseVersionName = baseTagVersion ?: fallbackVersionName
val baseVersionCode = baseTagVersion?.let(::semVerToVersionCode) ?: fallbackVersionCode

// Tagged releases stay clean. Builds ahead of a tag carry trace metadata.
val versionNameFromGit = when {
    latestTag == null -> "$fallbackVersionName+local.$sha"
    commitsSinceTag == 0 -> baseVersionName
    else -> "$baseVersionName+$commitsSinceTag.$sha"
}

val versionCodeFromGit = baseVersionCode + commitsSinceTag

android {
    namespace = "com.example.android_version_automation"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.android_version_automation"
        minSdk = 24
        targetSdk = 37
        versionCode = versionCodeFromGit
        versionName = versionNameFromGit

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            optimization {
                enable = false
            }
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