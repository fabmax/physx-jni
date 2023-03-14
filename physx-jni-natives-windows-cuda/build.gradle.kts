java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    exclude("**/*.dll")
    exclude("**/*.sha1")
}

dependencies {
    implementation(project(":physx-jni"))
}
