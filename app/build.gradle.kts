import java.util.Properties

val versionFile = rootProject.file("version.properties")
val versionProperties = Properties().apply {
    if (versionFile.exists()) {
        versionFile.inputStream().use { load(it) }
    }
}
val configuredVersionName = versionProperties.getProperty("VERSION_NAME", "0.0.1")
val configuredVersionParts = configuredVersionName.split(".").map { it.toIntOrNull() ?: 0 }
val configuredVersionCode = configuredVersionParts.getOrElse(0) { 0 } * 10000 +
    configuredVersionParts.getOrElse(1) { 0 } * 100 +
    configuredVersionParts.getOrElse(2) { 0 }
val requestedVersionBump = providers.gradleProperty("versionBump").orNull ?: "patch"

fun nextVersion(current: String, bump: String): String {
    val parts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return if (bump.equals("minor", ignoreCase = true) || bump.equals("feature", ignoreCase = true)) {
        "$major.${minor + 1}.0"
    } else {
        "$major.$minor.${patch + 1}"
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kzeng.myghnb"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "com.kzeng.myghnb"
        minSdk = 26
        targetSdk = 35
        versionCode = configuredVersionCode
        versionName = configuredVersionName
    }

    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

tasks.configureEach {
    if (name == "assembleDebug") {
        doLast {
            val outputDirectory = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
            val sourceApk = outputDirectory.resolve("app-debug.apk")
            val namedApk = outputDirectory.resolve("myGhNb-$configuredVersionName.apk")
            sourceApk.copyTo(namedApk, overwrite = true)

            val nextVersionName = nextVersion(configuredVersionName, requestedVersionBump)
            versionFile.writeText("VERSION_NAME=$nextVersionName\n")
            logger.lifecycle("Built myGhNb-$configuredVersionName.apk; next version is $nextVersionName")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
