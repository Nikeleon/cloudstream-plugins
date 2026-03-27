// Top-level build file for CloudStream plugin project
import com.lagradost.cloudstream3.gradle.CloudstreamExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.github.recloudstream:gradle:master-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    configure<CloudstreamExtension> {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Nikeleon/cloudstream-plugins")
    }

    dependencies {
        val implementation by configurations
        implementation("com.github.recloudstream.cloudstream:library:master-SNAPSHOT")
        implementation(kotlin("stdlib"))
        implementation("org.jsoup:jsoup:1.17.2")
    }

    android {
        namespace = "com.quyen"
        compileSdk = 34

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

