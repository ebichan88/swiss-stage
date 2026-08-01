# 04. Java 25バイトコードrelease固定の解除(Spring Boot 4移行完了により撤回条件が成立)

- Status: Accepted
- Issue: #103
- Date: 2026-08-01

## 1. 文脈

`06_adr/03_java25_bytecode_release_pin.md`は、Spring Boot 3.4.1のクラスパススキャン
(ASMベースの`SimpleMetadataReader`)がJava 25のクラスファイル形式(major version 69)を
認識できないため、`backend/build.gradle`に`options.release = 21`を暫定固定する決定を
記録した。同ADRの撤回条件は「Spring Boot 4移行(#103)着手時にSpring Framework 7が
Java 25のクラスファイル形式を認識できると確認できた場合」であった。

Spring Boot 4系への移行(#103、`06_adr/02_spring_boot_4_upgrade.md`)の実装PRで、
`options.release`指定を完全に削除した状態(toolchain・release共にJava 25)で
`./gradlew check`を実行し、全206件のテスト(`@SpringBootTest`を含む)が
`ClassFormatException`なくグリーンになることを実測で確認した。Spring Framework 7の
クラスパススキャンはJava 25のクラスファイル形式を問題なく認識する。

## 2. 決定

`backend/build.gradle`から`tasks.withType(JavaCompile) { options.release = 21 }`を
削除する。コンパイル・テスト実行・生成バイトコードのすべてをJava 25(toolchain)に揃える。

`CLAUDE.md`の落とし穴#16(release固定を外さない旨の注意書き)を削除する。
`06_adr/03_java25_bytecode_release_pin.md`のStatusを、本ADRへの`Superseded by`に
更新する(内容自体は書き換えず、決定が過去のものであることを示す)。

## 3. 却下した案

- **releaseを21に固定したまま維持する案**: Spring Boot 4移行が完了しSpring Framework 7で
  Java 25クラスファイルが問題なく読めることを実測済みであるにもかかわらず制約を残すのは、
  `options.release=21`という一見冗長な設定と`CLAUDE.md`落とし穴#16という「理由が既に
  解消されたにもかかわらず残り続ける規約」を放置することになり、「シンプルさ」の原則に反する
  ため却下

## 4. 結果

**得られるもの**: Java 25(toolchain・release共に)でのビルドが実現し、Java 25固有の
バイトコードレベルの言語機能・ランタイム最適化を活用できる状態になった。
`options.release=21`という暫定措置と、それに伴う`CLAUDE.md`の注意書きを撤去でき、
ビルド設定がシンプルになった。

**引き受けるトレードオフ**: 特になし(暫定措置の解消のため)。

**撤回条件**: なし(本ADRは暫定措置の解消を記録するものであり、再度Java 25の
クラスファイル形式が読めなくなるような回帰が起きない限り撤回不要)。
