import java.io.FileInputStream
import java.util.*

plugins {
    signing
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    @Suppress("UnstableApiUsage")
    withSourcesJar()
    @Suppress("UnstableApiUsage")
    withJavadocJar()

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
        "${rootDir}/physx-jni/src/main/java/de/fabmax/physxjni/Loader.java",
        "${rootDir}/physx-jni-native-win64/src/main/java/de/fabmax/physxjni/NativeMetaWin64.java",
        "${rootDir}/physx-jni-native-win64cuda/src/main/java/de/fabmax/physxjni/NativeMetaWin64.java",
        "${rootDir}/physx-jni-native-linux64/src/main/java/de/fabmax/physxjni/NativeMetaLinux64.java",
        "${rootDir}/physx-jni-native-linux64cuda/src/main/java/de/fabmax/physxjni/NativeMetaLinux64.java",
        "${rootDir}/physx-jni-native-mac64/src/main/java/de/fabmax/physxjni/NativeMetaMac64.java"
    )
}

tasks.register<GenerateJavaBindings>("generateJniBindings") {
    idlSource = File("${projectDir}/src/main/webidl/PhysXJs.idl").absolutePath
    generatorOutput = File("${projectDir}/src/main/generated/physx").absolutePath
}

tasks.register<CheckWebIdlConsistency>("checkWebIdlConsistency") {
    idlSource1 = File("${projectDir}/src/main/webidl/PhysXJs.idl").absolutePath
    idlSource2 = File("${rootDir}/Physx/physx/source/physxwebbindings/src/PhysXJs.idl").absolutePath
}

val compileJava by tasks.existing {
    dependsOn("checkWebIdlConsistency")
    dependsOn("generateJniBindings")
    dependsOn("updateVersionNames")
}

dependencies {
    testImplementation("junit:junit:4.12")
    testRuntimeOnly(project(":physx-jni-native-win64cuda"))
    testRuntimeOnly(project(":physx-jni-native-linux64cuda"))
    testRuntimeOnly(project(":physx-jni-native-mac64"))

    testImplementation("org.lwjgl:lwjgl:3.2.3")

    val lwjglNatives = org.gradle.internal.os.OperatingSystem.current().let {
        when {
            it.isLinux -> "natives-linux"
            it.isMacOsX -> "natives-macos"
            else -> "natives-windows"
        }
    }
    testRuntimeOnly("org.lwjgl:lwjgl:3.2.3:$lwjglNatives")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(project(":physx-jni-native-win64").tasks["jar"]).apply {
                classifier = "native-win64"
            }

            artifact(project(":physx-jni-native-win64cuda").tasks["jar"]).apply {
                classifier = "native-win64cuda"
            }

            artifact(project(":physx-jni-native-linux64").tasks["jar"]).apply {
                classifier = "native-linux64"
            }

            artifact(project(":physx-jni-native-linux64cuda").tasks["jar"]).apply {
                classifier = "native-linux64cuda"
            }

            artifact(project(":physx-jni-native-mac64").tasks["jar"]).apply {
                classifier = "native-mac64"
            }

            pom {
                name.set("physx-jni")
                description.set("JNI bindings for Nvidia PhysX.")
                url.set("https://github.com/fabmax/physx-jni")
                developers {
                    developer {
                        name.set("Max Thiele")
                        email.set("fabmax.thiele@gmail.com")
                        organization.set("github")
                        organizationUrl.set("https://github.com/fabmax")
                    }
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/fabmax/physx-jni.git")
                    developerConnection.set("scm:git:ssh://github.com:fabmax/physx-jni.git")
                    url.set("https://github.com/fabmax/physx-jni/tree/main")
                }
            }
        }
    }

    if (File("${rootDir}/publishingCredentials.properties").exists()) {
        val props = Properties()
        props.load(FileInputStream("${rootDir}/publishingCredentials.properties"))

        repositories {
            maven {
                url = if (version.toString().endsWith("-SNAPSHOT")) {
                    uri("https://oss.sonatype.org/content/repositories/snapshots")
                } else {
                    uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                }
                credentials {
                    username = props.getProperty("publishUser")
                    password = props.getProperty("publishPassword")
                }
            }
        }

        signing {
            sign(publications["mavenJava"])
        }
    }
}
