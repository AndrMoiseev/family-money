import org.gradle.api.tasks.Exec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.openapi.generator)
}

group = "io.familymoney"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
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
        kotlin.srcDir(layout.buildDirectory.dir("generated/backend-api/src/main/kotlin"))
    }
}

val koraBom = configurations.create("koraBom")
configurations {
    kapt.get().extendsFrom(koraBom)
    compileOnly.get().extendsFrom(koraBom)
    implementation.get().extendsFrom(koraBom)
    testImplementation.get().extendsFrom(koraBom)
}

dependencies {
    koraBom(platform(libs.kora.bom))

    kapt(libs.kora.annotation.processors)
    implementation(libs.kora.config.hocon)
    implementation(libs.kora.http.server.undertow)
    implementation(libs.kora.json.module)
    implementation(libs.kora.logging.logback)
    compileOnly(libs.jackson.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
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

val npmCi = tasks.register<Exec>("npmCi") {
    description = "Installs the exact root npm workspace dependencies."
    group = "build setup"
    inputs.files("package.json", "package-lock.json", "frontend/package.json", ".npmrc")
    outputs.dir("node_modules")
    commandLine(npmCommand, "ci", "--ignore-scripts", "--no-audit", "--no-fund")
}

val configureVueDemi = tasks.register<Exec>("configureVueDemi") {
    description = "Runs the reviewed vue-demi postinstall without a nested shell."
    group = "build setup"
    dependsOn(npmCi)
    commandLine(nodeCommand, "node_modules/vue-demi/scripts/postinstall.js")
}

val generateOpenApi = tasks.register<Exec>("generateOpenApi") {
    description = "Generates the committed OpenAPI contract from TypeSpec."
    group = "code generation"
    dependsOn(configureVueDemi)
    inputs.files("api/main.tsp", "api/tspconfig.yaml")
    inputs.files("package.json", "package-lock.json")
    outputs.file(openApiFile)
    commandLine(npmCommand, "run", "api:generate")
}

val generateOpenApiForCheck = tasks.register<Exec>("generateOpenApiForCheck") {
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

val verifyOpenApi = tasks.register<Exec>("verifyOpenApi") {
    description = "Fails when the committed OpenAPI differs from TypeSpec output."
    group = "verification"
    dependsOn(generateOpenApiForCheck)
    val committedPath = openApiFile.asFile.absolutePath
    val generatedPath = openApiCheckFile.get().asFile.absolutePath
    if (System.getProperty("os.name").startsWith("Windows")) {
        commandLine("cmd", "/c", "fc", "/b", committedPath, generatedPath)
    } else {
        commandLine("cmp", "--silent", committedPath, generatedPath)
    }
}

val generateBackendApi = tasks.register<GenerateTask>("generateBackendApi") {
    description = "Generates Kotlin HTTP transport models into build/."
    group = "code generation"
    dependsOn(generateOpenApi)
    generatorName.set("kotlin")
    inputSpec.set(openApiFile.asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/backend-api").get().asFile.absolutePath)
    modelPackage.set("io.familymoney.generated.api.model")
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
            "supportingFiles" to "false",
        ),
    )
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "jackson",
            "sourceFolder" to "src/main/kotlin",
        ),
    )
}

val generateFrontendClient = tasks.register<Exec>("generateFrontendClient") {
    description = "Generates the TypeScript Fetch client and Zod schemas into build/."
    group = "code generation"
    dependsOn(generateOpenApi, configureVueDemi)
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

val frontendTypecheck = tasks.register<Exec>("frontendTypecheck") {
    description = "Typechecks the Vue application in strict mode."
    group = "verification"
    dependsOn(generateFrontendClient, configureVueDemi)
    inputs.files(fileTree("frontend") { exclude("dist/**") })
    inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
    commandLine(npmCommand, "--workspace", "frontend", "run", "typecheck")
}

val frontendTest = tasks.register<Exec>("frontendTest") {
    description = "Runs frontend unit tests."
    group = "verification"
    dependsOn(generateFrontendClient, configureVueDemi)
    inputs.files(fileTree("frontend") { exclude("dist/**") })
    inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
    commandLine(npmCommand, "--workspace", "frontend", "run", "test")
    enableCodexWindowsNodeCompatibility()
}

val frontendLint = tasks.register<Exec>("frontendLint") {
    description = "Runs ESLint for Vue and TypeScript."
    group = "verification"
    dependsOn(generateFrontendClient, configureVueDemi)
    inputs.files(fileTree("frontend") { exclude("dist/**") })
    inputs.dir(layout.buildDirectory.dir("generated/frontend-client"))
    commandLine(npmCommand, "--workspace", "frontend", "run", "lint")
}

val frontendBuild = tasks.register<Exec>("frontendBuild") {
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
    dependsOn(verifyOpenApi, frontendLint, frontendTest, frontendTypecheck)
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
        jvmTarget.set(JvmTarget.JVM_25)
        allWarningsAsErrors.set(true)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "9.6.1"
    distributionType = Wrapper.DistributionType.BIN
    networkTimeout = 60_000
}
