import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
}

val sourceJar: Task by tasks.getting
val javadocJar: Task by tasks.getting

publishing {
    repositories {
        maven {
            name = "KotDis"

            url = uri("https://schlaubi.jfrog.io/artifactory/mikbot")

            credentials {
                username = project.findProperty("kotdis.user") as String? ?: System.getenv("KOTLIN_DISCORD_USER")
                password = project.findProperty("kotdis.password") as String?
                    ?: System.getenv("KOTLIN_DISCORD_PASSWORD")
            }

            version = project.version
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))

            artifact(sourceJar)
            artifact(javadocJar)
        }
    }
}
