import kotlin.math.min

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    exclude("**/*.so")
    exclude("**/*.sha1")
}

tasks.register<Exec>("generateNativeProjectLinux") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("$rootDir/PhysX/physx/generate_projects.sh", "jni-linux")

    doFirst {
        delete("$rootDir/PhysX/physx/compiler/jni-linux-checked")
        delete("$rootDir/PhysX/physx/compiler/jni-linux-debug")
        delete("$rootDir/PhysX/physx/compiler/jni-linux-profile")
        delete("$rootDir/PhysX/physx/compiler/jni-linux-release")
    }
}

// builds the linux platform native libraries
tasks.register<Exec>("buildNativeProjectLinux") {
    val makeWorkers = min(32, Runtime.getRuntime().availableProcessors())

    group = "native build"
    workingDir = File("$rootDir/PhysX/physx/compiler/jni-linux-${NativeBuildSettings.buildType}/")
    commandLine = listOf("make", "-j${makeWorkers}")

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-linux-${NativeBuildSettings.buildType}")
    if (!nativeProjectDir.exists()) {
        dependsOn(":generateNativeProject")
    }

    val resourcesDir = "${rootDir}/physx-jni-natives-linux/src/main/resources/de/fabmax/physxjni/linux/"
    val resourcesCudaDir = "${rootDir}/physx-jni-natives-linux-cuda/src/main/resources/de/fabmax/physxjni/linux/"

    doFirst {
        delete(resourcesDir)
        delete(resourcesCudaDir)
    }

    doLast {
        // copy non-cuda libs to regular linux natives subproject
        copy {
            from("$rootDir/PhysX/physx/bin/jni-linux.x86_64/${NativeBuildSettings.buildType}")
            include("*.so")
            exclude("libPhysXGpu_64.so")
            into(resourcesDir)
        }

        // copy cuda libs to cuda windows linux sub-project
        copy {
            from("$rootDir/PhysX/physx/bin/jni-linux.x86_64/${NativeBuildSettings.buildType}")
            include("*.so")
            into(resourcesCudaDir)
        }
        Sha1Helper.writeHashes(File(resourcesDir))
        Sha1Helper.writeHashes(File(resourcesCudaDir))
    }
}

dependencies {
    implementation(project(":physx-jni"))
}
