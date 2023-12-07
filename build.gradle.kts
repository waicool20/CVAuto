plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.dokka") version "1.9.10"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")

    group = "com.waicool20.cvauto"
    version = "1.0-SNAPSHOT"

    kotlin {
        jvmToolchain(11)
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        val versions = object {
            val KotlinCoroutines = "1.7.3"
        }

        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        implementation("org.slf4j:slf4j-api:2.0.9")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.KotlinCoroutines}")

        // Tests
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    }
}

task<Zip>("docs") {
    dependsOn("dokkaHtmlCollector")
    destinationDirectory.set(file("$${layout.buildDirectory.asFile.get()}/dokka"))
    from(file("${layout.buildDirectory.asFile.get()}/dokka/htmlCollector"))
    into("docs")
    archiveFileName.set("docs.zip")
}
