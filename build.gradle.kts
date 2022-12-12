import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import kotlin.math.min

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "de.fabmax"
    version = "2.0.3-SNAPSHOT"

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

    workingDir = File("$projectDir/PhysX/physx")
    commandLine = OperatingSystem.current().let {
        when {
            it.isWindows -> listOf("$projectDir/PhysX/physx/generate_projects.bat", "jni-vc17win64")
            it.isLinux -> listOf("$projectDir/PhysX/physx/generate_projects.sh", "jni-linux")
            else -> throw IllegalStateException("Unsupported OS: $it; for now, only Windows and Linux are supported")
        }
    }

    doFirst {
        if (!File("$projectDir/PhysX/physx").exists()) {
            throw IllegalStateException("Native PhysX project dir does not exist. Run 'git submodule update --init' first")
        }

        OperatingSystem.current().let {
            when {
                it.isWindows -> {
                    delete("$projectDir/PhysX/physx/compiler/jni-vc16win64")
                }
                it.isLinux -> {
                    delete("$projectDir/PhysX/physx/compiler/jni-linux-checked")
                    delete("$projectDir/PhysX/physx/compiler/jni-linux-debug")
                    delete("$projectDir/PhysX/physx/compiler/jni-linux-profile")
                    delete("$projectDir/PhysX/physx/compiler/jni-linux-release")
                }
                else -> throw IllegalStateException("Unsupported OS: $it; for now, only Windows and Linux are supported")
            }
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
    when {
        os.isWindows -> {
            workingDir = File("$projectDir/PhysX/physx")
            commandLine = listOf("cmake", "--build", "./compiler/jni-vc17win64/", "--config", "release")
        }
        os.isLinux -> {
            val makeWorkers = min(32, Runtime.getRuntime().availableProcessors())
            workingDir = File("$projectDir/PhysX/physx/compiler/jni-linux-release/")
            commandLine = listOf("make", "-j${makeWorkers}")
        }
        else -> throw IllegalStateException("Unsupported OS: $os; for now, only Windows and Linux are supported")
    }

    val nativeProjectDir = when {
        os.isWindows -> File("$projectDir/PhysX/physx/compiler/jni-vc17win64")
        os.isLinux -> File("$projectDir/PhysX/physx/compiler/jni-linux-release")
        else -> throw IllegalStateException("Unsupported OS: $os; for now, only Windows and Linux are supported")
    }
    if (!nativeProjectDir.exists()) {
        dependsOn("generateNativeProject")
    }

    doFirst {
        when {
            os.isWindows -> {
                delete("${projectDir}/physx-jni-natives-windows/src/main/resources/windows/")
                delete("${projectDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/")
            }
            os.isLinux -> {
                delete("${projectDir}/physx-jni-natives-linux/src/main/resources/windows/")
                delete("${projectDir}/physx-jni-natives-linux-cuda/src/main/resources/windows/")
            }
        }
    }

    doLast {
        // copy native libs from build output dir to project resources
        when {
            os.isWindows -> {
                // copy non-cuda libs to regular windows natives subproject
                copy {
                    from("$projectDir/PhysX/physx/bin/jni-windows.x86_64/release")
                    include("*.dll")
                    exclude("freeglut.dll", "PhysXDevice64.dll", "PhysXGpu_64.dll")
                    into("${projectDir}/physx-jni-natives-windows/src/main/resources/windows/")
                }
                // copy cuda libs to cuda windows natives sub-project
                copy {
                    from("$projectDir/PhysX/physx/bin/jni-windows.x86_64/release")
                    include("*.dll")
                    exclude("freeglut.dll")
                    into("${projectDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/")
                }
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-windows/src/main/resources/windows/"))
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-windows-cuda/src/main/resources/windows/"))
            }

            os.isLinux -> {
                // copy non-cuda libs to regular linux natives subproject
                copy {
                    from("$projectDir/PhysX/physx/bin/jni-linux.x86_64/release")
                    include("*.so")
                    exclude("libPhysXGpu_64.so")
                    into("${projectDir}/physx-jni-natives-linux/src/main/resources/linux/")
                }
                // copy cuda libs to cuda windows linux sub-project
                copy {
                    from("$projectDir/PhysX/physx/bin/jni-linux.x86_64/release")
                    include("*.so")
                    into("${projectDir}/physx-jni-natives-linux-cuda/src/main/resources/linux/")
                }
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-linux/src/main/resources/linux/"))
                Sha1Helper.writeHashes(File("${projectDir}/physx-jni-natives-linux-cuda/src/main/resources/linux/"))
            }
        }
    }
}
