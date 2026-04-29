plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion = "1.13.5"

android {
    namespace = "com.trafficracer.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trafficracer.game"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "2.1"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
            jniLibs.srcDirs("libs")
        }
    }
}

// Configuration for native dependencies
val natives: Configuration by configurations.creating

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")

    // LibGDX core
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    // LibGDX Android backend
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    // Native libs (extracted by copyNativeLibs task)
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

// Extract native .so files from jars into libs/ directory
tasks.register("copyNativeLibs") {
    doFirst {
        val libsDir = file("libs")
        libsDir.deleteRecursively()
        natives.files.forEach { jar ->
            val abi = when {
                jar.name.contains("armeabi-v7a") -> "armeabi-v7a"
                jar.name.contains("arm64-v8a") -> "arm64-v8a"
                jar.name.contains("x86_64") -> "x86_64"
                jar.name.contains("x86") -> "x86"
                else -> return@forEach
            }
            val outDir = file("libs/$abi")
            outDir.mkdirs()
            zipTree(jar).matching { include("*.so") }.forEach { soFile ->
                soFile.copyTo(File(outDir, soFile.name), overwrite = true)
            }
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyNativeLibs")
}
