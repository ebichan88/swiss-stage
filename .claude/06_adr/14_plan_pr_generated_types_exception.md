# 14. `schema/openapi.yaml` を変更するPlan PRでは生成型定義(`api.d.ts`)も例外に含める

- Status: Accepted
- Issue: #185
- Date: 2026-08-17

## 1. 文脈

`CLAUDE.md` #18・`04_development_process.md` §5.1・`03_feature_plan_template.md` §6 は、Plan PR
の「コード0行」原則の例外を**対象画面の `.stories.tsx` のみ**と定めている(`CLAUDE.md` #18 は
「のみ例外として許可する」と明示)。

Issue #185(大会の共同管理)の Plan PR で `schema/openapi.yaml` を変更したところ、`frontend/`
の CI(`ci.yml` の `frontend` ジョブ)に含まれる「生成型の鮮度チェック」ステップ

```yaml
- name: 生成型の鮮度チェック(schema変更にgenerate:apiが追随しているか)
  run: |
    pnpm run generate:api
    git diff --exit-code src/types/generated/
```

に抵触することが判明した。このステップはブランチ種別を問わず実行される必須ゲートで、
`schema/openapi.yaml` を変更したPRでは `frontend/src/types/generated/api.d.ts`
(`pnpm run generate:api` の出力)が常に追随していないとCIが失敗する。つまり
「Plan PRで `schema/openapi.yaml` を先に更新する」という既存ルール(`04_development_process.md` §2・
`03_feature_plan_template.md` §4)と、「Plan PRの例外は `.stories.tsx` のみ」という現行の文言は、
`schema/openapi.yaml` に変更がある場合には両立しない。

## 2. 決定

**`schema/openapi.yaml` を変更する Plan PR では、`pnpm run generate:api` を実行した結果
(`frontend/src/types/generated/api.d.ts`)もコード0行原則の例外に含める。** 手で編集した
実装コードではなく、コミット済みの `schema/openapi.yaml` から一意に導出される生成物である点は
`.stories.tsx` の例外根拠(実装前のUI合意に必要な最小限のコード)とは異なる理由だが、「例外を
許可する対象は `.stories.tsx` のみ」という現行の文言はこのケースを正しく記述できていなかった。

この決定は `CLAUDE.md` #18・`04_development_process.md` §5.1・`03_feature_plan_template.md` §6
の該当箇所を修正する。同じく「`.stories.tsx` のみ例外」と書いていた `01_development_docs/11_cicd_design.md`
(Plan PRブランチで `ai-review.yml`/`ai-qa.yml` をスキップする理由の説明)・`04_quality/01_review_checklist.md`
(plan-reviewerエージェント管轄の説明)・`commands/plan.md`(`/plan` の実行手順そのもの)も同じ内容の
記述を持っていたため、あわせて修正する。特に `commands/plan.md` は次回以降の `/plan` 実行時に
実際に読まれる手順であり、ここが古いままだと以後のPlan PRでこの決定が再現されない。`agents/planner.md`
(無人実行版 `/plan`)にも `schema/openapi.yaml` 更新時の `generate:api` 実行を追記する
(ストーリー例外(§5.1.1)は判断を伴うため引き続き無人実行の対象外だが、生成型定義の例外(§5.1.2)は
機械的な出力であり対象内とする)。

**このADRは例外としてPlan PR内(本PR)で `Accepted` にし、上記3文書もPlan PR内で更新した。**
通常、Plan PRで新規作成するADRは `Proposed` のまま実装PRで `Accepted` にする
(`04_development_process.md` §4)が、本ADRの決定内容は「Plan PRの作り方」そのものであり、
この決定を前提に本Plan PR自身が `frontend/src/types/generated/api.d.ts` を §6 の更新対象に
含めている。決定を `Proposed` のまま実装PR送りにすると、Plan PR自身が「まだ受理されていない決定」
を既成事実として使うという矛盾が生じるため、この場合に限り即時反映する
(`04_development_process.md` §4の例外規定。`06_adr/12_story_first_existing_page_placeholder.md`
と同じ扱い)。

## 3. 却下した案

- **`api.d.ts` の再生成をPlan PRから外し、実装PRで行う** → 却下。`schema/openapi.yaml` の変更を
  Plan PRに含める以上、CIの鮮度チェックが必ず落ちる。Plan PR自体がマージ不能になる
  (`autofix` ジョブが自動修正コミットを積む余地はあるが、それに頼る運用を前提にするより
  最初から正しい状態でコミットする方が素直)
- **鮮度チェックをPlan PRブランチ(`feature/plan-*`)では実行しないようCIを変更する** → 却下。
  Plan PR時点で `schema/openapi.yaml` とフロントの型が食い違ったままマージされると、レビュアーが
  スキーマの実際の使い勝手(生成される型の形)を確認できなくなる。チェック自体を弱めるより、
  生成物を許容する方が安全
- **`.stories.tsx` の例外文言はそのままにし、Plan PRの本文だけで`api.d.ts`同梱を都度説明する** →
  却下。design-reviewerが指摘したとおり、規約の文言と実際の運用が食い違ったまま放置すると、
  次にPlan PRで `schema/openapi.yaml` を変更する人が同じ矛盾に毎回突き当たる
  (`CLAUDE.md` #17 の「毎回確認しないと使えないものは採用しない」と同じ理由で、規約は
  実態に揃えておく)

## 4. 結果

**得るもの**: `schema/openapi.yaml` を変更するPlan PRが、既存のCI鮮度チェックと矛盾なくマージできる。
以後同様のPlan PRで同じ疑問が再発しない。

**引き受けるトレードオフ**: Plan PRの「コード0行」原則の例外が2種類(`.stories.tsx` / 生成型定義)
になり、原則の説明がわずかに複雑になる。ただし後者は人間が手で書くコードではなく機械的な生成物
であり、レビューの負担は実質増えない。

**撤回条件**: 生成型定義の同梱がレビューで見落とされやすい・ノイズになるという運用上の問題が
出た場合、鮮度チェック自体をPlan PRブランチで緩めるかどうかを含めて再検討する。
