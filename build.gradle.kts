import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "de.fabmax"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks["jar"].apply {
        this as Jar
        from("$rootDir/LICENSE", "$rootDir/NOTICE.md")
    }
}

// generates the cmake project for building windows platform native libraries (requires python3)
tasks.register<Exec>("generateNativeProject") {
    group = "native build"

    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("cmd", "/c", "generate_physx_win64.bat")
            it.isLinux -> listOf("./generate_physx_linux.sh")
            else -> throw IllegalStateException("Unsupported OS: $it; for now, only Windows and Linux are supported")
        }
    }
}

// generates native glue code based on PhysXJs.idl
tasks.register<GenerateNativeGlueCode>("generateNativeGlueCode") {
    idlModelName = "PhysXJni"
    idlSource = File("${projectDir}/physx-jni/src/main/webidl/").absolutePath
    generatorOutput = File("${projectDir}/PhysX/physx/source/webidlbindings/src/jni/").absolutePath
}

// builds the windows platform native libraries (requires cmake, Visual Studio 2019 (community) and a
// JDK with JNI headers)
tasks.register<Exec>("buildNativeProject") {
    group = "native build"
    dependsOn("generateNativeGlueCode")

    val os = OperatingSystem.current()
    commandLine = when {
        os.isWindows -> listOf("cmd", "/c", "build_physx_windows.bat")
        os.isLinux -> listOf("./build_physx_linux.sh")
        else -> throw IllegalStateException("Unsupported OS: $os; for now, only Windows and Linux are supported")
    }

    doLast {
        // generate native library file hashes
        when {
            os.isWindows -> {
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-windows/src/main/resources/windows/"))
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/"))
            }
            os.isLinux -> {
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-linux/src/main/resources/linux/"))
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-linux-cuda/src/main/resources/linux/"))
            }
        }
    }
}
