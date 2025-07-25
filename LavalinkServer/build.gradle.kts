import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.apache.tools.ant.filters.ReplaceTokens
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.dokka")
    kotlin("plugin.serialization")
    alias(libs.plugins.maven.publish.base)
}

apply(plugin = "org.springframework.boot")
apply(plugin = "com.gorylenko.gradle-git-properties")
apply(plugin = "org.ajoberstar.grgit")
apply(plugin = "com.adarshr.test-logger")
apply(plugin = "kotlin")
apply(plugin = "kotlin-spring")

val archivesBaseName = "Lavalink"
group = "dev.arbjerg.lavalink"
description = "Play audio to discord voice channels"

application {
    mainClass = "lavalink.server.Launcher"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("nl.adaptivity.xmlutil.ExperimentalXmlUtilApi")
        }
    }
}

configurations {
    compileOnly {
        extendsFrom(annotationProcessor.get())
    }
}

dependencies {
    implementation(projects.protocol)
    implementation(projects.pluginApi) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    implementation(libs.xmlutil.jdk)
    implementation(libs.xmlutil.serialization)

    implementation(libs.bundles.metrics)
    implementation(libs.spring.cloud.config)
    implementation(libs.bundles.spring) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    implementation(libs.koe) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation(libs.koe.udpqueue) {
        exclude(module="udp-queue")
    }
    implementation(libs.bundles.udpqueue.natives) {
        exclude(group = "com.sedmelluq", module = "lava-common")
    }

    implementation("com.github.lavalink-devs:lavaplayer:2.2.4")
    implementation(libs.lavaplayer.ip.rotator)

    implementation(libs.lavadsp)
    implementation(libs.kotlin.reflect)
    implementation(libs.logback)
    implementation(libs.sentry.logback)
    implementation(libs.oshi) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testImplementation(libs.spring.boot.test)
}

tasks {
    build {
        doLast {
            println("Version: $version")
        }
    }

    processResources {
        val tokens = mapOf(
            "project.version" to project.version,
            "project.groupId" to project.group,
            "project.artifactId" to "Lavalink-Server",
            "env.BUILD_TIME" to System.currentTimeMillis().toString()
        )

        filter(ReplaceTokens::class, mapOf("tokens" to tokens))
        copy {
            from("application.yml.example")
            into(layout.buildDirectory.dir("resources/main"))
        }
    }

    named<AbstractArchiveTask>("bootDistTar") {
        archiveClassifier = "bootTar"
    }

    named<AbstractArchiveTask>("bootDistZip") {
        archiveClassifier = "bootZip"
    }

    named<Test>("test") {
        useJUnitPlatform()
    }

    val nativesJar = create<Jar>("lavaplayerNativesJar") {
        from(configurations.runtimeClasspath.get().find { it.name.contains("lavaplayer-natives") }?.let { file ->
            zipTree(file).matching {
                include {
                    it.path.contains("musl")
                }
            }
        })

        archiveBaseName = "lavaplayer-natives"
        archiveClassifier = "musl"
    }

    withType<BootJar> {
        archiveFileName = "Lavalink.jar"

        if (findProperty("targetPlatform") == "musl") {
            archiveFileName = "Lavalink-musl.jar"
            exclude {
                it.name.contains("lavaplayer-natives-fork") || (it.name.contains("udpqueue-native-") && !it.name.contains("musl"))
            }
            classpath(nativesJar.outputs)
            dependsOn(nativesJar)
        }
    }

    withType<BootRun> {
        dependsOn("compileTestJava")
        if (project.hasProperty("jvmArgs")) {
            val args = project.property("jvmArgs")
                .toString()
                .split("\\s".toPattern())
            jvmArgs?.addAll(args)
        }
    }
}

mavenPublishing {
    configure(KotlinJvm(JavadocJar.Dokka("dokkaHtml")))
    pom {
        name = "Lavalink Server"
        description = "Lavalink Server"
    }
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifact(tasks.named("bootJar"))
        }
    }
}
