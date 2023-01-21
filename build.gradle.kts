plugins {
    kotlin("jvm") version "1.8.0"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.waicool20"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        val versions = object {
            val KotlinCoroutines = "1.6.4"
        }

        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.KotlinCoroutines}")

        // Tests
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    }
}
