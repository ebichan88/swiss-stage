# 14. 大会の共同管理(招待リンクによる共同管理者)

- Status: in_progress
- Issue: #185
- PR: #198(PR1: entryOrder採番カウンタ)、#199(PR1b: ラウンド確定と結果編集の競合対策)

## 1. 背景・目的

現在は大会を作成した運営者1人だけが管理画面を操作できる(`Tournament.ownerSub` と JWT の `sub` の
一致を `TournamentAccessSupport.loadOwned()` で検証する)。実際の大会運営は複数人のスタッフで分担する
ことが多く、特に当日の結果入力・ラウンド確定を1人が抱えるのは現実的でない。

共有トークン(`/s/{token}`)は参加者向けの閲覧と結果自己申告に閉じており、運営スタッフに渡すには
権限が足りない。Googleアカウントそのものを共有するのは論外である。

完了時に得られる状態:

- OWNER が設定画面から招待リンクを発行し、任意の手段で他の運営者に渡せる
- 招待リンクを受け取った人がGoogleログインして承諾すると MAINTAINER になる
- MAINTAINER は大会一覧から当該大会に入り、参加者管理・ラウンド進行・結果入力・順位表・帳票印刷を
  OWNER と同等に操作できる。大会設定(S09)にはアクセスできない
- 複数人が同時に参加者を追加しても `entryOrder` が重複しない

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度。** 本計画で最も守るべきは
「権限のない人が大会を操作できない」ことと「同時操作でデータが黙って壊れない」ことである。

### 決定済みの方向性(Issue #185 で合意済み)

| 論点 | 決定 |
|---|---|
| ユーザーの指定方法 | 招待リンク方式。ユーザーディレクトリを作らず、メールアドレスも保存しない |
| 招待リンクの寿命 | 共通リンク1本 + 人数枠 + 有効期限72時間。枠切れ・期限切れ・失効操作のいずれかで無効 |
| 権限モデル | OWNER / MAINTAINER の2段。role は enum で保持し、将来の VIEWER 追加を非破壊にする |
| 設定画面(S09) | 丸ごと OWNER 専用(共有URLの再発行も MAINTAINER には開放しない) |
| 参照専用ロール | スコープ外(共有リンクで大会状況は共有できる) |
| 組織(Organization) | スコープ外。将来は「前回の大会から共同管理者をコピー」で代替する |
| 大会一覧の見せ方 | 所有大会と混在させ、カードに「共同管理」バッジを出す |
| `entryOrder` の同時採番 | 大会メタデータにカウンタを持ち、既存の `version` 楽観ロックで採番する |

却下した案とその理由は `13_tournament_collaboration_model.md`(ADR)に記録する。

## 2. 画面シナリオ

### 招待する側(OWNER・PC)

- OWNER が 大会設定画面で 人数枠(1〜(9−現在の共同管理者数)人。上限は現在の共同管理者数に応じて動的に変わる。§3参照)を選んで「招待リンクを発行」すると 招待リンクが表示され、コピーできる
- OWNER が 大会設定画面で 招待リンクの「無効にする」を押すと 確認ダイアログのあとリンクが失効し、以後そのリンクでは承諾できない
- OWNER が 大会設定画面で 招待リンクを再発行すると 旧リンクは即時無効になり、使用済みの枠がリセットされる
- OWNER が 大会設定画面で 共同管理者一覧の取り消しを押すと 確認ダイアログのあとその人は以後この大会にアクセスできなくなり、発行中の招待リンクがあれば同時に失効する
- OWNER が 共同管理者が9人いる状態で 招待リンクを発行しようとすると 発行ボタンが無効で、上限に達している旨が表示される
- OWNER が 招待リンクをまだ発行していない状態で 設定画面を開くと 共同管理者は0人で、発行フォームだけが表示される

### 招待される側(MAINTAINER・PC/スマホ)

- 招待された人が 招待リンクを開くと 大会名・招待の説明・有効期限が表示され「参加する」を押せる
- 招待された人が 未ログインで招待リンクを開くと ログイン画面へ誘導され、Googleログイン後に元の招待画面へ戻る
- 招待された人が 「参加する」を押すと 共同管理者として登録され、その大会の概要画面へ遷移する
- 招待された人が 有効期限切れ・人数枠切れ・失効済み・存在しないリンクを開くと いずれも同じ「この招待リンクは無効です」表示になり、運営者への確認を促される
- 既に共同管理者の人が 同じリンクをもう一度開くと 「すでに参加しています」と表示され、枠を消費せず大会を開ける
- OWNER 本人が 自分の大会の招待リンクを開くと 「すでに参加しています」と表示され、二重登録されない
- 招待された人が ログイン後に大会一覧を開くと 自分の大会と混在した一覧に「共同管理」バッジ付きで表示される

### 共同管理者としての操作(MAINTAINER・PC/スマホ)

- MAINTAINER が 大会管理画面を開くと ナビゲーションに「設定」が表示されず、概要・参加者・ラウンド・順位・対戦結果だけが並ぶ
- MAINTAINER が 参加者管理・ラウンド生成・結果入力・ラウンド確定・帳票印刷を行うと OWNER と同じ結果になる
- MAINTAINER が 設定画面のURLを直接開くと 「この操作を行う権限がありません」と表示され、大会概要へ戻る導線が出る
- MAINTAINER が 大会の削除・共有URLの再発行・共同管理者の管理APIを呼ぶと 403 になる
- 取り消された元 MAINTAINER が 大会のURLを開くと 404(大会が見つかりません)になり、大会一覧からも消えている
- どの大会のメンバーでもない運営者が 大会のURLを開くと 404 になり、403 との差から「その大会に所属していないだけ」なのか「大会が存在しない」のかを区別できない

### 同時操作(競合)

- 2人の運営者が 同時に参加者を追加すると どちらも成功して `entryOrder` は連番になる。同一カウンタへの競合が起きた側は 409 になり、画面に再試行を促すメッセージが出る
- 2人が 人数枠1の招待リンクを同時に承諾すると 先に確定した1人だけが共同管理者になり、もう1人は「この招待リンクは無効です」になる
- OWNER が 共同管理者を取り消した直後に その人が操作を続けると 次のリクエストから404になる
- 取り消された人が 取り消し前に受け取っていた(期限内・枠内でまだ有効だった)招待リンクを再度開くと 取り消しと同時に招待リンクも失効しているため「この招待リンクは無効です」表示になり、再承諾でMAINTAINERに復帰することはできない
- MAINTAINER Aが対局結果を編集中に MAINTAINER Bが同じラウンドを先に確定させると Aの編集は失敗し(「確定済みラウンドの結果は変更できません」)、確定済みラウンドの結果が黙って書き換わることはない(§4.9)。個人戦・団体戦のどちらでも同じ対策が効く(同じRoundを参照するため)

### 個人戦・団体戦・グループ分け

- 共同管理の権限判定は個人戦/団体戦、グループ分けの有無で変わらない(大会単位の権限であり、参加者・チーム・グループの構造には依存しない)

### 大会の状態(PREPARING/IN_PROGRESS/FINISHED)

- 招待の発行・失効・承諾、共同管理者の取り消しは **大会の状態を問わず常に可能**(409になる状態遷移制約を持たない)。
  参加者・チーム・グループの追加/削除が大会開始後に409で拒否されるのは競技データの整合性を守るためだが、
  共同管理系の操作は競技データ(参加者・対局結果)を一切変更せず、「誰が管理画面を操作できるか」だけに
  関わる。むしろ IN_PROGRESS 中(当日の結果入力・ラウンド確定を分担したい場面)こそ共同管理者を
  増減させる主要な利用シーンであり、FINISHED 後も監査目的で共同管理者を整理できる必要がある

## 3. UI仕様

記述の厚みは `00_basic_design.md` §4 の優先度に従う。本機能は **4. 運営者管理画面** に属するため
MUI標準に寄せてよいが、招待受諾画面(S14)はスマホで開かれる可能性が高いため 375px の指定を明示する。

