# 02. Spring Boot 4系(Spring Framework 7)への移行

- Status: Proposed
- Issue: #103
- Date: 2026-07-31

## 1. 文脈

Java 25化(#102)の完了を前提に、Spring Bootを3系(現行3.4.1)から4系へ移行する計画を立てる。
Spring Boot 4.0はSpring Framework 7.0を基盤として2025-11-20にGAしており(Framework 7.0は
2025-11-13GA)、Java 25の第一級サポートを謳う。現行のSpring Boot 3.4.1はサポート期限が
近づいており、放置するほど後発のマイナーバージョンとの差分が積み上がる。

一方でSpring Boot 3→4はメジャーバージョンアップであり、3.x系で非推奨化されたAPIが
まとめて削除される(WebSecurityConfigurerAdapter、`@MockBean`/`@SpyBean`等)ほか、
JSON処理基盤がJackson 2からJackson 3(グループID`tools.jackson`)に切り替わる。
本プロジェクトは「正確性 > シンプルさ」を最重要原則としており、APIレスポンスの
シリアライズ挙動が意図せず変わることは絶対に避けなければならない。この判断は
後から覆すのが高くつく(依存関係・ビルド基盤・テスト基盤に広く波及する)ため、
ADRとして決定と却下案を残す。

## 2. 決定

Spring Bootを4系(Spring Framework 7ベース)に移行する。実装PRの中で以下を一括して行う:

- Gradleラッパーを8.14以上に更新する(Spring Boot 4の最低要件)
- `spring-boot-starter-web` → `spring-boot-starter-webmvc`、
  `spring-boot-starter-oauth2-client` → `spring-boot-starter-security-oauth2-client` へ
  artifact名を追随させる
- `io.jsonwebtoken:jjwt-jackson` を `io.jsonwebtoken:jjwt-gson` に差し替える
  (jjwt-jacksonはJackson 2依存でJackson 3と競合するため)
- `net.logstash.logback:logstash-logback-encoder` を9.0以降に更新する(Jackson 3対応版)
- `com.atlassian.oai:swagger-request-validator-mockmvc` をSpring Framework 7/Jakarta対応版に更新する
- Jackson 2→3の互換フラグ(`spring.jackson.use-jackson2-defaults=true`)は使わず、
  Jackson 3のデフォルト挙動を前提にcontractテストで検証し直す
- 実装PRは1本にまとめる(スパイク調査は本ADR・Plan PR作成時点の事前調査で完結させた)

**中止条件**: contractテスト基盤(`swagger-request-validator-mockmvc`)またはSpring Securityの
設定が実装PR着手時の実地検証でSpring Boot 4に対応しないと判明した場合、その時点で移行を
延期しSpring Boot 3系に留まる。Issueをbacklogに戻し、次のマイナーバージョンで再挑戦する。

## 3. 却下した案

- **`spring-boot-starter-classic`(移行ブリッジ)を使い、旧モジュール構成のまま留まる案**:
  Spring Bootが移行期間中の一時措置として提供しているが、将来的に削除される予定の
  非推奨経路であり、結局いつか本移行をやり直す必要がある。「シンプルさ」の原則に反し
  技術的負債を先送りするだけのため却下
- **`spring.jackson.use-jackson2-defaults=true` でJackson 2互換動作に留める案**:
  同様に暫定フラグへの依存が残り続け、Jackson 3本来のデフォルトへの追随を先送りするだけ
  のため却下(4.1参照)
- **調査と実装を別々のPR(スパイクPR→実装PR)に分割する案**:
  依頼者(運営者)の意向により、調査はPlan PR作成時点の事前調査で完結させ、実装PRは
  1本にまとめる方針を採用した。GitHubのStacked pull requestsは本Issueでは使用しない
- **Java 25化とSpring Boot 4化を1つのIssue・PRにまとめる案**:
  依頼者の意向により、Issueを#102(Java 25化)と#103(Spring Boot 4化)に分割した
  (依存関係: #103は#102完了後に着手)

## 4. 結果

**得られるもの**: 最新のSpring Framework 7・Jackson 3・Java 25の組み合わせによる
長期的なサポート継続性と、将来のマイナーバージョン追従コストの低減。

**引き受けるトレードオフ**: Jackson 3移行に伴うJSON処理ライブラリ(jjwt、
logstash-logback-encoder)の差し替え作業と、contractテストによる挙動検証コストを
実装PRで一括して負う。

**撤回条件**: 実装PR着手後にcontractテスト基盤またはSpring Securityの設定が
Spring Boot 4に対応しないと判明した場合(本ADR§2の中止条件)、本ADRの決定を
`Superseded by` で置き換え、Spring Boot 3系への継続維持を新しいADRとして記録する。
