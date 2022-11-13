java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    tasks.processResources.get().exclude("*")
}

dependencies {
    implementation(project(":physx-jni"))
}
