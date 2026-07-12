# 型定義設計書

## 1. 基本方針

- フロントエンド(TypeScript)とバックエンド(Java)で**同じ概念には同じ名前**を使う
- API境界のDTOは本ドキュメントを唯一の正とし、双方を必ず同期させる
- TypeScriptでは `any` 禁止。`unknown` + 型ガードを使う
- Javaでは DTO に record を使用。ドメインモデルとDTOを混同しない

---

## 2. 共通enum(文字列リテラルで統一)

| 名前 | 値 | 説明 |
|------|----|------|
| `GameType` | `GO` \| `SHOGI` | 競技 |
| `TournamentStatus` | `PREPARING` \| `IN_PROGRESS` \| `FINISHED` | 大会状態 |
| `Visibility` | `PRIVATE` \| `TOKEN` \| `PUBLIC` | 公開範囲 |
| `ParticipantStatus` | `ACTIVE` \| `WITHDRAWN` | 参加状態 |
| `RoundStatus` | `PAIRING` \| `PLAYING` \| `CONFIRMED` | ラウンド状態 |
| `Rank` | `KYU_20` … `KYU_1` \| `DAN_1` … `DAN_9` | 棋力(段級位)。29段階。弱→強: 20級 < … < 1級 < 初段(`DAN_1`)< … < 9段。順序は `sortOrder`(段=負、級=正、小さいほど強い)で判定。表示名は `KYU_n`=「n級」、`DAN_1`=「初段」、`DAN_2`〜`DAN_9`=「2段」〜「9段」 |
| `MatchResult` | `NONE` \| `PLAYER1_WIN` \| `PLAYER2_WIN` \| `DRAW` \| `BOTH_LOSE` \| `BYE` | 対局結果 |

- Java: enum クラス、JSON化は名前そのまま(`@JsonValue` 不使用)
- TypeScript: union型 + `as const` オブジェクトで定義(`types/enums.ts`)

---

## 3. 主要DTO定義

### Tournament

```typescript
// frontend/src/types/tournament.ts
interface Tournament {
  id: string;                    // ULID
  name: string;
  gameType: GameType;
  totalRounds: number;
  currentRound: number;          // 0 = 未開始
  status: TournamentStatus;
  visibility: Visibility;
  shareToken: string | null;     // 運営者にのみ返す
  version: number;               // 楽観ロック用
  createdAt: string;             // ISO8601
  updatedAt: string;
}
```

```java
// backend: application/dto/TournamentDto.java
public record TournamentDto(
    String id, String name, GameType gameType,
    int totalRounds, int currentRound,
    TournamentStatus status, Visibility visibility,
    String shareToken, long version,
    String createdAt, String updatedAt) {}
```

### Participant

```typescript
interface Participant {
  id: string;
  name: string;
  organization: string | null;
  rank: Rank | null;             // 棋力(段級位enum。null = 未入力)
  seedOrder: number;
  status: ParticipantStatus;
}
```

### Match / Round

```typescript
interface Match {
  id: string;
  roundNumber: number;
  tableNumber: number;
  player1: ParticipantSummary;          // { id, name, organization }
  player2: ParticipantSummary | null;   // null = BYE
  result: MatchResult;
  version: number;
}

interface Round {
  roundNumber: number;
  status: RoundStatus;
  matches: Match[];
}
```

### Standing(順位)

```typescript
interface Standing {
  rank: number;                  // 同順位あり(1,2,2,4 形式)
  participant: ParticipantSummary;
  wins: number;                  // 勝点(0.5刻みあり得るため number)
  losses: number;
  sos: number;
  sosos: number;
  hadBye: boolean;
}
```

### APIレスポンスラッパー

```typescript
type ApiResponse<T> =
  | { success: true; data: T; meta: { timestamp: string } }
  | { success: false; error: ApiErrorBody };

interface ApiErrorBody {
  code: string;                  // ErrorCode(06_error_handling_design.md)
  message: string;
  details?: { field: string; reason: string }[];
}
```

---

## 4. 命名・変換規約

| 項目 | 規約 |
|------|------|
| JSONプロパティ | camelCase(Java側もJacksonデフォルトのまま) |
| 日時 | ISO8601文字列(タイムゾーン付き)。フロントでは文字列のまま保持し、表示時のみ整形 |
| ID | 文字列(ULID)。数値IDは使わない |
| null許容 | 「値がない」は null(undefined をAPIで返さない)。TypeScriptでは `| null` を明示 |
| 金額・勝点等の数値 | 勝点は number(0.5刻み)。それ以外の個数は integer |

---

## 5. ドメインモデルとDTOの分離(バックエンド)

- domain層のモデル(`Tournament`, `Participant` 等)はビジネスルールを持つクラス。**そのままJSONにしない**
- application層で `XxxDto.from(domainModel)` の静的ファクトリで変換する
- shareToken のように「見せる相手によって出し分ける」属性は DTO 変換時に制御する
  (共有トークンアクセス時のレスポンスには shareToken・ownerSub を含めない)
