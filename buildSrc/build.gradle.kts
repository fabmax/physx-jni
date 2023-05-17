plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    mavenCentral()
}

dependencies {
    implementation("de.fabmax:webidl-util:0.8.2")
}