### S14 招待の受諾(新規画面・`/invite/:token`)

`RequireAuth` 配下に置き、未認証ならログインへ誘導する(トークン自体はアクセス制御に使わない。
「誰が承諾したか」を記録するためログインが前提)。

- **レイアウト構成**(上から順):
  1. アイコン(招待を表す。`GroupAddIcon` 等、primary色・48px)
  2. 見出し h1「大会の共同管理に招待されています」
  3. 大会名(見出し h2・強調)+ 競技種別チップ(囲碁/将棋)+ 開催日(未設定なら非表示)
  4. 説明文「承諾すると、この大会の参加者管理・ラウンド進行・結果入力ができるようになります。大会設定の変更と大会の削除はできません。」
  5. 有効期限の注記(caption)「この招待は YYYY/MM/DD HH:mm まで有効です」
  6. ボタン: 「参加する」(contained・primary)/「キャンセル」(text → 大会一覧へ)
- **主要要素**: カード1枚を中央寄せ。ボタンは縦積みで「参加する」が上
- **4状態の見せ方**:

  | 状態 | 表示 |
  |---|---|
  | 通常 | 上記レイアウト |
  | 空(0件) | 単一リソースのため該当なし。後述の「招待が無効」が実質の代替 |
  | ローディング | `FullPageSpinner`(ルート lazy と同じ扱い) |
  | エラー | `ErrorState`「招待情報の取得に失敗しました」+ 再試行ボタン(通信失敗時のみ。403は下記の「無効」へ振り分ける) |

- **通常・エラー以外の分岐**(このAPIは単一リソースのため4状態だけでは足りない):

  | 分岐 | 表示 |
  |---|---|
  | 招待が無効(期限切れ・枠切れ・失効・不正トークン。GETプレビュー時点で判明) | アイコン + 「この招待リンクは無効です」+「大会の運営者に新しいリンクの発行を依頼してください」+「大会一覧へ」ボタン。**理由は出し分けない**(§4のセキュリティ方針) |
  | すでにメンバー(MAINTAINER または OWNER 本人) | 「すでにこの大会に参加しています」+「大会を開く」ボタン(大会概要へ) |
  | 「参加する」実行時(POST accept)に403(期限切れ・枠切れ・失効)を受け取った場合 | プレビュー時点では有効だったが承諾までに無効化された場合(§2「同時操作」)。**上記「招待が無効」ビューへ切り替える**(別トーストは出さない。同じ理由文言に統一するため) |

- **大量データ時**: 該当なし(単一リソース)
- **レスポンシブ**: 375px はカード幅100%・外側padding 16px・ボタン全幅。デスクトップは `max-width: 480px` で中央寄せ
- **既存画面との一貫性**: 中央寄せカード1枚という構図・トーンは `LoginPage` に合わせるが、`LoginPage`
  自体が持つアプリロゴ+「Swiss Stage」の大見出しは再利用しない(このカードは招待という1つの
  アクションに閉じた小さな確認画面であり、ブランドの全面訴求ではないため)。招待という行為を
  表すアイコンを先頭に置く点は `ConfirmDialog` 等の既存パターンと同じ発想
- **新しいUIパターンの有無**: なし(Card + Stack + Button の既存構成)

> この画面は新規画面(0→1)のため、`frontend/src/pages/InvitationPage.stories.tsx` を本Plan PRに含める
> (`04_development_process.md` §5.1.1)。本物のページは未実装のため、ストーリー内にインラインの
> プレースホルダー実装を書き、実装PRで本物のページに置き換えて実importへ切り替える
> (`12_story_first_existing_page_placeholder.md`)。

### S09 大会設定 —「共同管理者」カードの追加(既存画面)

セクションを1枚足すだけでレイアウトの骨格(単カラム・`maxWidth: 600` のカード積み)は変えないため、
「大きなレイアウト変更」には当たらない。ストーリーは作成しない。

- **配置**: 「共有URL」カードの下、「大会を削除」カードの上
- **レイアウト構成**:
  1. 見出し h3「共同管理者」
  2. 説明文(body2・secondary)「招待リンクを渡した運営者が、この大会の参加者管理・ラウンド進行・結果入力を行えるようになります。大会設定の変更と大会の削除はできません。」
  3. 招待リンク領域(下記の状態別)
  4. 区切り線
  5. 共同管理者一覧
- **招待リンク領域の状態**:

  | 状態 | 表示 |
  |---|---|
  | 未発行 | 人数枠 Select(**1〜(9−現在の共同管理者数)人**・既定は選択可能な範囲とmin(3, 残り枠)のうち小さい方)+「招待リンクを発行」ボタン(outlined) |
  | 発行済み(有効) | 読み取り専用 TextField(リンク)+ コピー IconButton(既存の共有URLカードと同じ形)+ caption「残り N 人 / YYYY/MM/DD HH:mm まで有効」+「無効にする」ボタン(text・error)|
  | 発行済み(期限切れ・枠切れ) | 「この招待リンクは無効です」+ 人数枠 Select(未発行と同じ動的上限)+「招待リンクを再発行」ボタン |
  | 上限到達(共同管理者9人) | 発行フォームを disabled にし、「共同管理者は9人までです」を表示。人数枠 Select の選択肢が0件になる状態と同義のため、この状態は「未発行」「期限切れ・枠切れ」の派生として実装する(専用の判定を別に持たない) |

  招待リンク領域は共同管理者一覧と同じ `GET /members`(§4.7)の1回の fetch を共有する(queryKey
  1本、§4.8)。そのため**ローディング・エラー時の表示は共同管理者一覧側と同期する**(カード全体を
  下記の「共同管理者一覧の状態」表のローディング/エラー表示にし、招待リンク領域だけを個別に
  ローディング/エラー状態にしない)。

- **共同管理者一覧の状態**:

  | 状態 | 表示 |
  |---|---|
  | 通常 | List。1行 = 表示名(primary)+ 参加日(secondary)+ 取り消し IconButton |
  | 空(0件) | カード内テキスト「共同管理者はまだいません」(`EmptyState` はページ全体用のため使わない) |
  | ローディング | カード内 `CircularProgress`(ページ全体をブロックしない) |
  | エラー | カード内に「共同管理者の取得に失敗しました」+ 再試行ボタン |

- **確認ダイアログ**: 失効・取り消しはいずれも `ConfirmDialog` を通す(`window.confirm` 禁止)。
  文言は「招待リンクを無効にしますか?」「〇〇さんの共同管理を解除しますか?(発行中の招待リンクが
  あれば同時に無効になります)」。取り消しの文言は招待リンクが未発行・既に失効済みの場合でも同じ
  文言で統一する(取り消しのたびに招待リンクの状態を見て文言を出し分けない。実際に招待リンクが
  無い場合は「(発行中の招待リンクがあれば〜)」がそのまま無害な条件文として読める)
- **大量データ時**: 共同管理者は最大9人のためスクロール・ページングは不要
- **レスポンシブ**: 375px ではリンク TextField とボタンが縦積みになる(既存の共有URLカードと同じ挙動に合わせる)
- **既存画面との一貫性**: 共有URLカードのコピー IconButton + Tooltip、削除カードの ConfirmDialog をそのまま踏襲する
- **新しいUIパターンの有無**: なし。招待リンク領域・共同管理者一覧のカード内スコープの
  ローディング(`CircularProgress`)・エラー表示は、既存の「共有URL」カード(単一fetchに依存する
  設定画面内カード)と同じ構成の踏襲
- **MAINTAINER が直接URLを開いた場合**: `TournamentLayout` が持つ role を見て、設定ページ全体を
  「この操作を行う権限がありません」+「大会概要へ戻る」ボタンに差し替える(APIの403に頼らず即座に表示する)

### S03 大会一覧 —「共同管理」バッジ(既存画面)

- `TournamentCard` の状態バッジ(準備中/開催中/終了)の右に Chip(size="small"・variant="outlined")で
  「共同管理」を表示する。所有大会には出さない
