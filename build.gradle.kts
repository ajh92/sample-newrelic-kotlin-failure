import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Download

plugins {
    alias(mn.plugins.kotlin.jvm)
    alias(mn.plugins.kotlin.allopen)
    alias(mn.plugins.ksp)
    id("io.micronaut.application") version "4.5.0"
    id("io.micronaut.aot") version "4.5.0"
    id("de.undercouch.download") version "5.6.0"
}

version = "0.1"
group = "sample.newrelic.kotlin.failure"

repositories {
    mavenCentral()
}

dependencies {
    ksp(mn.micronaut.data.processor)
    ksp(mn.micronaut.http.validation)
    ksp(mn.micronaut.serde.processor)
    ksp(mn.micronaut.tracing.opentelemetry.annotation)

    implementation(mn.micronaut.data.r2dbc)
    implementation(mn.micronaut.r2dbc.core)
    implementation(mn.micronaut.kotlin.runtime)
    implementation(mn.micronaut.http.client)
    implementation(mn.micronaut.serde.jackson)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    runtimeOnly(mn.logback.classic)
    runtimeOnly(mn.slf4j.jul.to.slf4j)
    runtimeOnly(mn.snakeyaml)
    runtimeOnly(mn.r2dbc.postgresql)
    runtimeOnly(mn.micronaut.jdbc.hikari)

    aotPlugins(platform(mn.micronaut.platform))
}

application {
    mainClass.set("sample.newrelic.kotlin.failure.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    if (project.hasProperty("jvmArgs")) {
        jvmArgs(project.property("jvmArgs").toString().split(Regex("\\s+")))
    }
}

graalvmNative.toolchainDetection.set(false)

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("sample.newrelic.kotlin.failure.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}

tasks.register<Download>("downloadJavaAgent") {
    group = "NewRelic JARs"
    description = "Download NewRelic Java Agent"
    src("https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.20.0/newrelic-java.zip")
    dest(
        File.createTempFile("nragent", ".zip", temporaryDir),
    )
    overwrite(true)
    quiet(false)
}

tasks.register<Copy>("addAgent") {
    group = "NewRelic JARs"
    description = "Copy NewRelic Java Agent JAR to project directory"

    val downloadedFileProvider = tasks.named<Download>("downloadJavaAgent").map { it.dest }

    val jar =
        zipTree(downloadedFileProvider).apply {
            include("**/newrelic.jar")
            eachFile {
                relativePath = RelativePath.parse(true, name)
            }
            includeEmptyDirs = false
        }

    from(jar)
    into(project.rootDir) // Extracts to the root of your project
}

tasks.register<Download>("downloadCoroutineInstrumentation") {
    group = "NewRelic JARs"
    description = "Download NewRelic Java Agent"
    src("https://github.com/newrelic/newrelic-java-kotlin-coroutines/releases/download/v1.0.7/kotlin-coroutines-instrumentation-v1.0.7.zip")
    dest(
        File.createTempFile("nragent", ".zip", temporaryDir),
    )
    overwrite(true)
    quiet(false)
}

tasks.register<Copy>("addCoroutineInstrumentation") {
    group = "NewRelic JARs"
    description = "Copy NewRelic Java Agent JAR to project directory"

    val downloadedFileProvider = tasks.named<Download>("downloadCoroutineInstrumentation").map { it.dest }

    val jar =
        zipTree(downloadedFileProvider).apply {
            include("**/Kotlin-Coroutines_1.9.jar", "**/Kotlin-Coroutines-Suspends.jar")
            eachFile {
                relativePath = RelativePath.parse(true, name)
            }
            includeEmptyDirs = false
        }

    from(jar)
    into(File(project.rootDir, "extensions"))
}

tasks.register<Download>("downloadMicronautHttpInstrumentation") {
    group = "NewRelic JARs"
    description = "Download NewRelic Java Agent"
    src("https://github.com/newrelic/newrelic-java-micronaut-http/releases/download/v1.1.4/micronaut-http-instrumentation-v1.1.4.zip")
    dest(
        File.createTempFile("nragent", ".zip", temporaryDir),
    )
    overwrite(true)
    quiet(false)
}

tasks.register<Download>("downloadMicronautCoreInstrumentation") {
    group = "NewRelic JARs"
    description = "Download NewRelic Java Agent"
    src("https://github.com/newrelic/newrelic-java-micronaut-core/releases/download/v1.1.4/micronaut-core-instrumentation-v1.1.4.zip")
    dest(
        File.createTempFile("nragent", ".zip", temporaryDir),
    )
    overwrite(true)
    quiet(false)
}

tasks.register<Copy>("addMicronautInstrumentation") {
    group = "NewRelic JARs"
    description = "Copy NewRelic Java Agent JAR to project directory"

    val downloadedHttpFileProvider = tasks.named<Download>("downloadMicronautHttpInstrumentation").map { it.dest }

    val httpJar =
        zipTree(downloadedHttpFileProvider).apply {
            include("**/*.jar")
            eachFile {
                relativePath = RelativePath.parse(true, name)
            }
            includeEmptyDirs = false
        }

    val downloadedCoreFileProvider = tasks.named<Download>("downloadMicronautCoreInstrumentation").map { it.dest }

    val coreJar =
        zipTree(downloadedCoreFileProvider).apply {
            include("**/*.jar")
            eachFile {
                relativePath = RelativePath.parse(true, name)
            }
            includeEmptyDirs = false
        }

    from(httpJar, coreJar)
    into(File(project.rootDir, "extensions"))
}

// 5. (Optional) Register a convenience task to run both download and unzip

/*
tasks.register(mainTaskName) {
    group = "Custom Setup"
    description = "Downloads and unzips the archive to the project root."
    dependsOn(tasks.named(unzipTaskName))
    doLast {
        println("Setup from ZIP completed.")
    }
}
*/
