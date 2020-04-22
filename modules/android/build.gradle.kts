import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
}

version = "1.0-SNAPSHOT"

dependencies {
    val versions = object {
        val Kotlin by lazy { plugins.getPlugin(KotlinPluginWrapper::class).kotlinPluginVersion }
        val KotlinCoroutines = "1.3.2"
    }

    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", versions.Kotlin)
    implementation("org.jetbrains.kotlin", "kotlin-reflect", versions.Kotlin)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", versions.KotlinCoroutines)

    api(project(":modules:core"))

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf(
            "-XXLanguage:+InlineClasses",
            "-Xuse-experimental=kotlinx.coroutines.FlowPreview",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.coroutines.ObsoleteCoroutinesApi"
        )
    }
}

tasks.named<Copy>("processResources") {
    dependsOn("server:build")
    from(projectDir.resolve("server/build/outputs/apk/release")) {
        include("**/*.apk")
        rename("server-(.*?).apk", "server.apk")
    }
}