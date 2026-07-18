import type { MatchResult, RoundStatus } from './enums';
import type { Group } from './group';
import type { ParticipantSummary } from './participant';

/**
 * 対局。player2 が null なら不戦勝(BYE)。
 * group はグループ大会での帰属(null = グループなし大会)。
 * tableNumber はグループ大会ではグループ内で1始まり(表示は「A-1」形式)
 */
export interface Match {
  id: string;
  roundNumber: number;
  tableNumber: number;
  group: Group | null;
  player1: ParticipantSummary;
  player2: ParticipantSummary | null;
  result: MatchResult;
  version: number;
}

export interface Round {
  roundNumber: number;
  status: RoundStatus;
  matches: Match[];
}

/**
 * POST /rounds(組み合わせ生成)のレスポンス。
 * relaxations が空でなければUIに警告を表示する(REMATCH / BYE_REPEAT / SAME_ORGANIZATION)
 */
export interface GeneratedRound {
  round: Round;
  relaxations: string[];
}

/** PUT /matches/{mid}/result(backend: InputResultRequest)。versionは楽観ロック用 */
export interface InputResultInput {
  result: MatchResult;
  version: number;
}
