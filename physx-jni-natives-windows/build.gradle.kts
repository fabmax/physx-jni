java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    exclude("**/windows")
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
    commandLine = listOf("cmake", "--build", "./compiler/jni-vc17win64/", "--config", BuildSettings.buildType)

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-vc17win64")
    if (!nativeProjectDir.exists()) {
        dependsOn(":generateNativeProject")
    }

    doFirst {
        delete("${rootDir}/physx-jni-natives-windows/src/main/resources/windows/")
        delete("${rootDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/")
    }

    doLast {
        // copy non-cuda libs to regular windows natives subproject
        copy {
            from("$rootDir/PhysX/physx/bin/jni-windows.x86_64/${BuildSettings.buildType}")
            include("*.dll")
            exclude("freeglut.dll", "PhysXDevice64.dll", "PhysXGpu_64.dll")
            into("${rootDir}/physx-jni-natives-windows/src/main/resources/windows/")
        }
        Sha1Helper.writeHashes(File("${rootDir}/physx-jni-natives-windows/src/main/resources/windows/"))

        // copy cuda libs to cuda windows natives sub-project
        copy {
            from("$rootDir/PhysX/physx/bin/jni-windows.x86_64/${BuildSettings.buildType}")
            include("*.dll")
            exclude("freeglut.dll")
            into("${rootDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/")
        }
        Sha1Helper.writeHashes(File("${rootDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/"))
    }
}

dependencies {
    implementation(project(":physx-jni"))
}
