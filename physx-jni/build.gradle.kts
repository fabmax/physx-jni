import java.io.File
import java.io.FileInputStream
import java.util.Properties

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    @Suppress("UnstableApiUsage")
    withSourcesJar()

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

publishing {
    if (File("publishingCredentials.properties").exists()) {
        val props = Properties()
        props.load(FileInputStream("publishingCredentials.properties"))

        repositories {
            maven {
                url = uri("${props.getProperty("publishRepoUrl")}/physx-jni")
                credentials {
                    username = props.getProperty("publishUser")
                    password = props.getProperty("publishPassword")
                }
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(project(":physx-jni-native-win64").tasks["jar"]).apply {
                classifier = "native-win64"
            }

            artifact(project(":physx-jni-native-linux64").tasks["jar"]).apply {
                classifier = "native-linux64"
            }

            pom {
                name.set("physx-jni")
                description.set("JNI bindings for Nvidia PhysX.")
                url.set("https://github.com/fabmax/physx-jni")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/fabmax/physx-jni.git")
                    developerConnection.set("scm:git:https://github.com/fabmax/physx-jni.git")
                    url.set("https://github.com/fabmax/physx-jni")
                }
            }
        }
    }
}
