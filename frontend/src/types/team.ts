import type { components } from './generated/api';

/** 団体戦のチーム。membersは埋め込み(役割・棋力込み) */
export type Team = components['schemas']['Team'];

/** チームメンバー。boardPosition: 1..teamSize(必須ポジション)またはnull(補欠) */
export type TeamMember = components['schemas']['TeamMember'];

/** POST /teams。groupId 省略時は先頭グループに割当 */
export type CreateTeamInput = components['schemas']['CreateTeamRequest'];

/** PATCH /teams/{tid}。未指定の項目は変更しない */
export type UpdateTeamInput = components['schemas']['UpdateTeamRequest'];

/** POST /teams/{tid}/members。boardPosition省略は補欠として追加 */
export type AddTeamMemberInput = components['schemas']['AddTeamMemberRequest'];

/** PATCH /teams/{tid}/members/{memberId}。未指定の項目は変更しない */
export type UpdateTeamMemberInput = components['schemas']['UpdateTeamMemberRequest'];

/** チームCSVインポート結果(全行正常時のみ取り込む) */
export type TeamCsvImportResult = components['schemas']['TeamCsvImportResult'];

/** 組み合わせ・戦績一覧・順位表で使う表示用の要約。メンバーの個人名は含めない */
export type TeamSummary = components['schemas']['TeamSummary'];

/**
 * 主将戦・副将戦…の1ボード分の結果。resultはteam1視点。
 * team1ReportedResult/team2ReportedResultは共有トークン経由の自己申告(NONE=未申告)
 */
export type BoardResult = components['schemas']['BoardResult'];

/** 団体戦対局。team2 が null なら不戦勝(BYE) */
export type TeamMatch = components['schemas']['TeamMatch'];

export type TeamRound = components['schemas']['TeamRound'];

/**
 * POST /team-rounds(組み合わせ生成)のレスポンス。
 * relaxations が空でなければUIに警告を表示する(REMATCH / BYE_REPEAT)
 */
export type GeneratedTeamRound = components['schemas']['GeneratedTeamRound'];

/** PUT /team-matches/{mid}/result(運営者による直接確定)。versionは楽観ロック用 */
export type InputTeamMatchResultInput = components['schemas']['InputTeamMatchResultRequest'];

/**
 * PUT /shared/{token}/team-matches/{mid}/result(トークン経由の自己申告)。
 * reportedBy側(team1/team2)のボード配列として送信する
 */
export type ReportTeamMatchResultInput = components['schemas']['ReportTeamMatchResultRequest'];

/** 団体戦順位表の1行。rank は同順位あり(1,2,2,4 形式)。グループ大会ではグループ内順位 */
export type TeamStanding = components['schemas']['TeamStanding'];

/** グループ別チーム順位表。グループごとに1要素(group は常に非null) */
export type GroupTeamStandings = components['schemas']['GroupTeamStandings'];
