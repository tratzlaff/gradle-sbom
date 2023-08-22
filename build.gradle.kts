plugins {
    id("java-gradle-plugin")
}

group = "com.tratzlaff"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.spdx:java-spdx-library:1.1.7")

    // Jackson handles serialization and deserialization of JSON.
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("sbom") {
            id = "com.tratzlaff.gradle-sbom"
            implementationClass = "com.tratzlaff.gradle.sbom.SbomGeneratorPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}