import org.gradle.internal.os.OperatingSystem

allprojects {
    group = "de.fabmax.physx-jni"
    version = "0.3.0"

    repositories {
        jcenter()
        mavenCentral()
    }

    subprojects {
        apply(plugin = "java-library")
    }
}

// generates the cmake project for building windows platform native libraries (requires python3)
tasks.register<Exec>("generateNativeProject") {
    group = "nativeWin64"

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
    group = "nativeWin64"
    dependsOn("generateNativeGlueCode")

    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("cmd", "/c", "build_physx_win64.bat")
            else -> listOf("./build_physx_linux.sh")
        }
    }
}
