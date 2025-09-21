plugins {
    alias(libs.plugins.webidl)
    alias(libs.plugins.mavenPublish)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()

    sourceSets {
        main {
            java {
                srcDir("src/main/generated")
            }
        }
    }
}

webidl {
    modelPath.set(file("${projectDir}/src/main/webidl/"))
    modelName.set("PhysXJni")

    generateJni {
        javaClassesOutputDirectory.set(file("$projectDir/src/main/generated/physx"))
        nativeGlueCodeOutputFile.set(file("${rootDir}/PhysX/physx/source/webidlbindings/src/jni/PhysXJniGlue.h"))

        packagePrefix.set("physx")
        onClassLoadStatement.set("de.fabmax.physxjni.Loader.load();")
        nativeIncludeDir.set(file("$rootDir/PhysX/physx/include"))
    }

    generateCompactWebIdl {
        outputFile.set(file("${rootDir}/PhysX/physx/source/webidlbindings/src/wasm/PhysXWasm.idl"))
    }
}

tasks.javadoc {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("Xdoclint:none", "-quiet")
}

// make sure version string constants in main project and platform projects match the gradle project version
tasks.register<VersionNameUpdate>("updateVersionNames") {
    versionName = "$version"
    filesToUpdate = listOf(
        "${rootDir}/physx-jni/src/main/java/de/fabmax/physxjni/Loader.java",
        "${rootDir}/physx-jni-natives-windows/src/main/java/de/fabmax/physxjni/windows/NativeLibWindows.java",
        "${rootDir}/physx-jni-natives-windows-cuda/src/main/java/de/fabmax/physxjni/windows/NativeLibWindows.java",
        "${rootDir}/physx-jni-natives-linux/src/main/java/de/fabmax/physxjni/linux/NativeLibLinux.java",
        "${rootDir}/physx-jni-natives-linux-cuda/src/main/java/de/fabmax/physxjni/linux/NativeLibLinux.java",
        "${rootDir}/physx-jni-natives-macos/src/main/java/de/fabmax/physxjni/macos/NativeLibMacos.java",
        "${rootDir}/physx-jni-natives-macos-arm64/src/main/java/de/fabmax/physxjni/macosarm/NativeLibMacosArm64.java"
    )
}

tasks["clean"].doLast {
    delete("$projectDir/src/main/generated")
}

tasks.compileJava {
    dependsOn("generateJniJavaBindings")
    dependsOn("updateVersionNames")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    //testRuntimeOnly(project(":physx-jni-natives-windows-cuda"))
    //testRuntimeOnly(project(":physx-jni-natives-linux-cuda"))
    testRuntimeOnly(project(":physx-jni-natives-windows"))
    testRuntimeOnly(project(":physx-jni-natives-linux"))
    testRuntimeOnly(project(":physx-jni-natives-macos"))
    testRuntimeOnly(project(":physx-jni-natives-macos-arm64"))

    testImplementation(libs.lwjgl.core)

    val os = org.gradle.internal.os.OperatingSystem.current()
    val arch = System.getProperty("os.arch", "unknown")
    val lwjglPlatform = when {
        os.isLinux -> "natives-linux"
        os.isMacOsX && arch == "aarch64" -> "natives-macos-arm64"
        os.isMacOsX && arch != "aarch64" -> "natives-macos"
        else -> "natives-windows"
    }
    testRuntimeOnly("${libs.lwjgl.core.get()}:$lwjglPlatform")
}

mavenPublishing {
    publishToMavenCentral()
    if (!version.toString().endsWith("-SNAPSHOT")) {
        signAllPublications()
    }

    coordinates(group.toString(), name, version.toString())

    pom {
        name.set("physx-jni")
        description.set("JNI bindings for Nvidia PhysX.")
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

afterEvaluate {
    tasks["generateMetadataFileForMavenPublication"].dependsOn("plainJavadocJar")

    publishing.publications["maven"].apply {
        this as MavenPublication

        artifact(project(":physx-jni-natives-windows").tasks["jar"]).apply {
            classifier = "natives-windows"
        }
        artifact(project(":physx-jni-natives-linux").tasks["jar"]).apply {
            classifier = "natives-linux"
        }
        artifact(project(":physx-jni-natives-macos").tasks["jar"]).apply {
            classifier = "natives-macos"
        }
        artifact(project(":physx-jni-natives-macos-arm64").tasks["jar"]).apply {
            classifier = "natives-macos-arm64"
        }
    }
}
