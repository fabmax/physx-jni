import org.gradle.internal.os.OperatingSystem

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "de.fabmax"
    version = "0.4.14-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// generates the cmake project for building windows platform native libraries (requires python3)
tasks.register<Exec>("generateNativeProject") {
    group = "native build"

    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("cmd", "/c", "generate_physx_win64.bat")
            it.isLinux -> listOf("./generate_physx_linux.sh")
            else -> listOf("./generate_physx_mac.sh")
        }
    }
}

// generates native glue code based on PhysXJs.idl
tasks.register<GenerateNativeGlueCode>("generateNativeGlueCode") {
    idlSource = File("${projectDir}/physx-jni/src/main/webidl/PhysXJs.idl").absolutePath
    generatorOutput = File("${projectDir}/PhysX/physx/source/physxjnibindings/src/").absolutePath
}

// builds the windows platform native libraries (requires cmake, Visual Studio 2019 (community) and a
// JDK with JNI headers)
tasks.register<Exec>("buildNativeProject") {
    group = "native build"
    dependsOn("generateNativeGlueCode")

    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("cmd", "/c", "build_physx_win64.bat")
            it.isLinux -> listOf("./build_physx_linux.sh")
            else -> listOf("./build_physx_mac.sh")
        }
    }
}
