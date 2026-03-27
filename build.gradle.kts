// Top-level build file for CloudStream plugin project

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.github.recloudstream:gradle:master-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.1.20")
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
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    // Configuration for CloudStream plugin
    configure<com.lagradost.cloudstream3.gradle.CloudstreamExtension> {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Nikeleon/cloudstream-plugins")
    }

    dependencies {
        val implementation by configurations
        implementation("com.github.recloudstream.cloudstream:library:master-SNAPSHOT")
        implementation(kotlin("stdlib"))
        
        implementation("com.github.Blatzar:NiceHttp:0.4.11")
        implementation("org.jsoup:jsoup:1.17.2")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    }

    // Use configure to be safe with types in KTS
    configure<com.android.build.gradle.LibraryExtension> {
        namespace = "com.quyen.${project.name.lowercase().replace("-", "")}"
        compileSdk = 34

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    // Kotlin JVM target and skip metadata version check
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xskip-metadata-version-check")
        }
    }
}



tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
