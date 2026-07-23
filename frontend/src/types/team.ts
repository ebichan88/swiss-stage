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
