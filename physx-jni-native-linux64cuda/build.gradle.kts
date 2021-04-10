java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    @Suppress("UnstableApiUsage")
    withSourcesJar()
}

dependencies {
    implementation(project(":physx-jni"))
}
