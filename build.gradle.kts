allprojects {
    group = "de.fabmax.physx-jni"
    version = "0.2.0"

    repositories {
        jcenter()
        mavenCentral()
    }

    subprojects {
        apply(plugin = "java-library")
    }
}

// make sure version string constants in main project and platform projects match the gradle project version
tasks.register<VersionNameUpdate>("updateVersionNames") {
    versionName = "$version"
    filesToUpdate = listOf(
        "physx-jni/src/main/java/de/fabmax/physxjni/Loader.java",
        "physx-jni-native-win64/src/main/java/de/fabmax/physxjni/NativeMetaWin64.java"
    )
}

// generates the cmake project for building windows platform native libraries (requires python3)
tasks.register<Exec>("generateNativeProject") {
    group = "nativeWin64"
    commandLine = listOf("cmd", "/c", "generate_physx_project.bat")
}

// generates native glue code based on PhysXJs.idl
tasks.register<GenerateNativeGlueCode>("generateNativeGlueCode")

// builds the windows platform native libraries (requires cmake, Visual Studio 2019 (community) and a
// JDK with JNI headers)
tasks.register<Exec>("buildNativeProject") {
    group = "nativeWin64"
    dependsOn("generateNativeGlueCode")
    commandLine = listOf("cmd", "/c", "build_physx_project.bat")
}
