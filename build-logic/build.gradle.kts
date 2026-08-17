import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.kotlin.dsl.repositories

plugins {
    `kotlin-dsl`
}

group = "com.git.versioniong"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("gitVersioning"){
            id = "com.git.versioning"
            implementationClass = "GitVersioningPlugin"
        }
    }
}