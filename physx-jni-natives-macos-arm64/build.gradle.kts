java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    exclude("**/*.dylib")
    exclude("**/*.sha1")
}

tasks.register<Exec>("generateNativeProjectMacosArm64") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("$rootDir/PhysX/physx/generate_projects.sh", "jni-mac-aarch64")

    doFirst {
        delete("$rootDir/PhysX/physx/compiler/jni-mac-aarch64")
    }
}


// builds the windows platform native libraries
tasks.register<Exec>("buildNativeProjectMacosArm64") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("cmake", "--build", "./compiler/jni-mac-aarch64/", "--config", BuildSettings.buildType)

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-mac-aarch64")
    if (!nativeProjectDir.exists()) {
        dependsOn(":generateNativeProject")
    }

    val resourcesDir = "${rootDir}/physx-jni-natives-macos-arm64/src/main/resources/de/fabmax/physxjni/macosarm/"

    doFirst {
        delete(resourcesDir)
    }

    doLast {
        copy {
            from("$rootDir/PhysX/physx/bin/jni-mac.aarch64/${BuildSettings.buildType}")
            include("*.dylib")
            into(resourcesDir)
        }
        Sha1Helper.writeHashes(File(resourcesDir))
    }
}
dependencies {
    implementation(project(":physx-jni"))
}
