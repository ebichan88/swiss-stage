import type { Group } from '../src/types/group';
import type { Participant, ParticipantSummary } from '../src/types/participant';
import type { Match, Round } from '../src/types/round';
import type { SharedTournament, SharedTournamentSummary } from '../src/types/shared';
import type { GroupStandings, Standing } from '../src/types/standing';
import type {
  BoardResult,
  GroupTeamStandings,
  Team,
  TeamMatch,
  TeamMember,
  TeamRound,
  TeamStanding,
  TeamSummary,
} from '../src/types/team';
import type { Tournament } from '../src/types/tournament';

/** テストデータビルダー(09_test_strategy.md §5)。個人名は架空の名前のみ使用する */

export function tournamentOf(overrides: Partial<Tournament> = {}): Tournament {
  return {
    id: '01TESTTOURNAMENT0000000000',
    name: '第1回テスト囲碁大会',
    gameType: 'GO',
    competitionType: 'INDIVIDUAL',
    teamSize: null,
    totalRounds: 5,
    currentRound: 0,
    status: 'PREPARING',
    visibility: 'PRIVATE',
    shareToken: null,
    resultInputEnabled: false,
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
    entryOrder: 1,
    status: 'ACTIVE',
    groupId: '01TESTGROUP000000000000000',
    ...overrides,
  };
}

export function summaryOf(overrides: Partial<ParticipantSummary> = {}): ParticipantSummary {
  return {
    id: '01TESTPARTICIPANT000000000',
    name: '架空 太郎',
    organization: 'テスト囲碁会',
    rank: 'DAN_3',
    entryOrder: 1,
    ...overrides,
  };
}

export function matchOf(overrides: Partial<Match> = {}): Match {
  return {
    id: '01TESTMATCH000000000000000',
    roundNumber: 1,
    tableNumber: 1,
    group: groupOf(),
    player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
    player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
    result: 'NONE',
    player1ReportedResult: 'NONE',
    player2ReportedResult: 'NONE',
    version: 0,
    ...overrides,
  };
}

export function roundOf(overrides: Partial<Round> = {}): Round {
  return {
    roundNumber: 1,
    status: 'PLAYING',
    matches: [matchOf()],
    ...overrides,
  };
}

export function sharedSummaryOf(
  overrides: Partial<SharedTournamentSummary> = {},
): SharedTournamentSummary {
  return {
    name: '第1回テスト囲碁大会',
    gameType: 'GO',
    competitionType: 'INDIVIDUAL',
    teamSize: null,
    totalRounds: 5,
    currentRound: 1,
    status: 'IN_PROGRESS',
    resultInputEnabled: false,
    ...overrides,
  };
}

export function sharedTournamentOf(overrides: Partial<SharedTournament> = {}): SharedTournament {
  return {
    tournament: sharedSummaryOf(),
    rounds: [roundOf()],
    standings: [groupStandingsOf()],
    teamRounds: null,
    teamStandings: null,
    ...overrides,
  };
}

export function groupOf(overrides: Partial<Group> = {}): Group {
  return {
    id: '01TESTGROUP000000000000000',
    name: 'A',
    ...overrides,
  };
}

/** 単一グループ大会の形(デフォルトグループの単一要素)。複数グループ大会は group を上書きする */
export function groupStandingsOf(overrides: Partial<GroupStandings> = {}): GroupStandings {
  return {
    group: groupOf(),
    standings: [standingOf()],
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

export function teamMemberOf(overrides: Partial<TeamMember> = {}): TeamMember {
  return {
    id: '01TESTMEMBER0000000000000',
    name: '架空 主将',
    rank: 'DAN_3',
    boardPosition: 1,
    ...overrides,
  };
}

export function teamOf(overrides: Partial<Team> = {}): Team {
  return {
    id: '01TESTTEAM00000000000000A',
    name: 'Aチーム',
    entryOrder: 1,
    status: 'ACTIVE',
    groupId: '01TESTGROUP000000000000000',
    members: [],
    ...overrides,
  };
}

export function teamSummaryOf(overrides: Partial<TeamSummary> = {}): TeamSummary {
  return {
    id: '01TESTTEAM00000000000000A',
    name: 'Aチーム',
    entryOrder: 1,
    ...overrides,
  };
}

export function boardResultOf(overrides: Partial<BoardResult> = {}): BoardResult {
  return {
    boardPosition: 1,
    result: 'NONE',
    team1ReportedResult: 'NONE',
    team2ReportedResult: 'NONE',
    ...overrides,
  };
}

export function teamMatchOf(overrides: Partial<TeamMatch> = {}): TeamMatch {
  return {
    id: '01TESTTEAMMATCH000000000A',
    roundNumber: 1,
    tableNumber: 1,
    group: groupOf(),
    team1: teamSummaryOf({ id: 't1', name: 'Aチーム' }),
    team2: teamSummaryOf({ id: 't2', name: 'Bチーム' }),
    boardResults: [boardResultOf({ boardPosition: 1 })],
    version: 0,
    ...overrides,
  };
}

export function teamRoundOf(overrides: Partial<TeamRound> = {}): TeamRound {
  return {
    roundNumber: 1,
    status: 'PLAYING',
    matches: [teamMatchOf()],
    ...overrides,
  };
}

export function teamStandingOf(overrides: Partial<TeamStanding> = {}): TeamStanding {
  return {
    rank: 1,
    team: teamSummaryOf(),
    wins: 3,
    losses: 1,
    sos: 10,
    sosos: 24.5,
    hadBye: false,
    ...overrides,
  };
}

export function groupTeamStandingsOf(
  overrides: Partial<GroupTeamStandings> = {},
): GroupTeamStandings {
  return {
    group: groupOf(),
    standings: [teamStandingOf()],
    ...overrides,
  };
}
