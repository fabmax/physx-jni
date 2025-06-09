plugins {
    id("com.android.library") version "8.1.2"
    alias(libs.plugins.webidl)
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

tasks.register<Exec>("generateNativeProjectAndroid") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("$rootDir/PhysX/physx/generate_projects.sh", "jni-android-arm64-v8a")

    doFirst {
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-checked")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-debug")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-profile")
        delete("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-release")
    }
}

tasks["preBuild"].dependsOn("generateJniJavaBindings")

// builds the linux platform native libraries
tasks.register<Exec>("buildNativeProjectAndroid") {
    group = "native build"
    workingDir = File("$rootDir/PhysX/physx")
    commandLine = listOf("cmake", "--build", "./compiler/jni-android-arm64-v8a-release/")

    val nativeProjectDir = File("$rootDir/PhysX/physx/compiler/jni-android-arm64-v8a-release")
    if (!nativeProjectDir.exists()) {
        dependsOn("generateNativeProjectAndroid")
    }
    dependsOn("generateJniNativeBindings")

    val nativeLibsDir = "${projectDir}/src/main/jniLibs/arm64-v8a/"

    doFirst {
        delete(nativeLibsDir)
    }

    doLast {
        copy {
            from("$rootDir/PhysX/physx/bin/jni-android.aarch64/${NativeBuildSettings.buildType}/libPhysXJniBindings_64.so")
            into(nativeLibsDir)
        }
    }
}

webidl {
    modelPath.set(file("${rootDir}/physx-jni/src/main/webidl/"))
    modelName.set("PhysXJni")

    generateJni {
        javaClassesOutputDirectory.set(file("$projectDir/src/main/generated/physx"))
        nativeGlueCodeOutputFile.set(file("${rootDir}/PhysX/physx/source/webidlbindings/src/jni/PhysXJniGlue.h"))

        nativePlatform.set("android")
        packagePrefix.set("physx")
        onClassLoadStatement.set("de.fabmax.physxjni.Loader.load();")
        nativeIncludeDir.set(file("$rootDir/PhysX/physx/include"))
    }
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
            url = if (version.toString().endsWith("-SNAPSHOT")) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            }
        }
    }

    val props = LocalProperties.get(project)
    if (!props.publishUnsigned) {
        signing {
            publications.forEach {
                val privateKey = props["GPG_PRIVATE_KEY"]
                val password = props["GPG_PASSWORD"]
                useInMemoryPgpKeys(privateKey, password)
                sign(it)
            }
        }
    }
}