- 色の違いのみに依存しない(テキストで判別できる)
- 並び順・検索・状態フィルタは現行のまま(所有大会と混在。`TRN-AC-020`〜`TRN-AC-023` の挙動を変えない)
- 空状態のメッセージも現行のまま(所有大会0件でも共同管理大会があれば一覧は空にならない)

### 運営者管理画面の共通ナビゲーション(`TournamentLayout`)

- role が MAINTAINER のとき、サイドバー(PC)・下部タブ(スマホ)の項目から「設定」を除外する
  (6項目 → 5項目。下部タブの横幅制約はより緩くなるため崩れない)
- 現在地表示・キーボード操作の要件は現行どおり(`TRN-AC-017` を壊さない)

## 4. 技術設計

### 4.1 認可モデル

現行の認可は `TournamentAccessSupport.loadOwned(id, sub)` の1箇所に集約されており、全サービスが
これを通る。この関門を役割別に割ることで、サービス層の呼び出し先を差し替えるだけで済む。

- `loadOwner(id, sub)` — OWNER のみ許可。大会設定・削除・共有トークン再発行・招待/メンバー管理で使う
- `loadMember(id, sub)` — OWNER または MAINTAINER を許可。それ以外の全ユースケースで使う
- `roleOf(id, sub)` — `Optional<TournamentRole>`。レスポンスの `role` 組み立てに使う

**404 と 403 の使い分け**(`13_security_design.md` §3 の「存在を漏らさない」方針の拡張):

| 状況 | 応答 |
|---|---|
| メンバーでない(OWNERでもMAINTAINERでもない) | 404 `TOURNAMENT_NOT_FOUND` |
| MAINTAINER が OWNER 専用操作を呼んだ | 403 `FORBIDDEN` |

MAINTAINER に 403 を返しても「自分が所属している大会である」以上の情報は増えないため、存在秘匿は
保たれる。逆にここを404で統一すると、MAINTAINER が設定画面のバグと権限不足を区別できなくなる。

### 4.2 レイヤーごとの変更点

