# 型・スキーマ運用設計書

## 1. 基本方針

- **API境界のDTO・enumの定義は `schema/openapi.yaml`(リポジトリルート)が唯一の正**。本ドキュメントには個別の型定義を書かない(二重管理しない)
- バックエンド(Java record)は contractテストの OpenAPI 検証(`ApiContractTestSupport#performApi`)でスキーマとの一致が機械検査される
- フロントエンド(TypeScript)は `npm run generate:api` で `frontend/src/types/generated/api.d.ts` を生成し、`frontend/src/types/*.ts` が生成型をre-exportする。生成物の鮮度はCIで検査される
- TypeScriptでは `any` 禁止。`unknown` + 型ガードを使う
- Javaでは DTO に record を使用。ドメインモデルとDTOを混同しない(§3)

### 型を変更する手順

1. `schema/openapi.yaml` を先に更新する(仕様変更は先にスキーマ、が原則。`schema/` はAI Fixerの聖域で人間が判断する)
2. バックエンドのDTO・実装を追随させる(`./gradlew check` のcontractテストが一致を検証)
3. `cd frontend && npm run generate:api` で型を再生成する(`npm run check` で追随漏れを検出)

## 2. 命名・変換規約

| 項目 | 規約 |
|------|------|
| JSONプロパティ | camelCase(Java側もJacksonデフォルトのまま) |
| 日時 | ISO8601文字列(タイムゾーン付き)。フロントでは文字列のまま保持し、表示時のみ整形 |
| ID | 文字列(ULID)。数値IDは使わない |
| null許容 | 「値がない」は null(undefined をAPIで返さない)。TypeScriptでは `| null` を明示 |
| 金額・勝点等の数値 | 勝点は number(0.5刻み)。それ以外の個数は integer |

enumの順序・優先度は宣言順(ordinal)に依存しない。明示的な数値フィールド(`sortOrder` 等)で比較する(例: `Rank`。フロントの並び順定数 `RANKS_STRONGEST_FIRST` は `satisfies` でスキーマとの整合を型検査している)。

## 3. ドメインモデルとDTOの分離(バックエンド)

- domain層のモデル(`Tournament`, `Participant` 等)はビジネスルールを持つクラス。**そのままJSONにしない**
- application層で `XxxDto.from(domainModel)` の静的ファクトリで変換する
- shareToken のように「見せる相手によって出し分ける」属性は DTO 変換時に制御する
  (共有トークンアクセス時のレスポンスには shareToken・ownerSub を含めない)
