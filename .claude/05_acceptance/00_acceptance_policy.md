# 受け入れ基準の運用ルール(Acceptance Policy)

受け入れケース台帳(`01_acceptance_scope.md`)の書き方・育て方を定める。
QAエージェント(`.claude/agents/qa.md`)の判断基準はこのファイルにある(Reviewerと `.claude/04_quality/` の関係と同じ)。

## 1. 台帳の目的

- 「どの受け入れケースがあり、どこまで実装済みか」を1行1ケースで管理する
- 実装タスク・PRのスコープを受け入れケース単位で切れるようにする
- QAエージェントが「テストが受け入れ基準を守っているか」を機械的に突合できるようにする

**台帳は仕様書ではない。** 受け入れ基準の一文とID・状態のみを持ち、仕様の詳細は設計ドキュメント(`.claude/01_development_docs/`)を参照する。同じ内容を複数の場所に書かない(重複はAIにとってノイズになり、乖離事故の元になる)。

## 1.5 受け入れケースを作る/作らない基準

次のいずれにも当たらなければ、受け入れケースを作らない:

1. ユーザーが**できること**が増える・変わる
2. 業務上の**ルール**が変わる(順位計算・組み合わせ・権限・状態遷移)
3. 破られると**大会当日の運営が止まる・結果が狂う・情報が漏れる**

意匠変更・リファクタ・内部実装の置き換えのみで上記いずれにも当たらない場合は、
受け入れケースではなく回帰テスト(IDタグなし)で守る。バグ修正の場合の判定は
`04_development_process.md` §2.5 を参照。

## 2. コンポーネントとIDプレフィックス

コンポーネントの単位は**contractテストクラス**(= APIリソース境界)とする。

| Prefix | コンポーネント | 検証するテスト |
|--------|--------------|---------------|
| AUTH | 認証 | AuthApiTest |
| TRN | 大会 | TournamentApiTest |
| PTC | 参加者 | ParticipantApiTest |
| GRP | グループ | GroupApiTest |
| RND | ラウンド・対局・順位 | RoundApiTest |
| TEAM | 団体戦(チーム・メンバー・団体戦ラウンド/対局/順位・共有トークン経由の自己申告) | TeamApiTest, TeamRoundApiTest, TeamSharedApiTest |
| SHR | 共有(トークン) | SharedApiTest, SharedRateLimitApiTest |
| MBR | 大会の共同管理(共同管理者・招待リンク) | TournamentMemberApiTest, InvitationApiTest |
| SPA | SPA配信 | SpaFallbackApiTest |
| PRT | 帳票印刷(対局カード・対戦結果表・参加者名簿) | Vitest単体テスト(`frontend/tests/unit/components/print/`, `tests/unit/pages/`) |
| E2E | 一気通貫(クリティカルパス) | Playwright(`frontend/tests/e2e/`) |

API契約に変化がなく**UI表示のみ**を追加・変更するケース(例: 既存のレスポンスフィールドからフロントエンドが導出する表示・警告)は、
該当コンポーネントのPrefixのまま、検証テストをフロントエンドのVitest単体テスト(`frontend/tests/unit/`)にしてよい
(クリティカルパスでない限りE2Eを新設しない。E2Eは`12_e2e_test_design.md`のとおりクリティカルパスのみに限定する)。

**PRTの例外**: コンポーネントの単位は原則contractテストクラス(=APIリソース境界)だが、帳票印刷は既存APIのみで完結し
専用のcontractテストクラスを持たない。そのためPRTはVitest単体テストのみを検証先とする(上記のUI表示のみの規定と同じ扱い)。

## 3. ID体系

- 形式: `<PREFIX>-AC-<3桁連番>`(例: `TRN-AC-003`)
- 連番は**永久欠番**: ケースを廃止したら行を削除し、そのIDは再利用しない
- 新しいコンポーネント(テストクラス)を追加するときは、この表にPrefixを追記してから採番する

## 4. 優先度(P0 / P1 / P2)

