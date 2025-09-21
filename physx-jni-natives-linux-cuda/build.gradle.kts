java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

tasks["sourcesJar"].apply {
    this as Jar
    exclude("**/*.so")
    exclude("**/*.sha1")
}

dependencies {
    implementation(project(":physx-jni"))
}
