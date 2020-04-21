plugins {
    id("com.android.application")
    kotlin("android")
}

buildscript {
    repositories {
        google()
        jcenter()
    }
}

android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.2"

    defaultConfig {
        applicationId = "com.waicool20.cvauto.android.server"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.android.support", "appcompat-v7", "28.0.0")
    implementation("commons-cli", "commons-cli", "1.4")
    testImplementation("junit", "junit", "4.12")
    androidTestImplementation("com.android.support.test", "runner", "1.0.2")
    androidTestImplementation("com.android.support.test.espresso", "espresso-core", "3.0.2")
}