`.claude/04_quality/02_severity.md` の判定フローを流用する(内容を再記述しない)。
「この受け入れ基準が破られたら」と読み替えて判定する。

- **P0** = 破られると大会当日に運営が止まる・結果が狂う・情報が漏れる(≒Critical。再戦禁止・BYE・順位計算・認可・楽観ロック・shareToken非漏洩)
- **P1** = 仕様違反・ユーザー影響のあるバグになる(≒Major)
- **P2** = それ以外(利便性・一貫性。≒Minor)

## 5. Status

| Status | 意味 |
|--------|------|
| todo | ケースは定義済み、実装・テスト未着手 |
| in_progress | 実装中(PRが開いている) |
| done | 受け入れテストが存在しCIで通っている |

## 6. テストとの紐づけ

- contractテスト: `@DisplayName` の**先頭**にIDを付ける。複数ケースを1メソッドで検証する場合はカンマ区切り

  ```java
  @DisplayName("TRN-AC-003,TRN-AC-004: 一覧・詳細が取得でき、他人の大会は404になる(存在を漏らさない)")
  ```

- Playwright E2E: `test('E2E-AC-001: ...')` のタイトルプレフィックス
- フロントエンドVitest単体テスト(§2の「UI表示のみ」のケース): `it('SHR-AC-015: ...')` のようにテストタイトルの先頭にIDを付ける(複数ケースを1つのテストで検証する場合はカンマ区切り。contractテストの`@DisplayName`と同じ規約)
- IDは `grep -rhoE '[A-Z0-9]+-AC-[0-9]+' backend/src/test/java/com/swiss_stage/contract/ frontend/tests/e2e/ frontend/tests/unit/` で抽出できる形を保つ(QAエージェントはこのgrepで台帳と双方向突合する。プレフィックスは `E2E` のように数字を含みうる)
- 1メソッドに詰め込みすぎない。テストを分割したら台帳の検証列は変えず、IDを付け替える

## 7. 運用フロー

1. **新機能・挙動変更は、実装前に台帳へケースを追加する(Status=todo)**。仕様変更を伴う場合は先に設計ドキュメントを直す(CLAUDE.mdの既存ルールどおり)
2. 実装PRで受け入れテストを書き、**同じPRで** Statusをdoneに更新し検証列を記入する(「実装とドキュメントの乖離は同じPRで直す」ルールの適用)
3. ケース自体の追加・変更・廃止(=仕様の決定)は**人間が判断する**。AIエージェント(特にFixer)は指摘を閉じる目的で台帳を書き換えてはならない
4. QAエージェントの誤検知・見逃しに気づいたら、本ファイルまたは台帳を同じPRで直して育てる(`01_review_checklist.md` と同じ運用)

## 7.5 QAの close 分類とqa-fixerの自動修正範囲

QAエージェント(`.claude/agents/qa.md`)の各指摘には `close:`(`ledger-side` / `test-side` /
`human-only`)を必須で付ける。**qa-fixer(`.claude/agents/qa-fixer.md`)が自動修正してよいのは
`close: test-side` の指摘のみ**(既存テストは該当ケースを実質的に検証済みで、`@DisplayName`
やテストタイトルにIDタグが付いていないだけの場合)。

台帳自体の書き換え(`ledger-side`: 新ケース追加・Status更新)や、基準hack検出・不足提案
(常に `human-only`)は**qa-fixerも触らない**。上記§7-3「AIエージェントは指摘を閉じる目的で
台帳を書き換えてはならない」の原則は、qa-fixer導入後も変わらず適用される
(`.github/workflows/ai-qa.yml` のゲートが `close: test-side` 以外を含む場合はqa-fixerを
起動せず `needs-human` にする)。

## 8. やらないこと(out-of-scope)の扱い

- 台帳冒頭の「やらないこと」欄に載っているものは、QAエージェントは不足ケースとして**提案しない**
- 出典は `.claude/00_project/02_inception_deck.md` §3 と各設計ドキュメント。台帳には一行要約+参照のみを書く
