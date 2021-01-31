java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    sourceSets {
        main {
            java {
                srcDir("src/main/generated")
            }
        }
    }
}

// make sure version string constants in main project and platform projects match the gradle project version
tasks.register<VersionNameUpdate>("updateVersionNames") {
    versionName = "$version"
    filesToUpdate = listOf(
        "physx-jni/src/main/java/de/fabmax/physxjni/Loader.java",
        "physx-jni-native-win64/src/main/java/de/fabmax/physxjni/NativeMetaWin64.java",
        "physx-jni-native-linux64/src/main/java/de/fabmax/physxjni/NativeMetaLinux64.java"
    )
}

tasks.register<GenerateJavaBindings>("generateJniBindings")
val compileJava by tasks.existing {
    dependsOn("generateJniBindings")
    dependsOn("updateVersionNames")
}

dependencies {
    testImplementation("junit", "junit", "4.12")
    testRuntimeOnly(project(":physx-jni-native-win64"))
    testRuntimeOnly(project(":physx-jni-native-linux64"))
}
