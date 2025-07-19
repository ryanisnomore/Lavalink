import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.ajoberstar.grgit.Grgit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.dokka") version "1.9.10" apply false
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
    id("org.ajoberstar.grgit") version "5.2.0"
    id("org.springframework.boot") version "3.1.0" apply false
    id("org.sonarqube") version "4.2.0.3129"
    id("com.adarshr.test-logger") version "3.2.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
    alias(libs.plugins.maven.publish.base) apply false
}

val (gitVersion, release) = versionFromGit()
logger.lifecycle("Version: $gitVersion (release: $release)")

allprojects {
    group = "lavalink"
    version = gitVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://m2.dv8tion.net/releases")
        maven("https://maven.lavalink.dev/releases")
        maven("https://maven.lavalink.dev/snapshots")
        maven("https://jitpack.io")
    }
}

subprojects {
    if (project.hasProperty("includeAnalysis")) {
        project.logger.lifecycle("applying analysis plugins")
        apply(from = "../analysis.gradle")
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }

    configurations.all {
        resolutionStrategy {
            force("dev.arbjerg:lavaplayer:2.2.4")
            eachDependency {
                if (requested.group == "dev.arbjerg" && requested.name == "lavaplayer") {
                    useVersion("2.2.4")
                    because("Force lavaplayer to version 2.2.4")
                }
            }
        }
    }

    afterEvaluate {
        plugins.withId(libs.plugins.maven.publish.base.get().pluginId) {
            configure<PublishingExtension> {
                val mavenUsername = findProperty("MAVEN_USERNAME")?.toString()
                val mavenPassword = findProperty("MAVEN_PASSWORD")?.toString()
                if (!mavenUsername.isNullOrEmpty() && !mavenPassword.isNullOrEmpty()) {
                    repositories {
                        val snapshots = "https://maven.lavalink.dev/snapshots"
                        val releases = "https://maven.lavalink.dev/releases"

                        maven(if (release) releases else snapshots) {
                            credentials {
                                username = mavenUsername
                                password = mavenPassword
                            }
                        }
                    }
                } else {
                    logger.lifecycle("Not publishing to maven.lavalink.dev because credentials are not set")
                }
            }
            if (release) {
                configure<MavenPublishBaseExtension> {
                    coordinates(group.toString(), project.the<BasePluginExtension>().archivesName.get(), version.toString())
                    val mavenCentralUsername = findProperty("mavenCentralUsername")?.toString()
                    val mavenCentralPassword = findProperty("mavenCentralPassword")?.toString()
                    if (!mavenCentralUsername.isNullOrEmpty() && !mavenCentralPassword.isNullOrEmpty()) {
                        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, false)
                        signAllPublications()
                    } else {
                        logger.lifecycle("Not publishing to OSSRH due to missing credentials")
                    }

                    pom {
                        url = "https://github.com/lavalink-devs/Lavalink"

                        licenses {
                            license {
                                name = "MIT License"
                                url = "https://github.com/lavalink-devs/Lavalink/blob/main/LICENSE"
                            }
                        }

                        developers {
                            developer {
                                id = "freyacodes"
                                name = "Freya Arbjerg"
                                url = "https://www.arbjerg.dev"
                            }
                        }

                        scm {
                            url = "https://github.com/lavalink-devs/Lavalink/"
                            connection = "scm:git:git://github.com/lavalink-devs/Lavalink.git"
                            developerConnection = "scm:git:ssh://git@github.com/lavalink-devs/Lavalink.git"
                        }
                    }
                }
            }
        }
    }
}

fun versionFromGit(): Pair<String, Boolean> {
    Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
        val headTag = git.tag
            .list()
            .find { it.commit.id == git.head().id }

        val clean = git.status().isClean || System.getenv("CI") != null
        if (!clean) {
            logger.lifecycle("Git state is dirty, version is a snapshot.")
        }

        return if (headTag != null && clean) headTag.name to true else "${git.head().id}-SNAPSHOT" to false
    }
}                                name = "MIT License"
                                url = "https://github.com/lavalink-devs/Lavalink/blob/main/LICENSE"
                            }
                        }

                        developers {
                            developer {
                                id = "freyacodes"
                                name = "Freya Arbjerg"
                                url = "https://www.arbjerg.dev"
                            }
                        }

                        scm {
                            url = "https://github.com/lavalink-devs/Lavalink/"
                            connection = "scm:git:git://github.com/lavalink-devs/Lavalink.git"
                            developerConnection = "scm:git:ssh://git@github.com/lavalink-devs/Lavalink.git"
                        }
                    }
                }
            }
        }
    }
}

@SuppressWarnings("GrMethodMayBeStatic")
fun versionFromGit(): Pair<String, Boolean> {
    Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
        val headTag = git.tag
            .list()
            .find { it.commit.id == git.head().id }

        val clean = git.status().isClean || System.getenv("CI") != null
        if (!clean) {
            logger.lifecycle("Git state is dirty, version is a snapshot.")
        }

        return if (headTag != null && clean) headTag.name to true else "${git.head().id}-SNAPSHOT" to false
    }
}
