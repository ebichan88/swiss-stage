# DynamoDB Local を使ったテスト方法

> 統合テスト(infrastructure層)での DynamoDB Local の使い方と落とし穴。

---

## 1. 方針

- リポジトリ実装のテストは**モックではなく DynamoDB Local に対して行う**
  (条件付き書き込み・GSI・トランザクションの挙動はモックで再現できないため)
- domain/application 層のテストでは DynamoDB を使わない(リポジトリインターフェースをMockitoでモック)

---

## 2. セットアップ(2通り)

### ローカル開発: docker compose(常駐)

`08_development_setup.md` の compose を使用。テストは `application-test.yml` で `http://localhost:8000` を向く。

### CI: GitHub Actions の service container

`11_cicd_design.md` 参照。テーブル作成は `create-table.sh` をテスト前に実行。

### (代替)Testcontainers

環境差異をなくしたい場合は Testcontainers を使ってもよい:

```java
@Testcontainers
class TournamentRepositoryTest {
    @Container
    static GenericContainer<?> dynamodb =
        new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(8000)
            .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.dynamodb.endpoint",
              () -> "http://localhost:" + dynamodb.getMappedPort(8000));
    }
}
```

- テスト用途は `-inMemory` でよい(高速・毎回クリーン)
- コンテナ起動は静的フィールド(クラス間で再利用)にして起動コストを抑える

---

## 3. テストの書き方ルール

```java
@SpringBootTest
@ActiveProfiles("test")
class DynamoDbTournamentRepositoryTest {

    @Autowired TournamentRepository repository;

    @Test
    void 大会を保存して取得できる() {
        // テストごとに一意のIDを使う(テーブルを共有しても衝突しない)
        var tournament = TournamentFixture.create();
        repository.save(tournament);

        var found = repository.findById(tournament.getId());
        assertThat(found).hasValue(tournament);
    }
}
```

1. **テーブルを消さない・作り直さない**: テストごとに一意のULIDを使って分離する(高速)
2. **テーブル作成はテスト初期化で冪等に**: `ResourceInUseException` は無視する `ensureTable()` ヘルパーを用意
3. **GSIの結果整合に依存しない**: GSI経由の検証はリトライ付きにするか、本体キーのQueryで検証する
4. **時刻は Clock をDI**: `Instant.now()` 直呼び禁止。テストで固定Clockを注入

---

## 4. ハマりポイント集

| 症状 | 原因と対策 |
|------|----------|
| ローカルでは通るがCIで `ResourceNotFoundException` | CIでテーブル作成スクリプトが走っていない → workflowの `create-table.sh` ステップ確認 |
| テーブルはあるのに見つからない | 認証情報・regionが起動時と異なると別DB扱い(`-sharedDb` なしの場合)→ `-sharedDb` を付ける & テストの認証情報を固定値に統一 |
| テストが順序依存で落ちる | ID共有が原因。フィクスチャで毎回新規ULIDを生成する |
| `TransactionCanceledException` の原因が分からない | `cancellationReasons()` をログ出力するテストヘルパーを使う(どの条件が失敗したか出る) |
| DynamoDB Localが古い挙動 | イメージを `latest` に更新。特にトランザクション系は古いバージョンでバグあり |
| ポートは応答するが全操作がタイムアウト | `-dbPath` のボリュームがroot所有でSQLiteが書けずクラッシュループ(`docker logs` に `unable to open database file`)→ composeに `user: root` を設定するか `-inMemory` にする |

---

## 5. 何をどの層でテストするか(再掲)

| テスト対象 | DynamoDB |
|-----------|----------|
| SwissPairingService / StandingCalculator(domain) | 使わない(純粋関数) |
| ユースケース(application) | 使わない(リポジトリをモック) |
| リポジトリ実装(infrastructure) | **DynamoDB Local** |
| APIコントラクト(presentation) | リポジトリをモック(MockMvc) |
| E2E | DynamoDB Local(実アプリ起動) |
