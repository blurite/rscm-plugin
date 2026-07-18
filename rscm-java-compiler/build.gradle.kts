plugins {
    `java-library`
    `maven-publish`
}

group = "io.blurite"
version = "2026.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":rscm-core"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "test"
            url = rootProject.layout.buildDirectory.dir("test-maven-repository").get().asFile.toURI()
        }
    }
}
