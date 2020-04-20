allprojects {
    group = "com.waicool20.cvauto"
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.6.3")
    }
}