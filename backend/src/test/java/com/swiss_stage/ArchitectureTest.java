package com.swiss_stage;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
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

    private static final Set<String> NAMED_VALUE_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.PathVariable",
            "org.springframework.web.bind.annotation.RequestParam",
            "org.springframework.web.bind.annotation.RequestHeader",
            "org.springframework.web.bind.annotation.CookieValue");

    /**
     * リクエスト引数アノテーションは名前を明示する。
     * 名前を省略するとコンパイル時の -parameters フラグに依存し、
     * IDE(Eclipse JDT)ビルドで起動した場合に実行時エラーになるため。
     */
    @ArchTest
    static final ArchRule リクエスト引数アノテーションは名前を明示する =
            ArchRuleDefinition.methods()
                    .that().areDeclaredInClassesThat()
                    .resideInAPackage("com.swiss_stage.presentation..")
                    .should(new ArchCondition<>(
                            "@PathVariable/@RequestParam/@RequestHeader/@CookieValue に名前を明示する") {
                        @Override
                        public void check(JavaMethod method, ConditionEvents events) {
                            for (JavaParameter parameter : method.getParameters()) {
                                for (JavaAnnotation<JavaParameter> annotation : parameter.getAnnotations()) {
                                    String type = annotation.getRawType().getName();
                                    if (NAMED_VALUE_ANNOTATIONS.contains(type) && !hasExplicitName(annotation)) {
                                        events.add(SimpleConditionEvent.violated(method,
                                                "%s の引数 %d の @%s に名前が明示されていない".formatted(
                                                        method.getFullName(), parameter.getIndex(),
                                                        annotation.getRawType().getSimpleName())));
                                    }
                                }
                            }
                        }
                    });

    private static boolean hasExplicitName(JavaAnnotation<?> annotation) {
        return Stream.of("value", "name")
                .map(annotation::tryGetExplicitlyDeclaredProperty)
                .flatMap(Optional::stream)
                .anyMatch(v -> v instanceof String s && !s.isEmpty());
    }
}
