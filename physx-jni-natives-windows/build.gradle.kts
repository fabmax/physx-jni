java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    exclude("**/*.dll")
    exclude("**/*.sha1")
}

tasks.register<Exec>("generateNativeProjectWindows") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("$rootDir/PhysX/physx/generate_projects.bat", "jni-vc17win64")

    doFirst {
        delete("$rootDir/PhysX/physx/compiler/jni-vc16win64")
    }
}

// builds the windows platform native libraries
tasks.register<Exec>("buildNativeProjectWindows") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("cmake", "--build", "./compiler/jni-vc17win64/", "--config", NativeBuildSettings.buildType)

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-vc17win64")
    if (!nativeProjectDir.exists()) {
        dependsOn(":generateNativeProject")
    }

    val resourcesDir = "${rootDir}/physx-jni-natives-windows/src/main/resources/de/fabmax/physxjni/windows/"
    val resourcesCudaDir = "${rootDir}/physx-jni-natives-windows-cuda/src/main/resources/de/fabmax/physxjni/windows/"

    doFirst {
        delete(resourcesDir)
        delete(resourcesCudaDir)
    }

    doLast {
        // copy non-cuda libs to regular windows natives subproject
        copy {
            from("$rootDir/PhysX/physx/bin/jni-windows.x86_64/${NativeBuildSettings.buildType}")
            include("*.dll")
            exclude("freeglut.dll", "PhysXDevice64.dll", "PhysXGpu_64.dll")
            into(resourcesDir)
        }
        Sha1Helper.writeHashes(File(resourcesDir))

        // copy cuda libs to cuda windows natives sub-project
        copy {
            from("$rootDir/PhysX/physx/bin/jni-windows.x86_64/${NativeBuildSettings.buildType}")
            include("*.dll")
            exclude("freeglut.dll")
            into(resourcesCudaDir)
        }
        Sha1Helper.writeHashes(File(resourcesCudaDir))
    }
}

dependencies {
    implementation(project(":physx-jni"))
}
