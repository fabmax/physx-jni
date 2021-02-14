import org.gradle.internal.os.OperatingSystem

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "de.fabmax"
    version = "0.4.0"

    repositories {
        jcenter()
        mavenCentral()
    }
}

// generates the cmake project for building windows platform native libraries (requires python3)
tasks.register<Exec>("generateNativeProject") {
    group = "native build"

    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("cmd", "/c", "generate_physx_win64.bat")
            else -> listOf("./generate_physx_linux.sh")
        }
    }
}

// generates native glue code based on PhysXJs.idl
tasks.register<GenerateNativeGlueCode>("generateNativeGlueCode")

// builds the windows platform native libraries (requires cmake, Visual Studio 2019 (community) and a
// JDK with JNI headers)
tasks.register<Exec>("buildNativeProject") {
    group = "native build"
    dependsOn("generateNativeGlueCode")

    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("cmd", "/c", "build_physx_win64.bat")
            else -> listOf("./build_physx_linux.sh")
        }
    }
}
