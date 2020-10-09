import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
}

version = "1.0-SNAPSHOT"

dependencies {
    val versions = object {
        val Kotlin by lazy { plugins.getPlugin(KotlinPluginWrapper::class).kotlinPluginVersion }
        val KotlinCoroutines = "1.3.9"
    }

    implementation("org.jetbrains.kotlin", "kotlin-reflect", versions.Kotlin)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", versions.KotlinCoroutines)
    implementation("org.lz4:lz4-pure-java:1.7.0")

    api(project(":modules:cvauto-core"))

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