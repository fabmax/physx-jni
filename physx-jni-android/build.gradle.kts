plugins {
    id("com.android.library") version "8.11.2"
    alias(libs.plugins.webidl)
    alias(libs.plugins.mavenPublish)
}

android {
    namespace = "de.fabmax.physxjni"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    sourceSets.getByName("main") {
        java.srcDir("src/main/generated")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.register<Exec>("generateNativeProjectAndroid") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("$rootDir/PhysX/physx/generate_projects.sh", "jni-android-arm64-v8a")

    doFirst {
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-checked")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-debug")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-profile")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-release")
    }
}

tasks["preBuild"].dependsOn("generateJniJavaBindings")

// builds the linux platform native libraries
tasks.register<Exec>("buildNativeProjectAndroid") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("cmake", "--build", "./compiler/jni-android-arm64-v8a-release/")

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-release")
    if (!nativeProjectDir.exists()) {
        dependsOn("generateNativeProjectAndroid")
    }
    dependsOn("generateJniNativeBindings")

    val nativeLibsDir = "${projectDir}/src/main/jniLibs/arm64-v8a/"

    doFirst {
        delete(nativeLibsDir)
    }

    doLast {
        copy {
            from("$rootDir/PhysX/physx/bin/jni-android.aarch64/${NativeBuildSettings.buildType}/libPhysXJniBindings_64.so")
            into(nativeLibsDir)
        }
    }
}

webidl {
    modelPath = file("${rootDir}/physx-jni/src/main/webidl/")
    modelName = "PhysXJni"

    generateJni {
        javaClassesOutputDirectory = file("$projectDir/src/main/generated/physxandroid")
        nativeGlueCodeOutputFile = file("${rootDir}/PhysX/physx/source/webidlbindings/src/jni/PhysXJniGlue.h")

        nativePlatform = "android"
        packagePrefix = "physxandroid"
        onClassLoadStatement = "de.fabmax.physxandroid.Loader.load();"
        nativeIncludeDirs = files("$rootDir/PhysX/physx/include")
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (!version.toString().endsWith("-SNAPSHOT")) {
        signAllPublications()
    }

    coordinates(group.toString(), name, version.toString())

    pom {
        name.set("physx-jni-android")
        description.set("JNI bindings for Nvidia PhysX on Android.")
        inceptionYear.set("2020")
        url.set("https://github.com/fabmax/physx-jni")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("fabmax")
                name.set("Max Thiele")
                url.set("https://github.com/fabmax/")
            }
        }
        scm {
            url.set("https://github.com/fabmax/physx-jni/")
            connection.set("scm:git:git://github.com/fabmax/physx-jni.git")
            developerConnection.set("scm:git:ssh://git@github.com/fabmax/physx-jni.git")
        }
    }
}
