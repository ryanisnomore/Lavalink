import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    alias(libs.plugins.maven.publish.base)
}

val archivesBaseName = "protocol"
group = "dev.arbjerg.lavalink"

kotlin {
    applyDefaultHierarchyTemplate()

    jvm {
        withJava()
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"  // Changed from JvmTarget.JVM_17 to string "17"
            }
        }
    }

    js(IR) {
        nodejs()
        browser()
        compilations.all {
            packageJson {
                val validVersion = """\d+\.\d+\.\d+""".toRegex()
                if (!validVersion.matches(project.version.toString())) {
                    version = "4.0.0"
                }
            }
        }
    }

    linuxX64()
    linuxArm64()

    mingwX64()

    macosArm64()
    macosX64()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()

    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

mavenPublishing {
    configure(KotlinMultiplatform(JavadocJar.Dokka("dokkaHtml")))
    pom {
        name = "Lavalink Protocol"
        description = "Protocol for Lavalink Client development"
    }
}

tasks {
    withType<KotlinJvmTest> {
        useJUnitPlatform()
    }
}

// Use system Node.Js on NixOS
if (System.getenv("NIX_PROFILES") != null) {
    extensions.configure<NodeJsRootExtension> {
        download = false
    }
}