- **domain**
  - `TournamentRole` enum(`OWNER` / `MAINTAINER`)。**順序比較をしない**。権限判定は
    `canManageSettings()` のような明示的な述語メソッドで表現する(ordinal依存の禁止。CLAUDE.md #13)
  - `TournamentMember` record(`memberId` / `sub` / `role` / `displayName` / `joinedAt`)
  - `TournamentInvite` record(`token` / `expiresAt` / `maxUses` / `usedCount` / `version` / `createdAt`)
    + `isAcceptable(Instant now)`(期限内かつ `usedCount < maxUses`)/ `accepted()`(usedCount+1)
  - `Tournament` に `nextEntryOrder`(`Integer`・null = 未初期化)を追加し、
    `withNextEntryOrder(int)` を生やす
  - repository IF: `TournamentMemberRepository` / `TournamentInviteRepository`
- **application**
  - `TournamentAccessSupport` を上記3メソッドへ再構成
  - `TournamentMemberService`(招待の発行・失効・一覧、共同管理者の一覧・取り消し)。取り消しは
    MEMBER アイテムの削除に加えて、有効な INVITE アイテムがあれば同時に削除する(§4.4)。招待の
    失効処理を内部メソッドとして共有し、DELETE `/invite` と同じ経路を通す
  - `InvitationService`(招待のプレビュー・承諾)
  - `TournamentService.list()` は「所有大会 + メンバーシップ由来の大会」をマージして返す
  - `ParticipantService` の `nextEntryOrder()` をカウンタ方式へ差し替え(§4.5)
  - 既存サービスの `loadOwned` 呼び出しを `loadMember` へ置換(設定系は `loadOwner`)
- **presentation**
  - `TournamentMemberController`(`/api/v1/tournaments/{id}/members`・`/invite`)
  - `InvitationController`(`/api/v1/invitations/{token}`)
  - `@PathVariable` の名前は必ず明示する(CLAUDE.md #15・ArchUnitで強制済み)
  - `OAuth2LoginSuccessHandler` のリダイレクト先を可変化(§4.6)
- **infrastructure**
  - `DynamoDbTournamentMemberRepository` / `DynamoDbTournamentInviteRepository`
  - `DynamoDbKeys` に `memberSk` / `INVITE_SK` / `gsi2Pk(inviteToken)` 相当を追加

### 4.3 データモデル(DynamoDB)

`02_database_design.md` への反映は実装PRで行う。方針は以下。

**新しいアクセスパターン**:

| # | アクセスパターン | 使用キー |
|---|---|---|
| AP11 | 大会の共同管理者一覧を取得 | PK=`TOURNAMENT#{id}`, SK begins_with `MEMBER#` |
| AP12 | ユーザーが共同管理する大会一覧を取得 | GSI1: PK=`USER#{sub}`(`entityType=MEMBER` の項目) |
| AP13 | 招待トークンから大会と招待を特定 | GSI2: PK=`INVITE#{token}` |
| AP14 | 大会の招待の現在状態を取得 | PK=`TOURNAMENT#{id}`, SK=`INVITE` |

**Member アイテム**:

| 属性 | 例 | 備考 |
|---|---|---|
| PK / SK | `TOURNAMENT#01J...` / `MEMBER#{sub}` | SKに `sub` を使い、同一ユーザーの二重登録を構造的に不可能にする |
| entityType | `MEMBER` | |
| memberId | `01J...`(ULID) | APIのパスで使う識別子。`sub` をURLに出さないため |
| sub | Google sub | |
| role | `MAINTAINER` | MVPでは常にこの値。OWNER はMEMBERアイテムを持たない(`ownerSub` が正) |
| displayName | 承諾時のGoogle表示名 | OWNERが「誰を許可したか」を確認するために保持する個人情報。**OWNER向けレスポンス以外に出さない・ログに出さない**(CLAUDE.md #8) |
| joinedAt | ISO8601(UTC) | |
| GSI1PK / GSI1SK | `USER#{sub}` / `TOURNAMENT#{createdAt}` | AP12用。SKには**大会の**作成日時を入れ、所有大会と同じ並び順に揃える |

`version` は持たない(作成と削除のみで更新がないため)。

**Invite アイテム**:

| 属性 | 例 | 備考 |
|---|---|---|
| PK / SK | `TOURNAMENT#01J...` / `INVITE` | 1大会につき有効な招待は常に1本。再発行は同じアイテムの上書き |
| entityType | `INVITE` | |
| token | `SecureRandom` 32バイトのURL-safe Base64 | 既存 `ShareTokens` を流用 |
| expiresAt / maxUses / usedCount | ISO8601 / 1〜9 / 0〜 | |
| GSI2PK | `INVITE#{token}` | AP13用 |
| version | number | 楽観ロック。**必須**(同時承諾での枠超過を防ぐ) |

**GSI を増やさない**。GSI2 は PK のみのインデックスで、`SHARE#{token}` と `INVITE#{token}` は
名前空間が異なるため同居できる(1アイテムがGSI2PKを1つしか持てない制約は、共有トークンが
METADATA アイテム・招待トークンが INVITE アイテムと別アイテムであることで回避される)。
GSI は引き続き2本で、上限3本に余裕を残す。

**既存データの移行は不要**。METADATA アイテムのキーも `ownerSub` も変えない。

> **実装時の落とし穴(CLAUDE.md への追記候補)**: `DynamoDbTournamentRepository.findByOwnerSub()` は
> GSI1 を `TableSchema.fromBean(TournamentItem.class)` で引いている。MEMBER アイテムが同じ GSI1PK に
> 相乗りするため、**`entityType = TOURNAMENT` のフィルタ式を必ず付ける**。付け忘れると MEMBER
> アイテムが `TournamentItem` としてマッピングされ、必須属性欠落で壊れるか、最悪サイレントに
> 不正な大会が一覧に混ざる。同様に MEMBER 側のクエリには `entityType = MEMBER` を付ける。

**大会削除**: 既存の「パーティション全Query → BatchWrite」で MEMBER・INVITE アイテムも自動的に
物理削除される(個人情報保護方針どおり `displayName` も消える)。

### 4.4 招待リンクのライフサイクル

- 発行: OWNER が人数枠(`maxUses`)を指定。有効期限は **発行から72時間固定**(UIで期限を選ばせない。
  シンプルさ優先)。トークンは `ShareTokens.generate()` と同じ強度
- 無効化される条件: 期限超過 / `usedCount >= maxUses` / OWNER による失効(DELETE) / 再発行による上書き /
  **共同管理者の取り消し(`DELETE /members/{memberId}`)**
- **取り消しは招待リンクも道連れにする**: 承諾処理のMEMBER作成条件は `attribute_not_exists(SK)`
  のみ(後述)で、招待リンク自体は期限内・枠内なら承諾のたびに有効であり続ける。もし取り消し操作が
  招待リンクの状態に影響しないと、取り消された本人が同じリンクをまだ持っていた場合、取り消し直後に
  再度「参加する」を実行するだけでMAINTAINERに復帰でき、「取り消し」の意味(以後アクセスできなく
  する)が保証されない。これを防ぐため、`DELETE /members/{memberId}` は MEMBER アイテムの削除に
  加えて、その時点で有効な招待リンクがあれば INVITE アイテムも同時に削除する(実装は DELETE
  `/invite` と同じ処理を内部的に呼ぶだけで、新しい仕組みは増やさない)。副作用として、取り消しの
  対象ではない・まだ承諾していない他の招待済み候補者もこのリンクを使えなくなる(招待リンクは
  1大会につき1本の共通リンクであり個人宛てではないため、取り消しと無関係な人だけを選んで無効化する
  ことはできない)。OWNER が引き続き他の人を招きたい場合は招待リンクを再発行する
- **枠は単調減少**: 共同管理者を取り消しても枠は戻らない。枠を戻したい場合は招待リンクを再発行する
  (再発行で `usedCount` は0にリセットされる)。理由は「上限判定を1アイテムの条件付き更新に閉じ込め、
  同時承諾で枠を超えないことを保証するため」。メンバー数を数えて判定する方式は、数えた後・書き込む前に
  他の承諾が入ると上限を超えうる
- **大会全体の上限は共同管理者9人**(OWNER含めて10人)。この上限は**承諾時ではなく発行時**に
  一度だけ強制する: `maxUses` は「1〜9」に加えて「9 − 発行時点の共同管理者数」以下でなければならず、
  超える指定は 400 `VALIDATION_ERROR` になる(MBR-AC-019)。**承諾時に別途チェックしない**
  (承諾時にもチェックすると、招待自体の `remainingUses` は正なのに一部の承諾だけ拒否されるという
  UI上説明の難しい状態が生まれる。発行時点で枠を絞ることで、招待が有効である限り「残り枠 = 実際に
  受け入れ可能な人数」という不変条件を維持し、承諾フローを1本に保てる)。1大会につき有効な招待は
  常に1本(§4.3)なので、複数の招待が発行済み上限を同時に食い合う心配もない。再発行時も同じ動的
  上限を発行時点の共同管理者数で再計算するため、「再発行で枠をリセットして上限を回避する」ことは
  できない(MBR-AC-012)
- **二重承諾**: 既に MAINTAINER / OWNER 本人が承諾した場合は成功(200)を返すが、`usedCount` は
  増やさず MEMBER も追加しない(冪等)
- **承諾の手順と競合時の応答**: (1) INVITE を読み、`isAcceptable(now)` を確認する。不可なら
  403 `INVALID_INVITE_TOKEN`。(2) `TransactWriteItems` で「INVITE の条件付き更新(version一致で
  `usedCount+1`)」+「MEMBER の作成(`attribute_not_exists(SK)`。二重承諾を弾く)」を原子的に行う。
  (3) INVITE 側の version 不一致(同時承諾での競合)は **クライアントに409を返さず、サーバー内部で
  (1)から再試行する**(数回程度の上限付き)。「参加する」ボタンはユーザーが編集内容を持つ画面ではなく
  1回のクリックで完結させたい操作であり、`entryOrder` の採番(§4.5)のように「画面を更新して再試行」
  をユーザーに委ねる理由がないため、ここでは `ParticipantService` とは異なる選択をする。再試行後に
  枠が尽きていれば §2「同時操作」のとおり403になる。再試行の上限に達した場合(通常はミリ秒単位の
  競合ウィンドウでしか起こらない)は500として扱う。この設計により `POST /invitations/{token}/accept`
  は**契約上409を返さない**(`schema/openapi.yaml` の responses に409を含めない)

### 4.5 `entryOrder` の同時採番

現行の `ParticipantService.nextEntryOrder()`(個人戦)・`TeamService.nextEntryOrder()`(団体戦、
`TeamService.java:316`)は**どちらも**全件を読んで最大値+1を返す read-then-write で、条件付き
書き込みがない。単一運営者前提が崩れると採番が重複し、**初回マッチングのエントリー順が壊れる**
(`Team.entryOrder` は `TeamSwissPairingService` の初回ペアリング順でも `Participant.entryOrder`
と同じ重みを持つ。`05_swiss_pairing_algorithm.md` の初回ペアリングに直結する)。

対策: 大会メタデータに `nextEntryOrder` カウンタを1つ持ち、既存の `@DynamoDbVersionAttribute` に
よる楽観ロックで採番する。**個人戦・団体戦の両方でこのカウンタを共有する**。`competitionType` は
作成後変更不可で、1大会は個人戦か団体戦のどちらか一方でしか `Participant`/`Team` を作らないため
(`02_database_design.md` 「Team は `competitionType=TEAM` の大会でのみ存在する」)、同一大会内で
2つの採番系列が同時に動くことはなく、カウンタを分ける理由がない。

1. 大会を読む。`nextEntryOrder` が null なら、その場で初期値を算出する。この経路は既存大会の移行
   だけでなく、**この機能のリリース後に作られる新規大会もすべて最初の1件目でここを通る**
   (`Tournament.create()` では `nextEntryOrder` を設定しない。作成時に確定する初期値を持たせても
   最初の追加時にどのみち読み直すため、初期化のタイミングを1箇所に絞る)。
   `Participant`/`Team`(`competitionType` に応じたどちらか一方)が**1件以上あれば**その最大
   `entryOrder` + 1、**1件もなければ 1** を初期値とする(空集合の `max()` は例外にしない)
   (**移行スクリプト不要**。初回の追加で自動的に埋まる)
2. 払い出す個数(単体追加=1、CSVインポート=行数)を決め、`nextEntryOrder + n` を書き戻す。
   version 不一致なら `ConflictException`(409)
3. 採番済みの番号で `Participant`(または `Team`)を作成・保存する

CSVインポートは**連続した範囲をまとめて確保する**(1行ずつ採番しない)。途中で他の追加が割り込んでも
範囲が重ならない。個人戦・団体戦とも同じ採番ヘルパー(`TournamentEntryOrderAllocator` のような
共通コンポーネント)に寄せ、実装を2箇所に重複させない。

409 が返った場合の再試行はサーバー側では行わず、既存の競合と同じくフロントに「ほかの端末で更新
されました。画面を更新して再度お試しください」を出す(`ErrorCode.CONFLICT` の既定メッセージ。
自動リトライを入れると、失敗の理由が利用者から見えなくなる)。

> `Participant` / `Team` / `Group` アイテム自体への `version` 追加は本スコープでは行わない。
> 追加・削除は採番カウンタ経由で直列化され、個々の参加者・チームの編集(氏名・所属の変更)は
> 「最後の書き込みが勝つ」で実害が小さいためである。この割り切りは §8 に残す。

### 4.6 ログイン後のリダイレクト先

現在 `OAuth2LoginSuccessHandler` は `frontendBaseUrl + "/tournaments"` にハードコードされており、
招待リンク経由で未認証のユーザーがログインしても招待画面に戻れない。

- `GET /api/v1/auth/login?redirect=<パス>` で戻り先を受け取り、**短命の HttpOnly Cookie**
  (`swiss_stage_redirect`・maxAge 10分・SameSite=Lax)に保存する
- `OAuth2LoginSuccessHandler` がこの Cookie を読んで遷移先に使い、同時に失効させる
- **オープンリダイレクト対策**: 受け取る値は「`/` で始まり `//` で始まらない相対パス」のみ許可し、
  それ以外は無視して `/tournaments` に倒す。絶対URL・スキーム付きは一切受け付けない

### 4.7 API変更(`schema/openapi.yaml` を本PRで更新)

| メソッド | パス | 権限 | 概要 |
|---|---|---|---|
| GET | `/api/v1/tournaments/{id}/members` | OWNER | 共同管理者一覧 + 現在の招待状態を1レスポンスで返す |
| DELETE | `/api/v1/tournaments/{id}/members/{memberId}` | OWNER | 共同管理者の取り消し。存在しない `memberId` は冪等にせず 404 `TOURNAMENT_MEMBER_NOT_FOUND` を返す(`DELETE /invite` の冪等204とは異なる。参加者・チームメンバー削除と同じ「特定サブリソースの404」の扱いに揃える) |
| POST | `/api/v1/tournaments/{id}/invite` | OWNER | 招待リンクの発行・再発行(body: `maxUses`)。`maxUses` は1〜9かつ「9−発行時点の共同管理者数」以下(超過は400)。**GET /members と同じ更新後のビューを返す**。応答は**200**(初回発行・再発行を同じ処理(同一アイテムの上書き)として扱う既存の `regenerateShareToken` と同じ判断。`03_api_design.md` §4のルール上は新規作成寄りに見えるが、1大会につき有効な招待は常に1本で「作成」と「更新」を利用者側が区別する意味がないため、既存の共有トークン発行と同じ扱いに揃える) |
| DELETE | `/api/v1/tournaments/{id}/invite` | OWNER | 招待リンクの失効 |
| GET | `/api/v1/invitations/{token}` | 認証済み・**IPベースのレート制限あり** | 招待のプレビュー(大会名・期限・すでにメンバーか) |
| POST | `/api/v1/invitations/{token}/accept` | 認証済み・**IPベースのレート制限あり** | 承諾。成功時に `tournamentId` を返す |

**大会の状態による制約はない**: 上記6エンドポイントはいずれも大会の状態(PREPARING/IN_PROGRESS/
FINISHED)を問わず利用できる(409の状態遷移制約を持たない)。理由は §2「大会の状態」を参照。

設定画面は「共同管理者一覧」と「招待リンク」を必ず同時に表示するため、両者を1つのビュー
(`TournamentMembersView`)にまとめ、発行も同じビューを返す。フロントは queryKey 1本で扱える。

招待は `TournamentMembersView` の中にインラインで定義し、独立したスキーマにしない。OpenAPI 3.0 では
`$ref` を nullable にできず、`allOf` での合成もこのファイルでは使えない(`schema/openapi.yaml` の
responses 節の注記のとおり、swagger-request-validator が `additionalProperties: false` を暗黙適用
するため `allOf` と併用すると必ず失敗する)。招待を返すのはこのビューだけなので、インライン定義に
しても重複は生じない。

**招待トークンのレート制限**: `GET /api/v1/invitations/{token}` ・ `POST /api/v1/invitations/{token}/accept`
はいずれも既存の共有トークン(`/api/v1/shared/{token}` 系)と同じく、トークンの正しさをレスポンスの
違いで判別できてはならない(§4.4 で「理由を出し分けない」と決めた前提そのもの)。総当たりでの
トークン探索を防ぐため、既存の `SharedRateLimitFilter`(bucket4j・IPベース)と同じ仕組みをこの2
エンドポイントにも適用する(`13_security_design.md` §5「結果入力・トークンアクセスにIPベースの
簡易レート制限」、既存の `SHR-AC-009` / `SharedRateLimitApiTest` が同種の前例)。超過時は既存の
`RATE_LIMITED`(429)をそのまま使う(新しいエラーコードは追加しない)

既存スキーマの変更:

- `Tournament` に `role`(`OWNER` / `MAINTAINER`)を追加。一覧・詳細の両方で返す
- `Tournament.shareToken` は **OWNER にのみ返す**(MAINTAINER 向けレスポンスでは省略)。
  共有URLの管理は設定画面=OWNER専用の機能であり、MAINTAINER に渡す必要がないため
  (`13_security_design.md` §6-4 の「共有トークン経由のレスポンスに shareToken を含めない」と同じ発想)
- `GET /api/v1/auth/login` に `redirect` クエリパラメータを追加(§4.6)
- **`updateTournament`(PATCH `/tournaments/{id}`)・`deleteTournament`(DELETE `/tournaments/{id}`)・
  `regenerateShareToken`(POST `/tournaments/{id}/share-token/regenerate`)の3エンドポイントの
  responsesに403 Forbiddenを追加する**。この3つは既存エンドポイントで、`loadOwner` の対象
  (§4.1)であり MBR-AC-002 の対象でもあるが、これまで存在しなかった「認証済みだが権限がない」
  という応答をこのPRで初めて持つため、契約(openapi.yaml)側の追加が必要
- `invitations` の2エンドポイントのresponsesに429 `$ref: "#/components/responses/RateLimited"` を追加

`schema/openapi.yaml` の変更が不要なもの:

- `ResultInputBy` は API に露出していない(対局アイテムの監査用属性でDTOに含まれない)ため、
  `MAINTAINER` の追加は**バックエンドの enum とデータモデルのみの変更**になる。
  誰が入力したかを個人単位では記録しない(`sub` を対局アイテムに保存すると保持方針・削除方針・
  表示範囲を含む監査ログの設計が別途必要になり、スコープが膨らむため)
- `POST /api/v1/tournaments/{id}/participants` と `/participants/import` は**既に409を定義済み**
  (大会開始後の追加拒否)。採番カウンタの競合も同じ `CONFLICT` に乗るためスキーマ変更は不要

**新しいエラーコード**:

- `INVALID_INVITE_TOKEN`(403 /「この招待リンクは無効になっています。運営者に確認してください」)。
  期限切れ・枠切れ・失効・不正トークンをすべてこの1コードに寄せ、**理由を出し分けない**
  (総当たりで有効なトークンの存在を推測させないため。`INVALID_SHARE_TOKEN` と同じ方針)
- `TOURNAMENT_MEMBER_NOT_FOUND`(404 /「共同管理者が見つかりません」)。存在しない `memberId` の
  取り消しに使う。`TEAM_MEMBER_NOT_FOUND` と同じ命名規約(特定サブリソースの404には既存の
  `TOURNAMENT_NOT_FOUND` を再利用しない)。「存在を漏らさない」方針(`13_security_design.md` §3)は
  他人の大会を隠す目的のものであり、OWNER自身が管理する大会内のメンバーIDの存在有無を隠す必要はない

`06_error_handling_design.md` の表への追記は実装PRで行う。

### 4.8 フロントエンド

- ページ: `InvitationPage`(`/invite/:token`・`RequireAuth` 配下・ルート分割は既存どおり lazy)
- `routes.ts` に `invitation: (token) => \`/invite/${token}\`` を追加。共有ページの `/s/:token` とは
  パスを明確に分ける(トークンの種類を取り違えないため)
- **`LoginPage.tsx` の配線**(§4.6 のバックエンド側の仕組みを実際に使うために必須): 現行の
  `LoginPage.tsx` は Google ログインボタンを `<Button component="a" href={GOOGLE_LOGIN_URL}>`
  (`GOOGLE_LOGIN_URL = '/api/v1/auth/login'` 固定文字列)として実装しており、`redirect` パラメータを
  付与する余地がない。既に計算済みの `redirectTo`(`location.state.from.pathname` 相当。開発用
  ログインの `navigate(redirectTo)` が使っているのと同じ値)を、Google ログインリンクの href に
  `?redirect=${encodeURIComponent(redirectTo)}` として付与するよう変更する。OAuth2はSPA内遷移ではなく
  `<a>` によるフルページ遷移のため(`LoginPage.tsx` の既存コメントのとおり)、`location.state` は
  ログイン後まで生き残らない。バックエンドへ遷移先を伝える手段はこのクエリパラメータ経由に限られる
- features: `components/features/tournament/CollaboratorsCard.tsx`(設定画面のセクション)
- hooks: `useTournamentMembers(id)` / `useIssueInvite(id)` / `useRevokeInvite(id)` /
  `useRemoveMember(id)` / `useInvitation(token)` / `useAcceptInvitation(token)`
- queryKey: `['tournaments', id, 'members']` / `['invitations', token]`。招待の発行・失効・
  共同管理者の取り消しはいずれも `['tournaments', id, 'members']` の1本を更新すればよい
  (発行は更新後のビューをそのまま返すため `setQueryData` で置き換えられる)。
  承諾成功時は `['tournaments']` を invalidate して一覧に反映する
- `TournamentLayout` の context に `role` を載せ、ナビゲーション項目と `SettingsPage` の
  権限判定に使う
- 型は `pnpm run generate:api` で `schema/openapi.yaml` から再生成する

### 4.9 ラウンド確定と結果編集の競合

本計画§1は「同時操作でデータが黙って壊れないこと」を最重要原則の1つに掲げており、当日の結果入力・
ラウンド確定を複数MAINTAINERで分担することが本機能の主要な利用シーンである。この前提のもとで、
既存の結果入力・ラウンド確定の実装に見落としていたTOCTOU(time-of-check-time-of-use)競合がある。

**現状の問題**: `RoundService.editMatch()`(個人戦)・`TeamRoundService` の同名メソッド(団体戦。
どちらも個人戦・団体戦で共通の `Round` を参照する)は、「ラウンドが確定済みか」を対局の読み取り
直後に**1回だけ**チェックし(`round.status() == CONFIRMED`)、実際のMatch書き込みは
Matchの`version`楽観ロックのみで行う(Round側は再チェックしない)。そのため次の競合ウィンドウが
成立する: MAINTAINER Aが既に結果入力済みの対局Xを訂正しようとしてRound確定チェックを通過した
直後、MAINTAINER Bが(その時点で全対局が決着済みのため)ラウンド確定に成功しRoundをCONFIRMEDで
保存し、続いてAの対局X訂正がMatchのversion楽観ロックだけを通って保存されてしまう。これは
`RND-AC-010`(確定済みラウンドの結果は変更できない)という既存の不変条件が破られる経路であり、
単一運営者前提では同一人物が2タブを同時に開く稀なケースでしか起きなかったが、本機能により
「当日の分担」という日常的な運用として発生確率が上がる。

**対策**: `editMatch`(`RoundService`・`TeamRoundService`の両方が対象)のMatch保存を、Round確定
チェックと同じ`TransactWriteItems`で行う。トランザクションに「Matchの更新」+「Round項目への
ConditionCheck(`status <> CONFIRMED`)」を含める。競合窓の間にRound確定が割り込んだ場合はこの
ConditionCheckが失敗し、既存の`InvalidStateException`(「確定済みラウンドの結果は変更できません」)
にマッピングする。Match自体のversion不一致は従来どおり`ConflictException`にマッピングする
(トランザクションのキャンセル理由をアイテムごとに判別して振り分ける)。この方式なら`Round`に
新たに`version`属性を追加する必要はなく、トランザクションのアイテム数もMatch1件+Round1件の
ConditionCheckの計2件のため、300名規模の大会でも`TransactWriteItems`の上限(100アイテム)に
抵触しない。

**対策の範囲外(残存リスク)**: 逆方向、すなわち「ラウンド確定の`undecided==0`判定が、判定直後に
割り込む結果編集によって陳腐化し、実際には未確定の対局が残ったままRoundがCONFIRMEDで保存される」
ケースは、本PRのスコープでは対策しない。この方向を完全に塞ぐには確定操作をその時点の全対局の
versionを条件に含めたトランザクションにする必要があるが、大規模大会(最大300名・1ラウンドあたり
最大150対局)では`TransactWriteItems`の100アイテム上限に抵触しうる。この制約は既存の
`02_database_design.md`決定事項5(「ラウンド確定+次ラウンド生成は`TransactWriteItems`で原子的に
行う(上限100アイテムに注意→大規模大会ではラウンド確定とマッチ生成を分離し、Roundのstatusで
整合性を担保する)」)と同根の既知の制約であり、本機能固有の新しい問題ではない。撤回条件: 複数
MAINTAINER運用でこの方向のレースが実際に発生した場合、別Issueで対応する(例: 確定操作の前に
全対局を再読込し不一致があれば409で拒否する、等の緩和策を追加で検討する)。

## 5. 受け入れケース

新しいコンポーネント(contractテストクラス)として `TournamentMemberApiTest` / `InvitationApiTest` を
追加するため、プレフィックス **MBR** を `00_acceptance_policy.md` §2 の表に追記する。

| ID | P | 受け入れ基準 | 検証手段 |
|---|---|---|---|
| MBR-AC-001 | P0 | 招待を承諾したユーザーはMAINTAINERとして登録され、その大会の参加者管理・ラウンド進行・結果入力APIを実行できる | contract |
| MBR-AC-002 | P0 | MAINTAINERが大会設定・削除・共有トークン再発行・招待/メンバー管理APIを呼ぶと403 FORBIDDENになる | contract |
| MBR-AC-003 | P0 | どのメンバーでもないユーザーは大会APIすべてで404 TOURNAMENT_NOT_FOUNDになり、403との差で所属を推測できない | contract |
| MBR-AC-004 | P0 | 期限切れ・人数枠切れ・失効済み・不正な招待トークンはいずれも同一の403 INVALID_INVITE_TOKENになり、理由を出し分けない | contract |
| MBR-AC-005 | P0 | 人数枠を超える同時承諾は上限で打ち切られ、枠を超えるMAINTAINERは作られない | contract |
| MBR-AC-006 | P0 | 共同管理者一覧・招待情報(トークンを含む)はOWNERにのみ返り、MAINTAINER・未認証には返らない | contract |
| MBR-AC-007 | P0 | MAINTAINER向けの大会レスポンスにはshareTokenが含まれない | contract |
| MBR-AC-008 | P0 | 招待リンクを再発行すると旧トークンは即時無効になり、使用済みの枠がリセットされる | contract |
| MBR-AC-009 | P0 | OWNERが共同管理者を取り消すと、取り消された側は以後その大会で404になり一覧からも消える | contract |
| MBR-AC-010 | P0 | 招待の失効(DELETE)後はそのトークンで承諾できない | contract |
| MBR-AC-011 | P1 | OWNER本人・既にMAINTAINERのユーザーが承諾しても二重登録されず、人数枠も消費しない | contract |
| MBR-AC-012 | P1 | 共同管理者は9人(OWNER含め10人)を超えて追加できず、招待を再発行しても上限は回避できない(再発行時もmaxUsesの上限が発行時点の共同管理者数で再計算されるため) | contract |
| MBR-AC-013 | P1 | 大会を削除すると共同管理者・招待アイテムも物理削除され、MAINTAINERの大会一覧から消える | contract |
| MBR-AC-014 | P1 | ログイン後のリダイレクト先は自サイト内の相対パスのみ許可し、絶対URL・`//`始まりは無視して大会一覧へ戻す | contract |
| MBR-AC-015 | P2 | 招待受諾画面は、通常・招待が無効・すでにメンバーの3分岐をそれぞれ専用の表示と導線で出し分ける | Vitest |
| MBR-AC-016 | P0 | 招待のプレビュー・承諾APIはIPベースのレート制限超過で429になる(共有APIと同じ保護だが、招待トークンの漏洩はMAINTAINER=書き込み権限の奪取に直結するためSHR-AC-009より優先度を上げる。下記の優先度注記を参照) | contract |
| MBR-AC-017 | P2 | 人数枠1で発行した招待は1人が承諾すると即座に枠切れになり、以後の承諾はINVALID_INVITE_TOKENになる | contract |
| MBR-AC-018 | P2 | 招待発行のmaxUsesに0または10以上を指定すると400 VALIDATION_ERRORになる | contract |
| MBR-AC-019 | P1 | 招待発行のmaxUsesが「9−発行時点の共同管理者数」を超えると400 VALIDATION_ERRORになる | contract |
| MBR-AC-022 | P2 | 共同管理者0人の状態でmaxUses=9を指定すると発行に成功し、共同管理者がN人いる状態でmaxUses=9-Nちょうどを指定しても発行に成功する | contract |
| MBR-AC-023 | P0 | 共同管理者を取り消すと発行中の招待リンクも同時に失効し、取り消された人が同じリンクで再承諾してMAINTAINERに復帰することはできない | contract |
| MBR-AC-024 | P1 | 招待の発行・失効・承諾、共同管理者の取り消しは大会の状態(PREPARING/IN_PROGRESS/FINISHED)を問わず利用できる | contract |
| MBR-AC-025 | P2 | 招待は発行から72時間経過直前は有効(承諾に成功する)、経過直後は無効(INVALID_INVITE_TOKEN)になる | contract |
| MBR-AC-026 | P2 | 存在しないmemberIdのDELETEは404 TOURNAMENT_MEMBER_NOT_FOUNDになる(DELETE /inviteの冪等204とは異なり、参加者・チームメンバー削除と同じ404の扱い) | contract |
| MBR-AC-020 | P2 | 招待を一度も発行していない大会でDELETE /inviteを呼んでも204になる(冪等) | contract |
| MBR-AC-021 | P2 | 招待を一度も発行していない大会のGET /membersはinvite:nullを返す | contract |
| MBR-AC-027 | P2 | GET /invitations/{token}はOWNER本人・既存MAINTAINERに対してalreadyMember:trueを返す | contract |
| PTC-AC-014 | P0 | 参加者を同時に追加してもentryOrderが重複せず、採番カウンタの競合は409 CONFLICTになる | contract |
| PTC-AC-015 | P0 | 採番カウンタ未設定の大会(既存大会の移行・新規大会いずれも)で、参加者0人からの初回追加はentryOrder=1、既存参加者がいる場合は最大entryOrder+1から採番される | contract |
| PTC-AC-016 | P1 | CSVインポートは連続したentryOrderの範囲をまとめて確保し、割り込みの追加と重複しない | contract |
| TEAM-AC-026 | P0 | チームを同時に追加してもentryOrderが重複せず、採番カウンタの競合は409 CONFLICTになる(PTC-AC-014の団体戦版。同じカウンタを使う) | contract |
| TEAM-AC-027 | P0 | 採番カウンタ未設定の大会(既存大会の移行・新規大会いずれも)で、チーム0件からの初回追加はentryOrder=1、既存チームがいる場合は最大entryOrder+1から採番される | contract |
| TEAM-AC-028 | P1 | チームCSVインポートは連続したentryOrderの範囲をまとめて確保し、割り込みの追加と重複しない | contract |
| RND-AC-015 | P0 | 対局結果の編集中に別の運営者が同じラウンドを先に確定させると、編集は失敗し(確定済みラウンドの結果は変更できないエラー)、確定済みラウンドの結果が黙って書き換わらない | contract |
| TEAM-AC-029 | P0 | 団体戦でも対局結果の編集中に別の運営者が同じラウンドを先に確定させると、編集は失敗し確定済みラウンドの結果が黙って書き換わらない(RND-AC-015の団体戦版。同じRoundを参照するため同じ対策が効く) | contract |
| TRN-AC-024 | P2 | 大会一覧で共同管理中の大会に「共同管理」バッジが表示され、所有大会には表示されない | Vitest |
| TRN-AC-025 | P2 | MAINTAINERには管理画面の共通ナビゲーションに「設定」が表示されず、設定画面を直接開くと権限がない旨と戻る導線が表示される | Vitest |
| TRN-AC-026 | P2 | 設定画面の共同管理者セクションで、招待リンクの発行・コピー・失効と共同管理者の取り消しができる | Vitest |
| TRN-AC-027 | P1 | 大会一覧APIは所有大会と共同管理大会を1つのリストに混在させて返し、各アイテムのroleが所有はOWNER・共同管理はMAINTAINERと正しく区別される | contract |
| E2E-AC-010 | P1 | 運営者が招待リンクを発行し、別アカウントが承諾して共同管理者として参加者を追加できる(CP8) | Playwright |

**`MBR-AC-016` の優先度が `SHR-AC-009`(共有APIのレート制限超過429・P2)より高い理由**: どちらも
IPベースのレート制限というトークン総当たり対策の欠落を検証する点は同じだが、漏洩した場合に奪える
権限が異なる。共有トークンの総当たりが成功しても得られるのは参加者向けの閲覧+結果自己申告
(`13_security_design.md` の運用ルールで両者の申告一致が必要・確定済み結果は上書き不可)に留まる。
一方、招待トークンの総当たりが成功すると即座に MAINTAINER(参加者管理・ラウンド確定・結果の直接
確定を含む書き込み権限)を奪取でき、`02_severity.md` の Critical 例「認可チェックの欠落」に近い
実害になる。この非対称性から `MBR-AC-016` は P0 とし、`SHR-AC-009` の優先度は変更しない。

**`MBR-AC-008`・`MBR-AC-009`・`MBR-AC-010`・`MBR-AC-023` はいずれも P0**: この4件は「取り消し・失効・
再発行というアクセスを止める操作が、実際にアクセスを止めているか」という同じ失敗モードを検証する
(再発行後に旧トークンがまだ生きている / 取り消し後も404にならない / 失効後もそのトークンで承諾できる
/ 取り消し後に同じ招待リンクで復帰できる、のいずれも「権限のない人が大会を操作できてしまう」という
`02_severity.md` の Critical 例そのものであり、優先度を分ける理由がない)。特定の1件だけを重く見る
判断はしない。

台帳のジャーニー表に「共同管理」行(CP8・主なPrefix: MBR, TRN)を追加する。

## 6. 更新する資料

### Plan PRで更新するもの

- [ ] `.claude/07_plans/14_tournament_collaboration.md` — 本計画(新規)
- [ ] `.claude/06_adr/13_tournament_collaboration_model.md` — ADR(新規・Status: Proposed)
- [ ] `.claude/06_adr/14_plan_pr_generated_types_exception.md` — ADR(新規・Status: Accepted。理由は下記`api.d.ts`の項を参照)
- [ ] `CLAUDE.md` #18・`.claude/00_project/04_development_process.md` §5.1・`.claude/00_project/03_feature_plan_template.md` §6・`.claude/01_development_docs/11_cicd_design.md`・`.claude/04_quality/01_review_checklist.md`・`.claude/commands/plan.md`・`.claude/agents/planner.md` — ADR 14の決定を反映(Plan PRのコード0行原則の例外を2種類に整理。「`.stories.tsx` のみ」と書いていた箇所を洗い出して統一)
- [ ] `.claude/01_development_docs/09_test_strategy.md`・`.claude/01_development_docs/10_frontend_design.md`・`.claude/commands/pr.md` — ストーリー例外だけを指す `§5.1` 参照を `§5.1.1` に統一(ADR 14 §2)
- [ ] `.claude/agents/plan-reviewer.md` — 「禁止」節にあった「`.stories.tsx` のみ例外」という文言を、他ファイルと同じ「2種類の例外」の表現に更新(ADR 14 §2)
- [ ] `.claude/05_acceptance/00_acceptance_policy.md` — MBRプレフィックスを§2の表に追記
- [ ] `.claude/05_acceptance/01_acceptance_scope.md` — 上記ケースをStatus=todoで追加 + ジャーニー表にCP8を追加
- [ ] `schema/openapi.yaml` — 新規6エンドポイント・`Tournament.role`・`shareToken` の記述をOWNER限定に変更・`/auth/login` の `redirect` パラメータ
- [ ] `frontend/src/types/generated/api.d.ts` — 上記 `schema/openapi.yaml` 変更に伴う `pnpm run generate:api` の機械的な再生成結果(手で編集しない)。CIの生成型鮮度チェックが必須ゲートのためPlan PRでも追随させる必要があり、`04_development_process.md` §5.1.2・`06_adr/14_plan_pr_generated_types_exception.md` でPlan PRの「コード0行」原則の例外として正式に定めた(このADRは本Plan PR内で`Accepted`にし、対象ドキュメントも同じPR内で更新している。§4「Plan PR内でAcceptedにする場合」の例外規定)
- [ ] `frontend/src/pages/InvitationPage.stories.tsx` — 新規画面S14のストーリー(プレースホルダー実装)

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [ ] `.claude/01_development_docs/02_database_design.md` — AP11〜AP14、Member/Inviteアイテム、`nextEntryOrder`、GSI1の`entityType`フィルタ、`editMatch`のMatch更新をRound項目へのConditionCheck付き`TransactWriteItems`に変更する旨(§4.9)
- [ ] `.claude/01_development_docs/13_security_design.md` — §3 認可マトリクスにMAINTAINER列を追加、404/403の使い分け、招待トークンの扱い
- [ ] `.claude/01_development_docs/04_screen_transition_design.md` — S14の追加、S09の権限、ナビゲーション項目の出し分け
- [ ] `.claude/01_development_docs/06_error_handling_design.md` — `INVALID_INVITE_TOKEN`・`TOURNAMENT_MEMBER_NOT_FOUND` の追記
- [ ] `.claude/01_development_docs/03_api_design.md` — 新規エンドポイント群
- [ ] `.claude/01_development_docs/12_e2e_test_design.md` — CP8の追加
- [ ] `CLAUDE.md` — GSI1相乗りに伴う`entityType`フィルタ必須を「避けるべき落とし穴」に追記
- [ ] `.claude/01_development_docs/05_swiss_pairing_algorithm.md` — **更新不要**(`entryOrder`の採番方法は変わるが、初回ペアリングが「entryOrder順」であるという仕様自体は変えない)

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)/ `./gradlew check`(backend)が通る
- [ ] 受け入れケースが台帳で done になり、対応するテストにIDが埋まっている
- [ ] 新規画面S14にsmoke E2E(E2E-AC-010)がある
- [ ] §6「実装PRで更新が必要な設計ドキュメント」が実装PRで更新されている
- [ ] ローカル実機で動作確認済み(`.claude/skills/verify`)。**2つのGoogleアカウント相当
      (local/testプロファイルの `test-login` で別 `sub`)で招待〜承諾〜操作を通す**
- [ ] **`LoginPage.tsx` の Google ログインリンクに `redirect` クエリパラメータが実際に付与され、
      `GET /api/v1/auth/login?redirect=...` → Cookie → `OAuth2LoginSuccessHandler` の経路を通って
      招待画面へ戻ることを確認する**(`test-login` の SPA内遷移は `location.state` を直接使えて
      しまうためこの配線漏れを検出しない。ブラウザの開発者ツールでリンクの href を確認する、または
      実際に Google アカウントでこの経路を1回通すことで確認する)
- [ ] `vrt.yml` を手動実行してS14のベースラインを更新した

### 実装PRの分割案

| # | 内容 | 依存 |
|---|---|---|
| PR1 | `entryOrder` 採番カウンタ + 楽観ロック(§4.5)。共同管理と独立して単体で価値がある | なし |
| PR1b | ラウンド確定と結果編集の競合対策(§4.9)。共同管理と独立して単体で価値がある。PR1と並行可 | なし |
| PR2 | 認可基盤(`TournamentRole` / MEMBERアイテム / GSI1の`entityType`フィルタ / `loadOwner`・`loadMember` 分割 / 一覧マージ / `role`・`shareToken` の出し分け / メンバー一覧・取り消しAPI) | PR1 |
| PR3 | 招待リンク(発行・失効・プレビュー・承諾)+ ログイン後リダイレクト(§4.6) | PR2 |
| PR4 | フロントエンド(S14の本実装 + 設定画面の共同管理者カード + 一覧バッジ + ナビゲーション出し分け) | PR3 |
| PR5 | E2E(CP8)+ 受け入れケースのdone化仕上げ | PR4 |

設計ドキュメントの更新は §6 の対応関係に沿って**各PRで実装コードと同じPRで**行う(PR5にまとめない)。

## 8. リスク・未確定事項

- **MAINTAINERの自己離脱(セルフ削除)はスコープ外**。共同管理者の解除は
  `DELETE /tournaments/{id}/members/{memberId}`(OWNER専用、§4.1)のみで、MAINTAINER自身が
  離脱する導線は用意しない。抜けたい場合はOWNERに依頼する運用とする。設定画面(S09)を丸ごと
  OWNER専用にするという既存の決定(Issue #185)と一貫させ、権限行使の起点をOWNER1箇所に保つ
  ほうがシンプルであるため。撤回条件: 「OWNERに連絡が取れず抜けられない」という運用上の不満が
  実際に出た場合、`memberId`が自分自身の場合に限り`loadOwner`ではなく`loadMember`で許可する
  (対象は自分自身のみなので認可上のリスクは小さい)形で別Issueとして追加する
- **招待リンクは共通リンクである**。リンクを持つ第三者は人数枠が残っている限り誰でも共同管理者に
  なれる(なりすまし対策ではない)。緩和は「72時間の期限」「人数枠」「OWNERが一覧で承諾者の表示名を
  確認して取り消せること」の3点。個別リンク方式(1回使い切り)へ切り替える場合、INVITEアイテムを
  1本 → 複数(`INVITE#{inviteId}`)に変えるだけで済むよう、SKの形は将来変更可能な設計にしておく
- **枠は取り消しで戻らない**割り切りを採用している(§4.4)。運用で不便が出たら「取り消し時に
  `usedCount` を減らす」ではなく「OWNERが再発行する」で対応する。前者は同時承諾の上限保証を壊す
- **`Participant` / `Group` に楽観ロックを入れない**割り切り(§4.5)。複数人での同時編集が実運用で
  問題になったら別Issueで対応する。中止条件は「同一参加者の編集内容が黙って消える事象が実際に起きた場合」
- **`displayName` は個人情報**。OWNER向けレスポンス以外に出さない・ログに出さないことをレビュー観点に
  含める(`01_review_checklist.md` の個人情報項目で拾う)
- **GSI1 の相乗りが最大の実装リスク**。`entityType` フィルタの付け忘れは既存の大会一覧を壊す。
  PR2で「MEMBERアイテムが存在する状態で所有大会一覧が正しく返る」テストを必ず入れる
- 承諾処理の `TransactWriteItems` は2アイテム(INVITE + MEMBER)で上限100に余裕があるが、
  DynamoDB Local でのトランザクション挙動は実装前にスパイクで確認する。動かない場合の代替は
  「INVITEの条件付き更新を先に確定 → MEMBER作成が失敗したら `usedCount` を戻さず枠を1つ失う」
  (正確性を優先し、枠の損失は許容する)
- **ラウンド確定と結果編集の競合(§4.9)**: 結果編集側(MAINTAINER Aの訂正がBの確定に割り込む)は
  `TransactWriteItems`のConditionCheckで対策するが、逆方向(確定側の`undecided==0`判定が編集の
  割り込みで陳腐化する)は大規模大会での`TransactWriteItems`100アイテム上限のため本PRでは対策
  しない。撤回条件は§4.9のとおり、この方向のレースが実運用で実際に発生した場合
