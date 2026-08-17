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