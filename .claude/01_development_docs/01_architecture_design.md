# アーキテクチャ設計書

## 1. 全体構成

Swiss Stage は **フロントエンド(SPA) + バックエンド(REST API)** の分離構成を採用する。

```
[ブラウザ(スマホ/PC)]
      │ HTTPS
      ▼
[Route53] → [ALB(SSL終端)] → [EC2: Spring Boot + 静的ファイル配信]
                                   │
                                   ▼
                              [DynamoDB]
                                   │
                                   ▼
                        [CloudWatch Logs/Alarms]
```

- フロントエンドは Vite でビルドした静的ファイルを Spring Boot から配信する(MVPではCDN不使用)
- API は `/api/v1/**` パスで提供する

---

## 2. なぜこの構成なのか

| 決定 | 理由 |
|------|------|
| SPA + REST API 分離 | 大会当日はスマホからの結果入力が主。SPAで軽快な操作感を実現 |
| Spring Boot (Java 21) | 型安全・堅牢性重視。マッチング/順位計算という「絶対に間違えられない」ロジックに向く |
| DynamoDB | 大会時のみトラフィックが集中するスパイク型負荷に従量課金が有利 |
| EC2単一構成 | 予算$17/月の制約。300名規模なら t3.micro で十分 |
| DDD + レイヤードアーキテクチャ | マッチング・順位計算というドメインロジックが複雑。ドメイン層を独立させテスト容易性を確保 |

---

## 3. バックエンド: DDDレイヤー構造(厳守)

```
backend/src/main/java/com/swiss_stage/
├── presentation/          # プレゼンテーション層
│   ├── controller/        # REST APIコントローラー
│   └── filter/            # 認証フィルター等
├── application/           # アプリケーション層
│   ├── service/           # ユースケース実装
│   └── dto/               # リクエスト/レスポンスDTO
├── domain/                # ドメイン層(最重要・外部依存ゼロ)
│   ├── model/             # エンティティ・値オブジェクト
│   ├── repository/        # リポジトリインターフェース
│   └── service/           # ドメインサービス(マッチング・順位計算)
└── infrastructure/        # インフラ層
    ├── repository/        # DynamoDB実装
    └── config/            # AWS設定・Spring設定
```

### 各層の責任

| 層 | 責任 | 依存してよい層 | 禁止事項 |
|----|------|--------------|---------|
| presentation | HTTPリクエスト/レスポンス変換、認証チェック | application | ビジネスロジックを書かない |
| application | ユースケースのオーケストレーション、トランザクション境界 | domain | ドメインルールを書かない |
| domain | ビジネスルール(マッチング、順位計算、大会状態遷移) | なし | Spring/AWS SDKへの依存禁止 |
| infrastructure | DynamoDBアクセス、外部サービス連携 | domain(インターフェース実装) | ビジネスロジックを書かない |

### ロジックの置き場所の判断基準

- **「大会」「対局」「順位」のルールに関するもの** → domain層
  - 例: スイス方式の組み合わせ、SOS計算、不戦勝の扱い、大会ステータス遷移
- **「操作の流れ」に関するもの** → application層
  - 例: 「ラウンドを確定し、次ラウンドのマッチングを生成し、保存する」
- **「見せ方・受け取り方」に関するもの** → presentation層
  - 例: バリデーションアノテーション、DTOへの変換、HTTPステータス決定
- **「保存の仕方」に関するもの** → infrastructure層
  - 例: シングルテーブルのキー構築、DynamoDBクエリ

---

## 4. ドメインモデル(コアエンティティ)

| エンティティ | 説明 | 主な属性 |
|------------|------|---------|
| Tournament | 大会 | 名前、競技(囲碁/将棋)、ラウンド数、ステータス、公開範囲 |
| Participant | 参加者 | 氏名、所属、段級位、参加ステータス |
| Round | ラウンド | ラウンド番号、ステータス(組み合わせ中/対局中/確定) |
| Match | 対局 | 対戦者ペア、結果(勝敗/不戦勝/引き分け)、卓番号 |
| Standing | 順位(計算結果) | 勝数、SOS、SOSOS、順位 |

### 値オブジェクト

- `TournamentId`, `ParticipantId` (UUID)
- `GameType` (GO / SHOGI)
- `MatchResult` (BLACK_WIN / WHITE_WIN / DRAW / BOTH_LOSE / BYE)
- `TournamentStatus` (PREPARING / IN_PROGRESS / FINISHED)
- `ShareToken` (参照URL用トークン)

### 状態遷移(Tournament)

```
PREPARING(準備中) ──開始──▶ IN_PROGRESS(開催中) ──終了──▶ FINISHED(終了)
     │                          │
     └── 参加者編集可           └── 参加者は欠席化のみ可、ラウンド進行可
```

---

## 5. フロントエンド構造

```
frontend/src/
├── components/      # 再利用可能UIコンポーネント
├── pages/           # 画面単位コンポーネント(React Routerと1:1)
├── services/        # API通信ロジック(fetchラッパー)
├── hooks/           # カスタムHooks(useTournament等)
├── types/           # TypeScript型定義(APIレスポンス型等)
└── utils/           # 汎用ユーティリティ
```

- ビジネスロジックはバックエンドに置く。**フロントで順位計算・マッチングをしない**(表示専用)
- 詳細は `10_frontend_design.md` を参照

---

## 6. 主要な設計パターン

1. **リポジトリパターン**: domain層にインターフェース、infrastructure層にDynamoDB実装
2. **ユースケースパターン**: applicationサービスは1ユースケース1公開メソッドを基本とする
3. **値オブジェクト**: ID・結果・ステータスは必ず値オブジェクト/enumにする(String直渡し禁止)
4. **ドメインサービス**: マッチング(`SwissPairingService`)・順位計算(`StandingCalculator`)は純粋関数的に実装し、単体テストを容易にする
