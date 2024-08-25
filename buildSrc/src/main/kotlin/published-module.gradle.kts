import java.util.Base64

plugins {
	`maven-publish`
	signing
	com.google.cloud.artifactregistry.`gradle-plugin`
}

val sourceJar: Task by tasks.getting
val javadocJar: Task by tasks.getting
//val dokkaJar: Task by tasks.getting

afterEvaluate {
	publishing {
		repositories {
			maven("artifactregistry://europe-west3-maven.pkg.dev/mik-music/mikbot") {
				credentials {
					username = "_json_key_base64"
					password = System.getenv("GOOGLE_KEY")?.toByteArray()?.let {
						Base64.getEncoder().encodeToString(it)
					}
				}

				authentication {
					create<BasicAuthentication>("basic")
				}
			}
		}

		publications {
			create<MavenPublication>("maven") {
				from(components.getByName("java"))

				artifact(sourceJar)
				artifact(javadocJar)
//                artifact(dokkaJar)

				pom {
					name.set(project.ext.get("pubName").toString())
					description.set(project.ext.get("pubDesc").toString())

					url.set("https://kordex.dev")

					packaging = "jar"

					scm {
						connection.set("scm:git:https://github.com/Kord-Extensions/kord-extensions.git")
						developerConnection.set("scm:git:git@github.com:Kord-Extensions/kord-extensions.git")
						url.set("https://github.com/Kord-Extensions/kord-extensions.git")
					}

					licenses {
						license {
							name.set("Mozilla Public License Version 2.0")
							url.set("https://www.mozilla.org/en-US/MPL/2.0/")
						}
					}

					developers {
						developer {
							id.set("gdude2002")
							name.set("Gareth Coles")
						}
					}
				}
			}
		}
	}

	signing {
		val signingKey = System.getenv("SIGNING_KEY")?.toString()
		val signingPassword = System.getenv("SIGNING_KEY_PASSWORD")?.toString()
		if (signingKey != null && signingPassword != null) {
			useInMemoryPgpKeys(String(Base64.getDecoder().decode(signingKey)), signingPassword)
			sign(publishing.publications["maven"])
		}
	}
}
