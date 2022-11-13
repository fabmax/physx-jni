import java.io.FileInputStream
import java.util.*

plugins {
    signing
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()

    sourceSets {
        main {
            java {
                srcDir("src/main/generated")
            }
        }
    }
}

tasks.javadoc {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("Xdoclint:none", "-quiet")
}

// make sure version string constants in main project and platform projects match the gradle project version
tasks.register<VersionNameUpdate>("updateVersionNames") {
    versionName = "$version"
    filesToUpdate = listOf(
        "${rootDir}/physx-jni/src/main/java/de/fabmax/physxjni/Loader.java",
        "${rootDir}/physx-jni-natives-windows/src/main/java/de/fabmax/physxjni/NativeMetaWindows.java",
        "${rootDir}/physx-jni-natives-windows-cuda/src/main/java/de/fabmax/physxjni/NativeMetaWindows.java",
        "${rootDir}/physx-jni-natives-linux/src/main/java/de/fabmax/physxjni/NativeMetaLinux.java",
        "${rootDir}/physx-jni-natives-linux-cuda/src/main/java/de/fabmax/physxjni/NativeMetaLinux.java"
    )
}

tasks.register<GenerateJavaBindings>("generateJniBindings") {
    idlModelName = "PhysXJni"
    idlSource = File("${projectDir}/src/main/webidl/").absolutePath
    generatorOutput = File("${projectDir}/src/main/generated/physx").absolutePath
}

tasks.compileJava {
    dependsOn("generateJniBindings")
    dependsOn("updateVersionNames")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testRuntimeOnly(project(":physx-jni-natives-windows"))
    testRuntimeOnly(project(":physx-jni-natives-windows-cuda"))
    testRuntimeOnly(project(":physx-jni-natives-linux-cuda"))

    testImplementation("org.lwjgl:lwjgl:3.3.1")

    val lwjglNatives = org.gradle.internal.os.OperatingSystem.current().let {
        when {
            it.isLinux -> "natives-linux"
            it.isMacOsX -> "natives-macos"
            else -> "natives-windows"
        }
    }
    testRuntimeOnly("org.lwjgl:lwjgl:3.3.1:$lwjglNatives")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(project(":physx-jni-natives-windows").tasks["jar"]).apply {
                classifier = "natives-windows"
            }

//            artifact(project(":physx-jni-natives-windows-cuda").tasks["jar"]).apply {
//                classifier = "natives-windows-cuda"
//            }

            artifact(project(":physx-jni-natives-linux").tasks["jar"]).apply {
                classifier = "natives-linux"
            }

//            artifact(project(":physx-jni-natives-linux-cuda").tasks["jar"]).apply {
//                classifier = "natives-linux-cuda"
//            }

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
