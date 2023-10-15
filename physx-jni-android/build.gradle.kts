plugins {
    id("com.android.library") version "8.1.2"
    `maven-publish`
    signing
}

android {
    namespace = "de.fabmax.physxjni"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
    }

    sourceSets.getByName("main") {
        java.srcDir("src/main/generated")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// generates native glue code based on PhysXJs.idl
tasks.register<GenerateNativeGlueCode>("generateNativeGlueCodeAndroid") {
    idlModelName = "PhysXJni"
    platform = "android"
    idlSource = File("${rootDir}/physx-jni/src/main/webidl/").absolutePath
    generatorOutput = File("${rootDir}/PhysX/physx/source/webidlbindings/src/jni/").absolutePath
}

tasks.register<Exec>("generateNativeProjectAndroid") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("$rootDir/PhysX/physx/generate_projects.bat", "jni-android-arm64-v8a")

    doFirst {
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-checked")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-debug")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-profile")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-release")
    }
}

// builds the linux platform native libraries
tasks.register<Exec>("buildNativeProjectAndroid") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("cmake", "--build", "./compiler/jni-android-arm64-v8a-release/")

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-release")
    if (!nativeProjectDir.exists()) {
        dependsOn("generateNativeProjectAndroid")
    }
    dependsOn("generateNativeGlueCodeAndroid")
    dependsOn("generateJniBindings")

    val nativeLibsDir = "${projectDir}/src/main/jniLibs/arm64-v8a/"

    doFirst {
        delete(nativeLibsDir)
    }

    doLast {
        copy {
            from("$rootDir/PhysX/physx/bin/jni-android.aarch64/${BuildSettings.buildType}/libPhysXJniBindings_64.so")
            into(nativeLibsDir)
        }
    }
}

tasks.register<GenerateJavaBindings>("generateJniBindings") {
    group = "build"
    idlModelName = "PhysXJni"
    idlSource = File("${rootDir}/physx-jni/src/main/webidl/").absolutePath
    generatorOutput = File("${projectDir}/src/main/generated/physx").absolutePath
    physxIncludeDir = "$rootDir/PhysX/physx/include"
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "de.fabmax"
            artifactId = "physx-jni-android"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("physx-jni-android")
                description.set("JNI bindings for Nvidia PhysX on Android.")
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

    repositories {
        maven {
            name = "ossrh"
            url = if (version.toString().endsWith("-SNAPSHOT")) {
                uri("https://oss.sonatype.org/content/repositories/snapshots")
            } else {
                uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            }
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }

    signing {
        val privateKey = System.getenv("GPG_PRIVATE_KEY")
        val password = System.getenv("GPG_PASSWORD")
        useInMemoryPgpKeys(privateKey, password)

        sign(publications["release"])
    }
}
