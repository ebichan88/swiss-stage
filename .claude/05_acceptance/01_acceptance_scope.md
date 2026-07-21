# 受け入れケース台帳(Acceptance Scope)

運用ルール・ID体系・優先度の定義は `00_acceptance_policy.md` を参照。
各ケースは一文のみ。仕様の詳細は設計ドキュメント(特に `05_swiss_pairing_algorithm.md` / `03_api_design.md` / `13_security_design.md`)が正。

## やらないこと(out-of-scope)

以下は受け入れケースを作らない(QAエージェントは不足として提案しない)。出典: `.claude/00_project/02_inception_deck.md` §3、`03_api_design.md` §4。

- 団体戦・エントリーサイト・決済・ネイティブアプリ・囲碁将棋以外の競技・リアルタイム同時編集
- 年間ランキング・QRコード受付・棋譜管理・API公開・参加者プロフィール・通算戦績(あとで決めること)
- 一覧APIのページネーション(MVPは最大300名のため不要)

## AUTH: 認証

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| AUTH-AC-001 | P0 | 未認証の保護APIアクセスは401 UNAUTHORIZEDを統一フォーマットで返す(X-Request-Id付き) | done | AuthApiTest |
| AUTH-AC-002 | P1 | test-loginでHttpOnlyのセッションCookieが発行される(local/testプロファイル限定・本番に存在しない) | done | AuthApiTest |
| AUTH-AC-003 | P0 | /auth/login はGoogleのOAuth2認可フローへリダイレクトする | done | AuthApiTest |
| AUTH-AC-004 | P1 | logoutでセッションCookieが失効する | done | AuthApiTest |
| AUTH-AC-005 | P0 | 改ざん・不正なセッションCookieは未認証として扱う | done | AuthApiTest |

## TRN: 大会

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| TRN-AC-001 | P1 | 大会を作成すると201で統一フォーマットのDTO(PREPARING・currentRound=0・version=1)が返る | done | TournamentApiTest |
| TRN-AC-002 | P1 | 入力バリデーション違反は400 VALIDATION_ERROR + detailsで返る | done | TournamentApiTest |
| TRN-AC-003 | P1 | 自分の大会の一覧・詳細を取得できる | done | TournamentApiTest |
| TRN-AC-004 | P0 | 他人の大会は404 TOURNAMENT_NOT_FOUNDになり存在を漏らさない | done | TournamentApiTest |
| TRN-AC-005 | P0 | 不正形式のID・パストラバーサルは404/400で拒否する(キー組み立てへの入力混入禁止) | done | TournamentApiTest |
| TRN-AC-006 | P1 | 更新はversion一致で成功しversionが1進む | done | TournamentApiTest |
| TRN-AC-007 | P0 | version不一致の更新は409 CONFLICTになる(黙って上書きしない) | done | TournamentApiTest |
| TRN-AC-008 | P1 | 参加者2名未満の大会は開始できない(409 INVALID_STATE) | done | TournamentApiTest |
| TRN-AC-009 | P1 | 大会を削除すると204で消え、以後404になる | done | TournamentApiTest |
| TRN-AC-010 | P0 | 未認証の大会APIアクセスはすべて401になる | done | TournamentApiTest |

## PTC: 参加者

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| PTC-AC-001 | P1 | 参加者を追加するとエントリー順(entryOrder)が自動採番される | done | ParticipantApiTest |
| PTC-AC-002 | P1 | CSVインポート(UTF-8)で全行取り込め、段級位表記が正しくenumに変換される | done | ParticipantApiTest |
| PTC-AC-003 | P1 | CSVインポート(Shift_JIS)も文字コード自動判定で取り込める | done | ParticipantApiTest |
| PTC-AC-004 | P1 | CSVの行エラーは行番号付きdetailsの400になり、1件も取り込まれない | done | ParticipantApiTest |
| PTC-AC-005 | P1 | ヘッダー行が不正なCSVは400になる | done | ParticipantApiTest |
| PTC-AC-006 | P0 | 大会開始後の参加者追加・削除は409になる | done | ParticipantApiTest |
| PTC-AC-007 | P0 | 棄権(status=WITHDRAWN)は大会開始後でもできる | done | ParticipantApiTest |
| PTC-AC-008 | P2 | clearRank=trueで棋力を未入力に戻せる(rankとの同時指定は400) | done | ParticipantApiTest |
| PTC-AC-009 | P1 | 部分更新で未指定の項目(グループ割当等)は失われない | done | ParticipantApiTest |

