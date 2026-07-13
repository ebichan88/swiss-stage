# データベース設計書(DynamoDB)

## 1. 設計方針

- **シングルテーブル設計**を採用する(テーブル名: `swiss-stage`)
- キャパシティモード: **オンデマンド**(大会時のみスパイクする負荷特性のため)
- アクセスパターンを先に定義し、それを満たすキー設計を行う(RDB的な正規化をしない)
- GSIは最大3個まで

---

## 2. アクセスパターン一覧

| # | アクセスパターン | 使用キー |
|---|----------------|---------|
| AP1 | 大会IDで大会情報を取得 | PK=`TOURNAMENT#{id}`, SK=`METADATA` |
| AP2 | 大会の参加者一覧を取得 | PK=`TOURNAMENT#{id}`, SK begins_with `PARTICIPANT#` |
| AP3 | 大会のラウンド・対局一覧を取得 | PK=`TOURNAMENT#{id}`, SK begins_with `ROUND#` |
| AP4 | 特定ラウンドの対局一覧を取得 | PK=`TOURNAMENT#{id}`, SK begins_with `ROUND#{n}#MATCH#` |
| AP5 | 運営者の大会一覧を取得(新しい順) | GSI1: PK=`USER#{sub}`, SK=`TOURNAMENT#{createdAt}` |
| AP6 | 共有トークンから大会を特定 | GSI2: PK=`SHARE#{token}` |
| AP7 | 大会の全データ一括取得(順位計算・共有ページ用) | PK=`TOURNAMENT#{id}` を Query(全SK) |

---

## 3. アイテム設計

### Tournament(大会メタデータ)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | ULID推奨(時系列ソート可能) |
| SK | `METADATA` | |
| entityType | `TOURNAMENT` | |
| name | `第30回実業団囲碁大会` | |
| gameType | `GO` / `SHOGI` | |
| totalRounds | `5` | |
| status | `PREPARING` / `IN_PROGRESS` / `FINISHED` | |
| visibility | `PRIVATE` / `TOKEN` / `PUBLIC` | 公開範囲 |
| shareToken | `a1b2c3...` | GSI2PK に `SHARE#{token}` を設定 |
| resultInputEnabled | `true` / `false` | 共有トークン経由の結果入力を許可するか(既定false。属性なし=false) |
| ownerSub | `google-oauth2|1234` | GSI1PK に `USER#{sub}` を設定 |
| GSI1PK / GSI1SK | `USER#{sub}` / `TOURNAMENT#{createdAt}` | AP5用 |
| GSI2PK | `SHARE#{token}` | AP6用(トークン未発行時は属性なし) |
| createdAt / updatedAt | ISO8601(UTC) | MVPでは METADATA アイテムのみ保持(子アイテムのタイムスタンプはDTOで不要なため) |
| version | number | 楽観ロック(`@DynamoDbVersionAttribute`)。初回保存で1が払い出される |

### Participant(参加者)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `PARTICIPANT#01J...` | |
| entityType | `PARTICIPANT` | |
| name | `蛯名 隆` | 必須 |
| organization | `〇〇株式会社` | 任意 |
| rank | `DAN_3` | 任意(棋力enum。`07_type_definitions.md` の `Rank` 参照) |
| seedOrder | `1` | 初回マッチングのシード順 |
| status | `ACTIVE` / `WITHDRAWN` | 途中棄権対応 |

### Match(対局)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `ROUND#03#MATCH#01J...` | ラウンド番号はゼロ埋め2桁(ソート用) |
| entityType | `MATCH` | |
| roundNumber | `3` | |
| tableNumber | `12` | 卓番号 |
| player1Id / player2Id | ParticipantId | player2Id=null なら不戦勝 |
| result | `PLAYER1_WIN` / `PLAYER2_WIN` / `DRAW` / `BOTH_LOSE` / `BYE` / `NONE` | NONE=未入力 |
| version | number | 楽観ロック(結果入力の競合検出) |
| resultInputBy | `OWNER` / `SHARE_TOKEN` | 監査用。結果を入力した主体(未入力・BYEは属性なし) |

### Round(ラウンド状態)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `ROUND#03` | |
| status | `PAIRING` / `PLAYING` / `CONFIRMED` | 確定後は対局結果を変更不可(運営者の明示操作を除く) |

---

## 4. 設計上の決定事項

1. **順位(Standing)は保存しない**: 対局結果から都度計算する(domain層の `StandingCalculator`)。
   保存すると結果修正時に不整合が生じるため。ただし全ラウンド確定時のスナップショットは `STANDING#FINAL` として保存してよい(過去大会の高速表示用)。
2. **ID は ULID**: UUIDv4ではなくULIDを使用(SKソートで作成順が保てる)。
3. **削除はソフトデリートしない**: 主催者による大会削除は物理削除(個人情報保護方針に基づく)。
   ただし削除前に確認ダイアログ+大会名の入力確認を必須とする。
4. **楽観ロック**: `version` 属性 + 条件付き書き込みで更新競合を防ぐ(特に結果入力・ラウンド確定)。
5. **トランザクション**: ラウンド確定+次ラウンド生成は `TransactWriteItems` で原子的に行う(上限100アイテムに注意 → 大規模大会ではラウンド確定とマッチ生成を分離し、Roundのstatusで整合性を担保)。

---

## 5. ローカル開発

- **DynamoDB Local** を Docker で起動(`08_development_setup.md` 参照)
- テーブル作成スクリプトは `backend/scripts/create-table.sh` に配置(GSI含めコード管理)
- テストでの利用方法は `.claude/03_library_docs/03_dynamodb_local_testing.md` を参照
