import java.security.MessageDigest

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.jk1.dependency-license-report") version "3.1.4"
}

group = "de.chesscoach"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // Chesslib wird ausschliesslich ueber JitPack (Build aus dem Git-Tag 1.3.7) veroeffentlicht.
    exclusiveContent {
        forRepository { maven("https://jitpack.io") }
        filter { includeGroup("com.github.bhlangonijr") }
    }
}

val chesslibVersion = "1.3.7"
val springdocVersion = "3.2.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("com.github.bhlangonijr:chesslib:$chesslibVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---------------------------------------------------------------------------
// Engine: Calvin wird als festgelegte Release-Datei mit dem Backend ausgeliefert.
// ---------------------------------------------------------------------------
val calvinVersion = "6.1.1"
val calvinJarName = "calvin-chess-engine-$calvinVersion.jar"
val calvinSha256 = "dab9383b478e41718251da2cf738f4f95df2130eb2a42baffc07ede27bb405fe"
val calvinUrl = "https://github.com/kelseyde/calvin-chess-engine/releases/download/$calvinVersion/$calvinJarName"
val engineFile = layout.projectDirectory.file("engine/$calvinJarName")

fun sha256(file: File): String =
    MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

val downloadEngine by tasks.registering {
    group = "engine"
    description = "Laedt die Calvin-Engine herunter, falls sie fehlt, und prueft die SHA-256-Pruefsumme."
    outputs.file(engineFile)
    doLast {
        val target = engineFile.asFile
        if (!target.exists()) {
            target.parentFile.mkdirs()
            logger.lifecycle("Lade $calvinUrl")
            uri(calvinUrl).toURL().openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        val actual = sha256(target)
        check(actual == calvinSha256) {
            "Pruefsumme der Engine stimmt nicht: erwartet $calvinSha256, gefunden $actual"
        }
    }
}

val verifyEngine by tasks.registering {
    group = "engine"
    description = "Prueft, dass die mitgelieferte Engine-Datei vorhanden und unveraendert ist."
    dependsOn(downloadEngine)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked"))
}

tasks.test {
    dependsOn(verifyEngine)
    useJUnitPlatform {
        if (project.hasProperty("skipEngineTests")) {
            excludeTags("engine")
        }
    }
    systemProperty("chesscoach.engine.calvin.jar-path", engineFile.asFile.absolutePath)
    systemProperty("chesscoach.docs.dir", rootProject.layout.projectDirectory.dir("../docs").asFile.absolutePath)
    jvmArgs("-Xshare:off")
    maxHeapSize = "1g"
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

// Mindestens 80 % Line Coverage fuer die eigene Coaching-Logik (Requirements 13.6).
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("de.chesscoach.coaching*", "de.chesscoach.domain.position*")
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

licenseReport {
    allowedLicensesFile = layout.projectDirectory.file("config/allowed-licenses.json").asFile
    filters = arrayOf(com.github.jk1.license.filter.LicenseBundleNormalizer())
}

// Optional: Frontend-Build in das Backend-JAR uebernehmen (ein einziges ausfuehrbares Artefakt).
val frontendDist = layout.projectDirectory.dir("../frontend/dist")
tasks.processResources {
    if (project.hasProperty("withFrontend")) {
        from(frontendDist) { into("static") }
    }
}

tasks.bootJar {
    archiveFileName = "chess-coach-backend.jar"
}

tasks.jar {
    enabled = false
}
