plugins {
    id("java")
    id("com.tratzlaff.gradle-sbom") version "0.1"
}

group = "com.tratzlaff.sample"
version = "1.2.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")
   // implementation("com.amazonaws:aws-java-sdk:1.12.533")

    //testImplementation(platform("org.junit:junit-bom:5.9.1"))
    //testImplementation("org.junit.jupiter:junit-jupiter")
}