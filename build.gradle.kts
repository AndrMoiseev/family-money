import dev.detekt.gradle.Detekt
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

abstract class VerifyFilesEqual : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val committedFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generatedFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val committed = committedFile.get().asFile
        val generated = generatedFile.get().asFile
        if (!committed.readBytes().contentEquals(generated.readBytes())) {
            throw GradleException(
                "OpenAPI contract drift detected. Run the generateOpenApi task and commit the result.",
            )
        }
    }
}

buildscript {
    dependencies {
        classpath(libs.kora.openapi.generator)
    }
}

plugins {
    application
    jacoco
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
}

group = "io.familymoney"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    sourceSets.main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/backend-server"))
    }
}

val koraBom = configurations.create("koraBom")
configurations {
    compileOnly.get().extendsFrom(koraBom)
    implementation.get().extendsFrom(koraBom)
    ksp.get().extendsFrom(koraBom)
    testImplementation.get().extendsFrom(koraBom)
}

dependencies {
    koraBom(platform(libs.kora.bom))

    implementation(libs.kora.config.hocon)
    implementation(libs.kora.http.server.undertow)
    implementation(libs.kora.json.module)
    implementation(libs.kora.logging.logback)
    ksp(libs.kora.symbol.processors)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "25"
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

application {
    applicationName = "family-money"
    mainClass.set("io.familymoney.bootstrap.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

val npmCommand = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"
val npxCommand = if (System.getProperty("os.name").startsWith("Windows")) "npx.cmd" else "npx"
val nodeCommand = if (System.getProperty("os.name").startsWith("Windows")) "node.exe" else "node"
val openApiFile = layout.projectDirectory.file("api/generated/openapi.yaml")
val openApiCheckDirectory = layout.buildDirectory.dir("generated/openapi-check")
val openApiCheckFile = openApiCheckDirectory.map { it.file("openapi.yaml") }
val backendApiDirectory = layout.buildDirectory.dir("generated/backend-server")

val npmCi =
    tasks.register<Exec>("npmCi") {
        description = "Installs the exact root npm workspace dependencies."
        group = "build setup"
        inputs.files("package.json", "package-lock.json", "frontend/package.json", ".npmrc")
        outputs.dir("node_modules")
        commandLine(npmCommand, "ci", "--ignore-scripts", "--no-audit", "--no-fund")
    }

val configureVueDemi =
    tasks.register<Exec>("configureVueDemi") {
        description = "Runs the reviewed vue-demi postinstall without a nested shell."
        group = "build setup"
        dependsOn(npmCi)
        commandLine(nodeCommand, "node_modules/vue-demi/scripts/postinstall.js")
    }

val generateOpenApi =
    tasks.register<Exec>("generateOpenApi") {
        description = "Generates the committed OpenAPI contract from TypeSpec."
        group = "code generation"
        dependsOn(configureVueDemi)
        inputs.files("api/main.tsp", "api/tspconfig.yaml")
        inputs.files("package.json", "package-lock.json")
        outputs.file(openApiFile)
        commandLine(npmCommand, "run", "api:generate")
    }

val generateOpenApiForCheck =
    tasks.register<Exec>("generateOpenApiForCheck") {
        description = "Generates OpenAPI into build/ for contract drift verification."
        group = "verification"
        dependsOn(configureVueDemi)
        inputs.files("api/main.tsp", "api/tspconfig.yaml")
        inputs.files("package.json", "package-lock.json")
        outputs.file(openApiCheckFile)
        val outputDirectory = openApiCheckDirectory.map { it.asFile.absolutePath.replace('\\', '/') }
        commandLine(
            npxCommand,
            "tsp",
            "compile",
            "api",
            "--option",
            outputDirectory.map { "@typespec/openapi3.emitter-output-dir=$it" }.get(),
        )
    }

val verifyOpenApi =
    tasks.register<VerifyFilesEqual>("verifyOpenApi") {
        description = "Fails when the committed OpenAPI differs from TypeSpec output."
        group = "verification"
        dependsOn(generateOpenApiForCheck)
        mustRunAfter(generateOpenApi)
        committedFile.set(openApiFile)
        generatedFile.set(openApiCheckFile)
    }

val generateBackendApi =
    tasks.register<GenerateTask>("generateBackendApi") {
        description = "Generates the Kotlin/Kora HTTP server contract into build/."
        group = "code generation"
        dependsOn(verifyOpenApi)
        generatorName.set("kora")
        inputSpec.set(openApiFile.asFile.absolutePath)
        outputDir.set(backendApiDirectory.get().asFile.absolutePath)
        apiPackage.set("io.familymoney.generated.api")
        modelPackage.set("io.familymoney.generated.api.model")
        invokerPackage.set("io.familymoney.generated.api")
        openapiNormalizer.set(
            mapOf(
                "DISABLE_ALL" to "true",
            ),
        )
        configOptions.set(
            mapOf(
                "mode" to "kotlin-server",
            ),
        )
    }

val generateFrontendClient =
    tasks.register<Exec>("generateFrontendClient") {
        description = "Generates the TypeScript Fetch client and Zod schemas into build/."
        group = "code generation"
        dependsOn(verifyOpenApi, configureVueDemi)
        inputs.files(openApiFile, "openapi-ts.config.ts", "package.json", "package-lock.json")
        outputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
        commandLine(npmCommand, "run", "api:generate-client")
    }

fun Exec.enableCodexWindowsNodeCompatibility() {
    val shim = layout.projectDirectory.file(".tools/windows-sandbox-node-shim.cjs").asFile
    if (System.getenv("CODEX_THREAD_ID") != null && shim.exists()) {
        environment("NODE_OPTIONS", "--require=${shim.absolutePath}")
    }
}

val frontendTypecheck =
    tasks.register<Exec>("frontendTypecheck") {
        description = "Typechecks the Vue application in strict mode."
        group = "verification"
        dependsOn(generateFrontendClient, configureVueDemi)
        inputs.files(fileTree("frontend") { exclude("dist/**") })
        inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
        commandLine(npmCommand, "--workspace", "frontend", "run", "typecheck")
    }

val frontendTest =
    tasks.register<Exec>("frontendTest") {
        description = "Runs frontend unit tests and produces V8 coverage reports."
        group = "verification"
        dependsOn(generateFrontendClient, configureVueDemi)
        inputs.files(fileTree("frontend") { exclude("dist/**") })
        inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
        commandLine(npmCommand, "--workspace", "frontend", "run", "test:coverage")
        enableCodexWindowsNodeCompatibility()
    }

val prettierCheck =
    tasks.register<Exec>("prettierCheck") {
        description = "Checks formatting for frontend, JSON, YAML, and Markdown."
        group = "verification"
        dependsOn(configureVueDemi)
        commandLine(npmCommand, "run", "format:check")
    }

val typeSpecFormatCheck =
    tasks.register<Exec>("typeSpecFormatCheck") {
        description = "Checks TypeSpec formatting with the TypeSpec formatter."
        group = "verification"
        dependsOn(configureVueDemi)
        commandLine(npmCommand, "run", "api:format:check")
    }

val frontendLint =
    tasks.register<Exec>("frontendLint") {
        description = "Runs ESLint for Vue and TypeScript."
        group = "verification"
        dependsOn(generateFrontendClient, configureVueDemi)
        inputs.files(fileTree("frontend") { exclude("dist/**") })
        inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
        commandLine(npmCommand, "--workspace", "frontend", "run", "lint")
    }

val frontendBuild =
    tasks.register<Exec>("frontendBuild") {
        description = "Builds the production frontend bundle."
        group = "build"
        dependsOn(frontendTypecheck, generateFrontendClient, configureVueDemi)
        inputs.files(fileTree("frontend") { exclude("dist/**") })
        inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
        outputs.dir("frontend/dist")
        commandLine(npmCommand, "--workspace", "frontend", "run", "build")
        enableCodexWindowsNodeCompatibility()
    }

tasks.named("check") {
    dependsOn(
        verifyOpenApi,
        frontendLint,
        frontendTest,
        frontendTypecheck,
        prettierCheck,
        typeSpecFormatCheck,
        "detekt",
        "spotlessCheck",
        "jacocoTestReport",
    )
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(frontendBuild)
    from("frontend/dist") {
        into("static")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateBackendApi)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xwarning-level=ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD:disabled",
            "-Xwarning-level=WRONG_NULLABILITY_FOR_JAVA_OVERRIDE:disabled",
        )
    }
}

tasks.matching { it.name == "kspKotlin" }.configureEach {
    dependsOn(generateBackendApi)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.wrapper {
    gradleVersion = "9.6.1"
    distributionType = Wrapper.DistributionType.BIN
    networkTimeout = 60_000
}
