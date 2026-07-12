package com.swiss_stage;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * DDDレイヤー構造の機械的強制(01_architecture_design.md)。
 */
@AnalyzeClasses(packages = "com.swiss_stage")
class ArchitectureTest {

    /** domain層は Spring / AWS SDK に依存してはならない */
    @ArchTest
    static final ArchRule domainは外部フレームワークに依存しない =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("com.swiss_stage.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "software.amazon.awssdk..",
                            "jakarta..");

    /** domain層は他の層(application/infrastructure/presentation)に依存してはならない */
    @ArchTest
    static final ArchRule domainは他レイヤーに依存しない =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("com.swiss_stage.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.swiss_stage.application..",
                            "com.swiss_stage.infrastructure..",
                            "com.swiss_stage.presentation..");

    /** presentation層はinfrastructure層に直接依存してはならない */
    @ArchTest
    static final ArchRule presentationはinfrastructureに依存しない =
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("com.swiss_stage.presentation..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.swiss_stage.infrastructure..")
                    // presentation層にクラスが増えるまでは検査対象ゼロを許容する
                    .allowEmptyShould(true);
}
