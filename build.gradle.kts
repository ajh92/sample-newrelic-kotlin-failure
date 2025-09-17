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

    implementation("io.r2dbc:r2dbc-pool")

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
    src("https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.24.0/newrelic-java.zip")
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
    src("https://github.com/newrelic/newrelic-java-kotlin-coroutines/releases/download/v1.0.8/kotlin-coroutines-instrumentation-v1.0.8.zip")
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

tasks.register<Download>("downloadMicronautAdditionalInstrumentation") {
    group = "NewRelic JARs"
    description = "Download NewRelic Java Agent"
    src("https://github.com/newrelic-experimental/newrelic-java-micronaut-additional/releases/download/v1.0.0/micronaut-additional-instrumentation-v1.0.0.zip")
    dest(
        File.createTempFile("nragent", ".zip", temporaryDir),
    )
    overwrite(true)
    quiet(false)
}

tasks.register<Copy>("addMicronautInstrumentation") {
    group = "NewRelic JARs"
    description = "Copy NewRelic Java Agent JAR to project directory"

    val downloadedHttpFileProvider = tasks.named<Download>("downloadMicronautAdditionalInstrumentation").map { it.dest }

    val additionalJar =
        zipTree(downloadedHttpFileProvider).apply {
            include("**/micronaut-aop-4.0.0.jar", "**/micronaut-data-tx-4.0.0.jar")
            eachFile {
                relativePath = RelativePath.parse(true, name)
            }
            includeEmptyDirs = false
        }

    from(additionalJar)
    into(File(project.rootDir, "extensions"))
}
