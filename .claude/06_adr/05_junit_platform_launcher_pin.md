# 05. junit-platform-launcherのバージョン固定(Spring Boot 4移行時に判明した不整合への対応)

- Status: Accepted
- Issue: #103
- Date: 2026-08-01

## 1. 文脈

Spring Boot 4系への移行(#103)の実装PRで、`./gradlew test`が全テストエンジン
(JUnit Jupiter・jqwik・ArchUnit JUnit 5)とも`initializationError`で起動失敗する
現象が発生した。原因は`org.junit.platform.commons.JUnitException`が指す
`NoSuchMethodError`で、`junit-platform-launcher`が`1.14.2`、`junit-jupiter`/
`junit-platform-engine`/`junit-platform-commons`が`6.0.3`という不整合な組み合わせで
解決されていたことによる。

`spring-boot-starter-test:4.0.7`自体のPOM・Gradle Module Metadataにも、
`spring-boot-dependencies:4.0.7`のBOM(`junit-bom:6.0.3`を`import`、
`junit-bom:6.0.3`自体は`junit-platform-launcher`を正しく`6.0.3`と管理)にも、
`junit-platform-launcher`を`1.14.2`に固定する記述は見当たらなかった
(`dependencyInsight`・各POM/`.module`を実地調査したが特定できず)。

この不整合への対応(`resolutionStrategy.eachDependency`での明示的なバージョン固定)は、
外すと全テストが起動失敗する「以後この規約に従わせる」性質の決定であり、`CLAUDE.md`の
「避けるべき落とし穴」に項目(#16)として記録した。`04_development_process.md` §3の
条件2(複数案を比較して1つを選んだ)・条件3(CLAUDE.mdの落とし穴が増える決定)の両方に
該当するため、本ADRを起票する(`06_adr/03_java25_bytecode_release_pin.md`で同種の
決定にADRを起こした先例と判断基準を揃える)。

## 2. 決定

`backend/build.gradle`に以下を追加し、`junit-platform-launcher`を`6.0.3`に固定する。

```gradle
configurations.all {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'org.junit.platform'
            && details.requested.name == 'junit-platform-launcher') {
            details.useVersion('6.0.3')
            details.because('junit-jupiter 6.0.3との組み合わせに必要(実測でNoSuchMethodError)')
        }
    }
}
```

## 3. 却下した案

- **`dependencies { testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.0.3' }`
  という通常のバージョン指定**: 実測したところ、この宣言をしても解決結果は`1.14.2`のまま
  変わらなかった(`dependencyInsight`で確認)。却下というより「効果がなかった」ため次の
  手段に進んだ
- **`configurations.testRuntimeClasspath.resolutionStrategy.force
  'org.junit.platform:junit-platform-launcher:6.0.3'`**: `force`を使っても解決結果は
  `1.14.2`のままだった。原因は特定できていないが、効果がないことを実測で確認したため却下
- **`dependencyManagement { dependencies { dependency
  'org.junit.platform:junit-platform-launcher:6.0.3' } }`(io.spring.dependency-management
  のDSL)**: 同様に効果がなく、解決結果は`1.14.2`のままだった。原因未特定のまま却下
- **`spring-boot-starter-test`・`spring-boot-starter-webmvc-test`から
  `junit-platform-launcher`を`exclude`し、別途`testRuntimeOnly`で`6.0.3`を宣言する案**:
  この組み合わせでは動作したが(`dependencyInsight`で`6.0.3`が選択されることを確認)、
  `resolutionStrategy.eachDependency`1つで同じ効果が得られ、除外設定を複数箇所に
  重複して書く必要がなくなるため、シンプルさを優先してeachDependencyのみに整理した

## 4. 結果

**得られるもの**: `junit-platform-launcher`と`junit-jupiter`系のバージョンを常に
一致させる仕組みが手に入り、依存関係の更新で同種の不整合が再発しても
`resolutionStrategy.eachDependency`のバージョン文字列を書き換えるだけで対応できる。

**引き受けるトレードオフ**: Gradleの依存関係解決に手動介入するコードがビルドスクリプトに
残り続ける。将来`spring-boot-starter-test`側でこの不整合が解消された場合、この設定は
不要になるが、削除し忘れても実害はない(単に無駄な`eachDependency`呼び出しが残るだけで、
正しいバージョンを指定し直すだけなので害はない)。

**撤回条件**: 将来のSpring Boot / JUnitのバージョンアップで、`dependencyInsight`により
`junit-platform-launcher`が素の`dependencies{}`宣言で正しく`junit-jupiter`と揃うことを
確認できた場合、この`resolutionStrategy.eachDependency`ブロックと`CLAUDE.md`落とし穴#16を
削除してよい(その際は新しいADRで撤回を記録する。`06_adr/04_java25_bytecode_release_pin_removed.md`
と同じパターン)。
