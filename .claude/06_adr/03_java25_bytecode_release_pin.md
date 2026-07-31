# 03. Java 25対応時のバイトコードrelease固定(Spring Boot 4移行までの暫定措置)

- Status: Accepted
- Issue: #102
- Date: 2026-08-01

## 1. 文脈

Java 25化(#102)を実装中、`backend/build.gradle`のtoolchainをJava 25にしたところ、
Spring Boot 3.4.1のクラスパススキャン(ASMベースの`SimpleMetadataReader`)がJava 25の
クラスファイル形式(major version 69)を認識できず、全`@SpringBootTest`が
`ClassFormatException`で起動失敗することが実測で判明した。Spring Boot自体の
アップグレードは別Issue(#103、`06_adr/02_spring_boot_4_upgrade.md`)の対象であり、
#102の完了時点ではSpring Bootを上げない方針を維持したい。

この制約への対応(`options.release = 21`固定)は、外すと全テストが壊れる「以後この
規約に従わせる」性質の決定であり、`CLAUDE.md`の「避けるべき落とし穴」に項目(#16)を
追加した。`04_development_process.md` §3の条件3(CLAUDE.mdの落とし穴が増える決定)に
該当するため、本ADRを起票する。

## 2. 決定

`backend/build.gradle`の`tasks.withType(JavaCompile)`に`options.release = 21`を
追加し、コンパイル・テスト実行はJDK25(toolchain)で行いつつ、生成バイトコードは
Java21相当(class file major version 65)に固定する。

Spring Boot 4系への移行(#103)着手時に、Spring Framework 7のクラスパススキャンが
Java25のクラスファイル形式を認識できるか再検証し、認識できればこのrelease固定を
解除してtoolchainとreleaseの両方をJava25に揃える(`.claude/07_plans/02_spring_boot_4_upgrade.md`
§4.1・§7・§8に確認タスクとして記載済み)。

## 3. 却下した案

- **toolchain自体も21のまま据え置く案**: Issue #102の目的(Java25への移行)を
  達成できないため却下。今回の制約はコンパイル後のバイトコード形式の問題であり、
  JDKそのものを25に上げてビルド・テストを実行すること自体は可能なため、release指定
  という部分的な対応で両立できる
- **Spring Boot 4への先行アップグレードを本Issue(#102)に含めて同時に行う案**:
  #103として既に承認済みのPlan PR(#104)・ADR(02)が、Jackson 2→3移行等のリスクを
  考慮した独立した実装PRとして計画されている。#102に混ぜると、進捗管理単位
  (Issueごとに1つの実装PR)が崩れ、#102で問題が起きた際の切り分けも困難になるため
  却下。ユーザー確認の上、既存の2Issue構成を維持する判断とした
- **`spring.jackson.use-jackson2-defaults=true`等の互換フラグでの回避**:
  そもそもJackson 3自体ではなくASMのクラスファイル読み取りの問題であり、Jackson側の
  互換フラグでは解決しないため検討対象外

## 4. 結果

**得られるもの**: Java 25(toolchain)でのコンパイル・テスト実行という#102の目的を、
Spring Boot自体を上げずに達成できる。

**引き受けるトレードオフ**: バイトコードが実質Java 21相当のままになるため、Java 25
固有の言語機能・ランタイム最適化の一部(バイトコードレベルのもの)は#103完了まで
恩恵を受けられない。また`options.release=21`という一見冗長に見える設定が残るため、
事情を知らない開発者が誤って削除するリスクがあり、`CLAUDE.md`落とし穴#16と本ADRで
明文化した。

**撤回条件**: #103(Spring Boot 4移行)着手時にSpring Framework 7がJava25のクラス
ファイル形式を認識できると確認できた場合、この決定は不要になる。その際は本ADRの
Statusを`Superseded by`で該当ADRを指す形に変更し、`options.release=21`と
`CLAUDE.md`落とし穴#16を削除する。
