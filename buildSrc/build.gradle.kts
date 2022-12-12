plugins {
    `kotlin-dsl`
}

repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    //mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("de.fabmax:webidl-util:0.8.0-SNAPSHOT")
}