## GRP: グループ

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| GRP-AC-001 | P1 | 大会作成時にデフォルトグループ「A」が自動作成される | done | GroupApiTest |
| GRP-AC-002 | P1 | グループの作成・一覧(作成順)・改名・削除ができる | done | GroupApiTest |
| GRP-AC-003 | P1 | 重複名のグループ作成・改名は400になる(同名のままの改名は可) | done | GroupApiTest |
| GRP-AC-004 | P1 | 最後の1グループは削除できず、グループ数の上限は10 | done | GroupApiTest |
| GRP-AC-005 | P1 | 存在しないグループへの操作は404 GROUP_NOT_FOUNDになる | done | GroupApiTest |
| GRP-AC-006 | P1 | 段級位の自動振り分けは強い順に均等分割する(端数は先頭グループ) | done | GroupApiTest |
| GRP-AC-007 | P1 | 自動振り分けは棄権中の参加者の割当を変更しない | done | GroupApiTest |
| GRP-AC-008 | P1 | 参加者を個別にグループ変更でき、未知グループの指定は400になる | done | GroupApiTest |
| GRP-AC-009 | P1 | グループ削除時、割当済み参加者は直前のグループへ移動する | done | GroupApiTest |
| GRP-AC-010 | P1 | 参加者追加はグループ省略時に先頭グループへ割り当てられる | done | GroupApiTest |
| GRP-AC-011 | P0 | 空または2名未満のグループがあると大会を開始できない(409) | done | GroupApiTest |
| GRP-AC-012 | P0 | 大会開始後のグループ作成・削除・自動振り分け・割当変更は409になる | done | GroupApiTest |
| GRP-AC-013 | P0 | ラウンド生成はグループごとに独立し、卓番号はグループ内1始まり、BYEはグループ内で発生する | done | GroupApiTest |
| GRP-AC-014 | P0 | 順位表はグループごとに独立に計算される(グループ内順位1始まり) | done | GroupApiTest |
| GRP-AC-015 | P1 | CSVのグループ列(4列)で割当付きインポートでき、空欄は先頭グループになる | done | GroupApiTest |
| GRP-AC-016 | P1 | CSVの未知グループ名は行番号付きエラーになり1件も取り込まれない | done | GroupApiTest |

## RND: ラウンド・対局・順位

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| RND-AC-001 | P0 | ラウンド生成で全ACTIVE参加者が1回ずつ対局に割り当てられる(status=PLAYING) | done | RoundApiTest |
| RND-AC-002 | P0 | 現在ラウンドが未確定のうちは次ラウンドを生成できない(409) | done | RoundApiTest |
| RND-AC-003 | P0 | 申告・入力が一切ない対局が1件でも残るラウンドは確定できない(409) | done | RoundApiTest |
| RND-AC-004 | P1 | 全対局入力後の確定でラウンドがCONFIRMEDになり、次ラウンドを生成できる | done | RoundApiTest |
| RND-AC-005 | P0 | 次ラウンドで再戦(同一ペアの再対局)が発生しない | done | RoundApiTest |
| RND-AC-006 | P1 | ラウンド一覧が対局付きで取得できる | done | RoundApiTest |
| RND-AC-007 | P0 | 順位表が仕様(05_swiss_pairing_algorithm.md §3)の順で計算されて返る | done | RoundApiTest |
| RND-AC-008 | P1 | 結果入力(運営者)で対局結果が更新されversionが進む | done | RoundApiTest |
| RND-AC-009 | P0 | 古いversionでの結果入力は409 CONFLICTになる(後勝ちしない) | done | RoundApiTest |
| RND-AC-010 | P0 | 確定済みラウンドの結果は変更できない(409) | done | RoundApiTest |
| RND-AC-011 | P1 | 不正な結果値(BYE等)の入力は400になる | done | RoundApiTest |
| RND-AC-012 | P1 | 片方のみ申告・申告不一致の対局が残っていてもラウンド確定はブロックしない(警告のみ・運営者の裁量) | done | RoundApiTest |

