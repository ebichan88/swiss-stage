# DynamoDB × Spring Boot 3 実装パターン(AWS SDK v2 Enhanced Client)

> ⚠️ **重要な技術決定**
> 要求定義書には `spring-data-dynamodb`(io.github.boostchicken:5.2.5)が記載されているが、
> このライブラリは **Spring Boot 3.x / Spring Data 3.x に非対応**(javax→jakarta移行前で更新停止)。
> 本プロジェクトでは **AWS SDK for Java v2 の DynamoDB Enhanced Client** を採用する。
> シングルテーブル設計(`02_database_design.md`)にはむしろ素のSDKの方が相性が良い。

---

## 1. 依存関係

```gradle
dependencies {
    implementation platform('software.amazon.awssdk:bom:2.25.+')
    implementation 'software.amazon.awssdk:dynamodb-enhanced'
}
```

---

## 2. クライアント設定(infrastructure/config)

```java
@Configuration
public class DynamoDbConfig {

    @Value("${app.dynamodb.endpoint:}")   // localのみ設定。本番は空
    private String endpoint;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.AP_NORTHEAST_1);
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   // DynamoDB Localはダミー認証情報でOK(ただし必ず固定値にする: -sharedDb参照)
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();  // 本番はEC2インスタンスロールから自動取得
    }

    @Bean
    public DynamoDbEnhancedClient enhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}
```

---

## 3. シングルテーブル用アイテムクラス

シングルテーブル設計のため、エンティティごとにBeanを分けつつ同一テーブルにマッピングする:

```java
@DynamoDbBean
public class TournamentItem {
    private String pk;          // TOURNAMENT#{id}
    private String sk;          // METADATA
    private String entityType;  // TOURNAMENT
    private String name;
    private String gameType;
    private Long version;
    // ...

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI1")
    @DynamoDbAttribute("GSI1PK")
    public String getGsi1Pk() { ... }

    @DynamoDbVersionAttribute   // 楽観ロック(自動インクリメント+条件付き書き込み)
    public Long getVersion() { return version; }
}
```

### 重要な注意点

- `@DynamoDbBean` は **getterにアノテーション**を付ける(fieldではない)。Lombokの `@Getter` と併用時は `onMethod_` が必要になるため、アイテムクラスはLombokなしの素のBeanで書くのが安全
- domain層のモデルとアイテムクラスは別物。infrastructure層で相互変換する(`TournamentItemMapper`)
- 異種エンティティの混在Queryは Enhanced Client が苦手 → **PK全件取得(AP7)は素の `DynamoDbClient.query()` で取得し、`entityType` を見て各Itemクラスへ振り分ける**ヘルパーを作る

---

## 4. よく使う操作パターン

```java
// Query: 大会の参加者一覧(SK前方一致)
QueryConditional.sortBeginsWith(
    Key.builder().partitionValue("TOURNAMENT#" + id).sortValue("PARTICIPANT#").build());

// 条件付き書き込み: 二重生成防止(存在しない場合のみPut)
PutItemEnhancedRequest.builder(RoundItem.class)
    .item(round)
    .conditionExpression(Expression.builder()
        .expression("attribute_not_exists(PK)").build())
    .build();
// → 失敗時 ConditionalCheckFailedException を捕捉して 409 に変換

// トランザクション(ラウンド確定+マッチ一括作成)
enhancedClient.transactWriteItems(b -> b
    .addUpdateItem(roundTable, confirmedRound)
    .addPutItem(matchTable, match1) /* ... */);
// → 上限100アイテム。超える場合はラウンドstatusで整合性担保(02_database_design.md)
```

---

## 5. ハマりポイント集

| 症状 | 原因と対策 |
|------|----------|
| `ResourceNotFoundException`(ローカル) | DynamoDB Localのテーブル未作成 or `-sharedDb` なしで認証情報ごとにDBが分かれている → `create-table.sh` 再実行 + `-sharedDb` 確認 |
| 楽観ロックが効かない | `@DynamoDbVersionAttribute` は Enhanced Client の `updateItem/putItem` 経由でのみ機能。素のclientで書くと素通りする |
| GSIへの反映が見えない | GSIは結果整合。書き込み直後のGSI Queryはテストで不安定 → テストは本体PKのQueryで検証する |
| 数値の精度 | 勝点0.5は `BigDecimal` ではなく文字列 or 2倍整数で保存を検討 → **本プロジェクトは勝点を2倍整数(勝=2, 分=1)で保存し、表示時に割る** |
| null属性 | Enhanced Clientはnull属性を「削除」として扱う(updateItem時)。部分更新は `ignoreNulls(true)` を明示 |
