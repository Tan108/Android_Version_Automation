import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

class GitVersioningPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val extension = project.extensions.create(
            "gitVersion",
            GitVersionExtension::class.java
        )

        val versionProperties = loadVersionProperties(project)

        val major = versionProperties["MAJOR"]
            ?.toString()
            ?.toIntOrNull()
            ?: 1

        val minor = versionProperties["MINOR"]
            ?.toString()
            ?.toIntOrNull()
            ?: 0

        val patch = versionProperties["PATCH"]
            ?.toString()
            ?.toIntOrNull()
            ?: 0

        val fallbackVersionName = "$major.$minor.$patch"
        val fallbackVersionCode = major * 10000 + minor * 100 + patch

        val latestTag = git(
            project,
            "describe",
            "--tags",
            "--abbrev=0"
        )

        val sha = git(
            project,
            "rev-parse",
            "--short",
            "HEAD"
        ) ?: "nogit"

        val commitsSinceTag = latestTag?.let { tag ->
            git(
                project,
                "rev-list",
                "$tag..HEAD",
                "--count"
            )?.toIntOrNull() ?: 0
        } ?: 0

        val baseTagVersion = latestTag?.removePrefix("v")

        val baseVersionName = baseTagVersion ?: fallbackVersionName

        val baseVersionCode =
            baseTagVersion
                ?.let(::semVerToVersionCode)
                ?: fallbackVersionCode

        val versionNameFromGit = when {
            latestTag == null -> "$fallbackVersionName+local.$sha"
            commitsSinceTag == 0 -> baseVersionName
            else -> "$baseVersionName+$commitsSinceTag.$sha"
        }

        val versionCodeFromGit = baseVersionCode + commitsSinceTag


        extension.versionName = versionNameFromGit
        extension.versionCode = versionCodeFromGit
        extension.releaseVersionName = baseVersionName

//        configureAndroid(
//            project = project,
//            calculatedVersionName = versionNameFromGit,
//            calculatedVersionCode = versionCodeFromGit,
//            releaseVersionName = baseVersionName
//        )
    }

    private fun loadVersionProperties(project: Project): Properties {
        return Properties().apply {
            val file = project.rootProject.file("version.properties")

            if (file.exists()) {
                file.inputStream().use {
                    load(it)
                }
            }
        }
    }

    private fun git(
        project: Project,
        vararg args: String
    ): String? {
        return try {
            project.providers.exec {
                commandLine("git", *args)
                workingDir(project.rootProject.projectDir)
            }
                .standardOutput
                .asText
                .get()
                .trim()
                .takeIf { it.isNotBlank() }

        } catch (_: Exception) {
            null
        }
    }

    private fun semVerToVersionCode(
        version: String
    ): Int? {

        val match = Regex(
            """(\d+)\.(\d+)\.(\d+)"""
        ).matchEntire(version)
            ?: return null

        val (major, minor, patch) =
            match.destructured

        return major.toInt() * 10000 +
                minor.toInt() * 100 +
                patch.toInt()
    }
}

open class GitVersionExtension {
    var versionName: String = ""
    var versionCode: Int = 1
    var releaseVersionName: String = ""
}