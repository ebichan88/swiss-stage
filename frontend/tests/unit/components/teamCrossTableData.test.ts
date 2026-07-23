import { describe, expect, it } from 'vitest';

import { buildTeamCrossTableRows } from '../../../src/components/features/team/teamCrossTableData';
import {
  boardResultOf,
  groupOf,
  teamMatchOf,
  teamRoundOf,
  teamStandingOf,
  teamSummaryOf,
} from '../../fixtures';

describe('buildTeamCrossTableRows', () => {
  it('行はentryOrder昇順に並び、対局は自分の視点で相手とマーク・ボード内訳を解決する', () => {
    const teamA = teamSummaryOf({ id: 't1', name: 'Aチーム', entryOrder: 2 });
    const teamB = teamSummaryOf({ id: 't2', name: 'Bチーム', entryOrder: 1 });
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [
          teamMatchOf({
            team1: teamA,
            team2: teamB,
            boardResults: [
              boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 2, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 3, result: 'PLAYER2_WIN' }),
            ],
          }),
        ],
      }),
    ];
    const standings = [teamStandingOf({ team: teamA }), teamStandingOf({ team: teamB })];

    const rows = buildTeamCrossTableRows(rounds, standings);

    expect(rows.map((r) => r.standing.team.id)).toEqual(['t2', 't1']);
    // Bチーム(team2)視点: 相手はAチーム、2-1で負け
    expect(rows[0].cells[0]).toEqual({
      opponent: teamA,
      isBye: false,
      mark: '●',
      breakdown: '1-2',
    });
    // Aチーム(team1)視点: 相手はBチーム、2-1で勝ち
    expect(rows[1].cells[0]).toEqual({
      opponent: teamB,
      isBye: false,
      mark: '○',
      breakdown: '2-1',
    });
  });

  it('BYEの対局は相手null・isBye trueになる', () => {
    const teamA = teamSummaryOf({ id: 't1', entryOrder: 1 });
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [teamMatchOf({ team1: teamA, team2: null, boardResults: [] })],
      }),
    ];
    const standings = [teamStandingOf({ team: teamA })];

    const rows = buildTeamCrossTableRows(rounds, standings);

    expect(rows[0].cells[0]).toEqual({ opponent: null, isBye: true, mark: null, breakdown: null });
  });

  it('一部ボードのみ決着している対局はマーク・内訳ともnullになる(全ボード決着前)', () => {
    const teamA = teamSummaryOf({ id: 't1', entryOrder: 1 });
    const teamB = teamSummaryOf({ id: 't2', entryOrder: 2 });
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [
          teamMatchOf({
            team1: teamA,
            team2: teamB,
            boardResults: [
              boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 2, result: 'NONE' }),
              boardResultOf({ boardPosition: 3, result: 'NONE' }),
            ],
          }),
        ],
      }),
    ];
    const standings = [teamStandingOf({ team: teamA })];

    const rows = buildTeamCrossTableRows(rounds, standings);

    expect(rows[0].cells[0]).toEqual({
      opponent: teamB,
      isBye: false,
      mark: null,
      breakdown: null,
    });
  });

  it('複数グループの対局が混在していてもチームIDで解決できる(グループ絞り込みは呼び出し側が行う前提)', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    const bTeam1 = teamSummaryOf({ id: 'b1', entryOrder: 1 });
    const bTeam2 = teamSummaryOf({ id: 'b2', entryOrder: 2 });
    const win3 = [
      boardResultOf({ boardPosition: 1, result: 'PLAYER2_WIN' }),
      boardResultOf({ boardPosition: 2, result: 'PLAYER2_WIN' }),
      boardResultOf({ boardPosition: 3, result: 'PLAYER2_WIN' }),
    ];
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [
          teamMatchOf({ group: groupA, team1: teamSummaryOf({ id: 'a1' }) }),
          teamMatchOf({ group: groupB, team1: bTeam1, team2: bTeam2, boardResults: win3 }),
        ],
      }),
    ];
    const standings = [teamStandingOf({ team: bTeam1 }), teamStandingOf({ team: bTeam2 })];

    const rows = buildTeamCrossTableRows(rounds, standings);

    expect(rows).toHaveLength(2);
    expect(rows[0].cells[0].mark).toEqual('●');
    expect(rows[1].cells[0].mark).toEqual('○');
  });
});
