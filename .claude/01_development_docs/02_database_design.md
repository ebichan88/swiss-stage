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
| AP8 | 大会のグループ一覧を取得 | PK=`TOURNAMENT#{id}`, SK begins_with `GROUP#` |
| AP9 | 大会のチーム一覧を取得(団体戦) | PK=`TOURNAMENT#{id}`, SK begins_with `TEAM#` |
| AP10 | 特定ラウンドのチーム対局一覧を取得(団体戦) | PK=`TOURNAMENT#{id}`, SK begins_with `ROUND#{n}#TEAM_MATCH#` |

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
| competitionType | `INDIVIDUAL` / `TEAM` | 作成後変更不可。既定は `INDIVIDUAL` |
| teamSize | `3` / `5` / 属性なし | `competitionType=TEAM` の時のみ必須。`INDIVIDUAL` では属性を持たない |
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
| rank | `DAN_3` | 任意(棋力enum。定義は `schema/openapi.yaml` の `Rank`) |
| entryOrder | `1` | 初回マッチングのエントリー順(参加者追加時に自動採番) |
| status | `ACTIVE` / `WITHDRAWN` | 途中棄権対応 |
| groupId | `01J...`(GroupのULID) | **必須**。常にいずれかのグループに帰属する(未割当状態は存在しない。`05_swiss_pairing_algorithm.md` §2.4) |

### Group(棋力帯グループ)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `GROUP#01J...` | ULID(作成順ソート=自動振り分けの割当順) |
| entityType | `GROUP` | |
| name | `A` | 必須。50文字以内。大会内で重複不可 |

- グループは常に1つ以上・1大会あたり最大10個。大会作成時にデフォルトグループ「A」を自動作成し、最後の1グループは削除できない。作成・改名・削除は PREPARING 中のみ
- version は持たない(PREPARING 中のみ編集・単一運営者前提。Participant と同格の扱い)
- 大会削除はパーティション全Query→BatchWriteのため、GROUPアイテムも自動で削除される

### Match(対局)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `ROUND#03#MATCH#01J...` | ラウンド番号はゼロ埋め2桁(ソート用) |
| entityType | `MATCH` | |
| roundNumber | `3` | |
| tableNumber | `12` | 卓番号。グループ大会ではグループ内で1始まり(表示は「A-1」形式) |
| groupId | `01J...` | **必須**。対局は常にいずれかのグループに帰属する。取得時のグループ絞り込みはアプリ側フィルタ(グループ≦10・参加者≦300のため十分) |
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

### Team(団体戦チーム)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `TEAM#01J...` | |
| entityType | `TEAM` | |
| name | `〇〇株式会社Aチーム` | 必須。50文字以内。大会内で重複可 |
| entryOrder | `1` | 初回マッチングのエントリー順(チーム追加時に自動採番。棋力シードは行わないため実質この順のみでペアリングする) |
| status | `ACTIVE` / `WITHDRAWN` | 途中棄権対応(個人戦と同じ扱い) |
| groupId | `01J...`(GroupのULID) | **必須**。個人戦と同じGroup機能をチーム単位で流用する |
| members | `[{id, name, rank, boardPosition}, ...]` | 埋め込みリスト属性(最大 teamSize+補欠上限 のため別アイテム化しない)。`boardPosition` は 1..teamSize(必須ポジション)または null(補欠) |

- `competitionType=TEAM` の大会でのみ存在する。`competitionType=INDIVIDUAL` の大会では `Participant`/`Group` 側のみを使う

### TeamMatch(団体戦対局)

| 属性 | 例 | 備考 |
|------|----|------|
| PK | `TOURNAMENT#01J...` | |
| SK | `ROUND#03#TEAM_MATCH#01J...` | ラウンド番号はゼロ埋め2桁(Matchと同じ規約) |
| entityType | `TEAM_MATCH` | |
| roundNumber | `3` | |
| tableNumber | `12` | 卓番号。グループ内1始まり(Matchと同じ規約) |
| groupId | `01J...` | **必須** |
| team1Id / team2Id | TeamId | team2Id=null なら不戦勝(Matchと同じ表現) |
| boardResults | `[{boardPosition, result, team1ReportedResult, team2ReportedResult}, ...]` | 埋め込みリスト属性(長さ=teamSize。BYEの場合は空)。`result` は既存の `MatchResult`(NONE/PLAYER1_WIN/PLAYER2_WIN/DRAW/BOTH_LOSE)を再利用 |
| version | number | 楽観ロック。ボード配列をまとめて1回の更新で書き換えるため、TeamMatch単位でversionを持つ(ボードごとのversionは持たない) |
| resultInputBy | `OWNER` / `SHARE_TOKEN` | Matchと同じ監査用属性 |

- チーム全体の勝敗(○/●/△相当)は保存せず、`boardResults` から都度点数集計して導出する(`05_swiss_pairing_algorithm.md` §5.3)

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
