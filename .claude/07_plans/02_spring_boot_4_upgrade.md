# 02. Spring Boot 4系(Spring Framework 7)への移行

- Status: planned
- Issue: #103
- PR: -

---

## 1. 背景・目的

現時点で差し迫った不具合はないが、将来に備えた計画的なバージョンアップとしてSpring Bootを
3系(現行3.4.1)から4系(Spring Framework 7ベース)へ移行する。

Spring Boot 4.0はSpring Framework 7.0を基盤として2025-11-20にGAされており(Spring Framework 7.0は
2025-11-13GA)、Java 25を第一級サポートする(Java 17は引き続きサポート)。Java 25化(#102)の
完了後に着手する前提であり、本Plan PRはSpring Boot 4系化(#103)のみを対象とする。
本Plan PR作成時点(2026-07-31)で#102は未着手(Open、実装PRなし)。本ADRの`Status`は
本Plan PRのマージ時点で`Accepted`に更新するが、それは「移行方針が決まった」ことを意味するのみで、
実際の着手可能時期は引き続き#102の完了を待つ(ADR Acceptedと実装着手は別軸)。

このIssueは種別`chore`だが「アーキテクチャ・技術選定の決定を含む」に該当するため、
`04_development_process.md` §2 のトリガー表に従いPlan PR・ADR([`06_adr/02_spring_boot_4_upgrade.md`](../06_adr/02_spring_boot_4_upgrade.md))が必須。
一方で同じ表の「受け入れケース」列は当該行が `—`(対象外)であり、利用者から見た挙動変更を
伴わないため本計画では受け入れケースを追加しない(§5参照)。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**
→ 本移行における「正確性」とは、既存の全テスト(domain層TDD・DynamoDB Local統合テスト・
contractテスト・ArchUnit)が移行後も従来と同じ結果を保証すること。特にJackson 2→3の
デフォルト挙動差(後述)はテストなしでは気づけない「静かな破壊」であるため最優先で潰す。

## 2. 画面シナリオ / 3. UI仕様

該当なし(バックエンドのビルド・フレームワークバージョンの更新のみで、利用者から見える
画面・挙動・APIレスポンスの内容に変更はない。`03_feature_plan_template.md` 冒頭の
「対象外: リファクタリング」に準ずるため省略)。

## 4. 技術設計

事前調査(Web検索、2026-07-31時点の情報)で判明した変更点を整理する。

### 4.1 ビルド基盤

| 項目 | 現状 | 変更後 | 根拠 |
|---|---|---|---|
| Gradleラッパー | ~~8.12~~ → **9.6.1(#102で対応済み)** | 変更不要 | #102(Java 25化)のCIで、Gradle 8.12は実行JVM自体がJava25を認識できず(`Unsupported class file major version 69`)全ジョブが失敗することが判明し、Gradle 9.6.1に更新済み。Spring Boot 4の要件(最低Gradle 8.14)は満たしている |
| `info.solidsoft.pitest`プラグイン | ~~1.15.0~~ → **1.19.0(#102で対応済み)** | 変更不要 | Gradle 9で`reporting.baseDir`が削除され1.15.0が起動不能になったため#102で更新済み |
| Spring Bootプラグイン | 3.4.1 | 4.0.x(最新パッチ) | — |
| Javaツールチェイン | 21 | 25(#102で先行対応) | Spring Boot 4はJava 17+互換、25を第一級サポート |
| `options.release`(JavaCompile) | **21に固定(#102で導入)** | **25に戻せるか要検証** | #102でSpring Boot 3.4.1のASM(`SimpleMetadataReader`)がJava25バイトコードを読めないためreleaseを21に固定した(`CLAUDE.md`落とし穴#16)。Spring Framework 7でこの制約が解消されているか実装PR着手時に確認し、解消されていれば削除してtoolchainとreleaseの両方をJava25に揃える |

### 4.2 依存関係(`backend/build.gradle`)

| 依存関係 | 現状 | 対応方針 | リスク |
|---|---|---|---|
| `spring-boot-starter-web` | 3.x | `spring-boot-starter-webmvc` にartifact名変更 | 中(ビルド設定のみ、機能影響小) |
| `spring-boot-starter-oauth2-client` | 3.x | `spring-boot-starter-security-oauth2-client` にartifact名変更 | 中(OAuth2クライアントプロパティのキー再編があり得るため`13_security_design.md`記載の設定を確認) |
| `spring-boot-starter-actuator` / `spring-boot-starter-validation` | 3.x | パッケージ自体は継続、BOM経由でバージョン追随 | 低 |
| **`io.jsonwebtoken:jjwt-jackson`** | 0.12.6 | **`io.jsonwebtoken:jjwt-gson`(0.12.7)に差し替え** | **高**: jjwt-jacksonはJackson 2依存でSpring Boot 4のJackson 3と競合する。GSON実装への切り替えが必要 |
| `net.logstash.logback:logstash-logback-encoder` | 8.0 | **9.0以降に更新** | 高: 8系はJackson 3非対応(9.0でJackson 3必須化・対応) |
| `com.atlassian.oai:swagger-request-validator-mockmvc` | 2.44.1 | Spring 7/Boot4/Jakarta対応版(2.27.x系列以降、要最新確認)に更新 | 中: contractテスト基盤の要。バージョン系列の数字が現行より小さく見えるが別採番系列のため、実際に上げてテストが通ることをスパイクで確認する |
| `com.tngtech.archunit:archunit-junit5` | 1.3.0 | 1.4.x系に更新 | 低(Spring Boot 4での動作実績あり) |
| `net.jqwik:jqwik` | 1.9.2 | 現行のまま(Spring非依存) | 低 |
| `com.bucket4j:bucket4j-core` / `com.github.ben-manes.caffeine:caffeine` | 現行 | 現行のまま(Spring非依存) | 低 |
| `software.amazon.awssdk` BOM / `dynamodb-enhanced` | 2.29.45 | 最新パッチに更新(内部Jacksonをシェーディングしており本体のJackson 3化と直接競合しない見込み) | 中(実地確認が必要) |

### 4.3 Jackson 2 → 3(最大の懸念点)

Spring Boot 4はJackson 3(グループID `tools.jackson`。`jackson-annotations`のみ`com.fasterxml.jackson.annotation`のまま)を前提とする。

- 本プロジェクトのコードは `com.fasterxml.jackson.*` を直接importしていないか事前に確認する(controller/DTOはSpring標準のJSON変換に任せている想定)
- Jackson 3のデフォルト挙動変更(プロパティのアルファベット順ソートなど)がAPIレスポンスの
  フィールド順に影響しうる。`schema/openapi.yaml`のcontractテストはフィールドの**存在**を
  検証する方式であり順序に依存しないことを確認するが、フロントエンドのスナップショット系
  テスト(あれば)への影響も確認する
- 移行の逃げ道として `spring.jackson.use-jackson2-defaults=true` があるが、**採用しない**
  (「シンプルさ」の原則。将来的に必ず剥がすことになる互換フラグを増やさない)

### 4.4 テスト基盤

- `@SpringBootTest` はSpring Boot 4で自動的にMockMvcを提供しなくなる。本プロジェクトの
  `ApiContractTestSupport` はすでに `@AutoConfigureMockMvc` を明示しているため影響なし(要再確認)
- `@MockBean`/`@SpyBean` は削除され `@MockitoBean`/`@MockitoSpyBean` に統合される。
  `grep -rn "@MockBean\|@SpyBean" backend/src/test/` で0件を確認済み(2026-07-31時点)。
  移行時に再度0件であることを確認する

### 4.5 レイヤーごとの変更点

- domain / application: 変更なし(Spring/AWS SDK非依存の原則どおり)
- infrastructure: DynamoDB Enhanced Client設定、`SecurityConfig`(OAuth2クライアントプロパティキー)の確認・必要なら追随
- presentation: artifact名変更に伴うimport・自動設定クラス名の変更があれば追随。加えて、
  `presentation/WebMvcConfig#addResourceHandlers` のSPAフォールバック実装(`PathResourceResolver`を
  継承したリソースハンドラ方式)は、「Spring Boot 3のPathPatternParserでは`/**/{path}`形式の
  コントローラマッピングが使えない」というSpring Boot 3固有の制約を前提にした設計判断
  (`04_react_router_patterns.md` §5)。Spring Framework 7でこの制約が変わっていないか
  実装PR着手時に再検証し、前提が変わっていれば`04_react_router_patterns.md`を更新する(§6)
- API変更・マッチング/順位計算の仕様変更: なし(`schema/openapi.yaml`・`05_swiss_pairing_algorithm.md`の更新は不要)

## 5. 受け入れケース

追加なし。`04_development_process.md` §2 トリガー表の「アーキテクチャ・技術選定」行は
受け入れケース列が `—`(対象外)であり、本移行は利用者から見えるAPI契約・画面挙動を
変更しない(既存のcontractテスト・E2Eがグリーンであることが受け入れ条件)。

## 6. 更新する設計資料

- [x] `.claude/06_adr/02_spring_boot_4_upgrade.md` — 本PRで新規作成(ADR、Status: Proposed。本PRマージ時にAcceptedへ更新)
- [ ] `CLAUDE.md` — 技術スタック表の「Spring Boot 3.x」の更新に加え、「避けるべき落とし穴」#4
      「`spring-data-dynamodb` を追加しない(Spring Boot 3非対応)」の文言もバージョン前提が
      古くなるため実装PRで見直す(実装PRで完了後に反映。本Plan PRでは変更しない)
- [x] `.claude/01_development_docs/01_architecture_design.md` — #102(Java 25化)で「Spring Boot (Java 21)」→
      「Spring Boot (Java 25)」に更新済み。Spring Boot自体のバージョン番号は記載されていないため、
      本Issue(#103)での追加更新は不要と判明した
- [ ] `.claude/03_library_docs/02_dynamodb_enhanced_client.md` — タイトル「DynamoDB × Spring Boot 3
      実装パターン」のバージョン表記を実装PRで更新
- [ ] `.claude/03_library_docs/04_react_router_patterns.md` — §5のSPAフォールバック実装が前提とする
      「Spring Boot 3のPathPatternParserの制約」を実装PR着手時に再検証し、前提が変われば更新(§4.5参照)
- [ ] `.claude/05_acceptance/01_acceptance_scope.md` — 対象外(§5参照)
- [ ] `schema/openapi.yaml` — 対象外(API変更なし)
- [ ] `.claude/01_development_docs/05_swiss_pairing_algorithm.md` — 対象外(仕様変更なし)

> 上記のうちADR以外(CLAUDE.md・01_architecture_design.md・02_dynamodb_enhanced_client.md・
> 04_react_router_patterns.md)の実際の書き換えは**実装PR**で行う(バージョン番号・制約の
> 再検証結果は実装が完了して初めて確定するため)。本Plan PRではADRのみ新規作成する。

## 7. DoD(完了の定義)

- [ ] `./gradlew check` が Java 25 / Spring Boot 4 / Gradle 9.6.1(#102で導入済み)の組み合わせで通る
- [ ] `options.release = 21`(#102で導入、`CLAUDE.md`落とし穴#16)を25に戻せるか検証し、
      戻せるなら削除する(戻せない場合は理由を本Planまたは後続ADRに記録する)
- [ ] domain層のカバレッジ閾値(90%以上)を実装PRでも維持している
- [ ] contractテスト・ArchUnitテストが全件グリーン
- [ ] Jackson 2→3移行に伴うAPIレスポンスの挙動差分がないことをcontractテストで確認済み
- [ ] Jackson 2→3の挙動差分がフロントエンドに影響しないことを、既存のE2Eクリティカルパス
      またはフロントエンドのMSW/契約整合テストで確認済み
- [ ] `jjwt-jackson` → `jjwt-gson` の差し替え後もJWT発行・検証が既存テストで確認済み
- [ ] `CLAUDE.md`・`01_architecture_design.md`・`02_dynamodb_enhanced_client.md`・
      `04_react_router_patterns.md`(該当する場合)のバージョン記載が実装PRで更新されている
- [ ] ローカル実機で動作確認済み(`.claude/skills/verify`)

## 8. リスク・未確定事項

- **最大のリスク**: Jackson 2→3のデフォルト挙動変更が、テストでは検知しにくい形でAPIレスポンス
  (日時フォーマット・null省略・数値表現等)に影響する可能性。実装PR着手時に、契約テストが
  レスポンスの**形**(値の型・必須フィールド)を検証しているか再点検し、不足があれば先に
  contractテストを強化してから移行する
- **中止条件**(ADR記載の中止条件と同一): contractテスト基盤(`swagger-request-validator-mockmvc`)
  またはSpring Securityの設定が実地検証でSpring Boot 4に対応しないと判明した場合、その時点で
  移行を延期しBoot 3系に留まる。Issueをbacklogに戻し、次のマイナーバージョンで再挑戦する
- **却下した代替**: Jackson 3対応を避けるため `spring.jackson.use-jackson2-defaults=true` で
  互換動作に留める案は、いずれ剥がす前提の暫定措置が増え「シンプルさ」に反するため却下(§4.3)
- **`options.release = 21`固定の解除確認**: #102でSpring Boot 3.4.1のASM制約により導入した
  この固定(`CLAUDE.md`落とし穴#16)は、Spring Framework 7がJava25のクラスファイル形式を
  読めるようになっていれば本Issueで削除できるはずである。実装PR着手時に最初に確認すること
  (確認を怠ると、releaseだけ21のまま取り残され、toolchainとreleaseの不一致が放置される)
- 実装PRは1本にまとめる(スパイク調査は本Plan PRの技術設計セクションで完結させた。ユーザーの
  意向により、GitHubのStacked pull requestsは本Issueでは使用しない)
- **中止条件に達した場合の切り戻し手順**: 実装ブランチをマージせずクローズし、
  `backend/build.gradle`は変更前の状態(Spring Boot 3.4.1系)のまま維持する。本ADRは
  編集せず、新しいADRを起こして`Superseded by`で置き換え、Boot 3系継続の判断として記録する
  (`04_development_process.md` §4「書き換えず積む」原則)
