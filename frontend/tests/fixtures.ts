import type { Participant, ParticipantSummary } from '../src/types/participant';
import type { Match } from '../src/types/round';
import type { Standing } from '../src/types/standing';
import type { Tournament } from '../src/types/tournament';

/** テストデータビルダー(09_test_strategy.md §5)。個人名は架空の名前のみ使用する */

export function tournamentOf(overrides: Partial<Tournament> = {}): Tournament {
  return {
    id: '01TESTTOURNAMENT0000000000',
    name: '第1回テスト囲碁大会',
    gameType: 'GO',
    totalRounds: 5,
    currentRound: 0,
    status: 'PREPARING',
    visibility: 'PRIVATE',
    shareToken: null,
    version: 1,
    createdAt: '2026-07-12T10:00:00+09:00',
    updatedAt: '2026-07-12T10:00:00+09:00',
    ...overrides,
  };
}

export function participantOf(overrides: Partial<Participant> = {}): Participant {
  return {
    id: '01TESTPARTICIPANT000000000',
    name: '架空 太郎',
    organization: 'テスト囲碁会',
    rank: 'DAN_3',
    seedOrder: 1,
    status: 'ACTIVE',
    ...overrides,
  };
}

export function summaryOf(overrides: Partial<ParticipantSummary> = {}): ParticipantSummary {
  return {
    id: '01TESTPARTICIPANT000000000',
    name: '架空 太郎',
    organization: 'テスト囲碁会',
    ...overrides,
  };
}

export function matchOf(overrides: Partial<Match> = {}): Match {
  return {
    id: '01TESTMATCH000000000000000',
    roundNumber: 1,
    tableNumber: 1,
    player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
    player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
    result: 'NONE',
    version: 0,
    ...overrides,
  };
}

export function standingOf(overrides: Partial<Standing> = {}): Standing {
  return {
    rank: 1,
    participant: summaryOf(),
    wins: 3,
    losses: 1,
    sos: 10,
    sosos: 24.5,
    hadBye: false,
    ...overrides,
  };
}
