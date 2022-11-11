plugins {
    `kotlin-dsl`
}

repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    mavenCentral()
}

dependencies {
    implementation("de.fabmax:webidl-util:0.7.8-SNAPSHOT")
}
