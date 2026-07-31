package io.familymoney.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ModuleArchitectureTest {
    private val importedClasses = ClassFileImporter().importPackages("io.familymoney")

    @Test
    fun `only accepted top-level packages are used`() {
        classes()
            .that()
            .resideInAPackage("io.familymoney..")
            .should()
            .resideInAnyPackage(
                "io.familymoney.bootstrap..",
                "io.familymoney.generated..",
                "io.familymoney.integration..",
                "io.familymoney.planning..",
                "io.familymoney.portfolio..",
                "io.familymoney.reporting..",
                "io.familymoney.sharedkernel..",
            ).check(importedClasses)
    }

    @Test
    fun `module internals are private`() {
        businessModules.forEach { module ->
            noClasses()
                .that()
                .resideOutsideOfPackage("io.familymoney.$module..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("io.familymoney.$module.internal..")
                .allowEmptyShould(true)
                .check(importedClasses)
        }
    }

    @Test
    fun `business module dependencies follow the accepted graph`() {
        allowedBusinessDependencies.forEach { (module, allowedDependencies) ->
            val forbiddenDependencies = businessModules - allowedDependencies - module
            noClasses()
                .that()
                .resideInAPackage("io.familymoney.$module..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    *forbiddenDependencies.map { "io.familymoney.$it.." }.toTypedArray(),
                ).allowEmptyShould(true)
                .check(importedClasses)
        }
    }

    @Test
    fun `domain and application code are independent from adapters`() {
        noClasses()
            .that()
            .resideInAnyPackage(
                "io.familymoney..internal.domain..",
                "io.familymoney..internal.application..",
            ).should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "ai.timefold..",
                "io.familymoney.generated..",
                "org.flywaydb..",
                "org.postgresql..",
                "ru.tinkoff.kora..",
            ).allowEmptyShould(true)
            .check(importedClasses)
    }

    private companion object {
        val businessModules =
            setOf(
                "integration",
                "planning",
                "portfolio",
                "reporting",
                "sharedkernel",
            )

        val allowedBusinessDependencies =
            mapOf(
                "integration" to setOf("portfolio", "sharedkernel"),
                "planning" to setOf("portfolio", "sharedkernel"),
                "portfolio" to setOf("sharedkernel"),
                "reporting" to setOf("portfolio", "sharedkernel"),
                "sharedkernel" to emptySet(),
            )
    }
}
