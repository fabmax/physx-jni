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

tasks.register<GenerateJavaBindings>("generateJniBindings")
val compileJava by tasks.existing {
    dependsOn("generateJniBindings")
}

dependencies {
    testImplementation("junit", "junit", "4.12")
    testRuntimeOnly(project(":physx-jni-native-win64"))
}