## SHR: 共有(トークン)

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| SHR-AC-001 | P0 | 共有トークンは運営者のみ発行・再発行できる(他人は404・未認証は401) | done | SharedApiTest |
| SHR-AC-002 | P0 | 再発行すると旧トークンは即時無効(403)になる | done | SharedApiTest |
| SHR-AC-003 | P1 | 共有ページ集約は大会・ラウンド・順位を返す | done | SharedApiTest |
| SHR-AC-004 | P0 | 共有レスポンスにshareToken・ownerSubを含めない | done | SharedApiTest |
| SHR-AC-005 | P0 | 無効・不明・形式不正トークンと非公開(PRIVATE)は同一の403で大会の存在を漏らさない | done | SharedApiTest |
| SHR-AC-006 | P0 | トークン経由の結果入力は大会設定(resultInputEnabled)で許可時のみ可能(未許可は403) | done | SharedApiTest |
| SHR-AC-007 | P0 | トークン経由の結果入力もversion競合409・確定後409の制約に従う | done | SharedApiTest |
| SHR-AC-008 | P1 | キャッシュ済みの共有ページも結果入力・確定・参加者改名後は即時反映される(evict) | done | SharedApiTest |
| SHR-AC-009 | P2 | 共有APIはIPベースのレート制限超過で429を統一フォーマットで返す | done | SharedRateLimitApiTest |
| SHR-AC-010 | P2 | 共有ページの参加者要約(ParticipantSummary)にrank・entryOrderを含む(戦績一覧表での参照用) | done | SharedApiTest |
| SHR-AC-011 | P0 | トークン経由の結果入力は「自分がplayer1/player2のどちらか」の申告として記録され、片方の申告のみでは対局結果は確定しない | done | SharedApiTest |
| SHR-AC-012 | P0 | 両者の申告が一致すると対局結果が自動確定し、一致しない場合は結果を確定せず双方の申告内容が参照できる | done | SharedApiTest |
| SHR-AC-013 | P0 | 運営者が直接確定した結果は、その後の参加者の自己申告(一致・不一致問わず)で上書きされない | done | SharedApiTest |
| SHR-AC-014 | P0 | 自己申告一致で自動確定した結果も、その後の自己申告の変更(一致・不一致問わず)で上書き・巻き戻りしない | done | ModelTest |
| SHR-AC-015 | P1 | 申告不一致・確定後の食い違いは、状態語だけでなく誰が何を申告したかが運営者画面・共有画面の両方に具体的に表示される | done | MatchResultControl.test / SharedResultPage.test |

## SPA: SPA配信

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| SPA-AC-001 | P1 | SPAルートへの直アクセスはindex.htmlを返す(no-cache) | done | SpaFallbackApiTest |
| SPA-AC-002 | P2 | ルート(/)とindex.html自体もSPAを返す | done | SpaFallbackApiTest |
| SPA-AC-003 | P2 | ハッシュ付きアセットは1年+immutableでキャッシュされる | done | SpaFallbackApiTest |
| SPA-AC-004 | P1 | 未知のAPIパスはindex.htmlにフォールバックせず404になる | done | SpaFallbackApiTest |

## E2E: 一気通貫(クリティカルパス)

定義の詳細は `12_e2e_test_design.md`。

| ID | P | 受け入れ基準 | Status | 検証 |
|----|---|------------|--------|------|
| E2E-AC-001 | P0 | CP1: ログインから順位表まで大会運営を一気通貫できる | done | cp1-tournament-flow |
| E2E-AC-002 | P1 | CP1補: Shift_JISのCSVもインポートできる | done | cp1-tournament-flow |
| E2E-AC-003 | P0 | CP2: 参加者がスマホの共有ページから結果送信でき運営者画面に反映される | done | cp2-shared-mobile |
| E2E-AC-004 | P0 | CP3: 奇数人数の大会で全ラウンドを通してBYEが重複しない | done | cp3-bye |
| E2E-AC-005 | P1 | CP4: 異常系(未入力確定の警告・競合409表示・無効トークンのエラーページ)がUIで扱える | done | cp4-errors |
| E2E-AC-006 | P0 | CP5: グループ大会をグループ独立で運営できる | done | cp5-groups |
