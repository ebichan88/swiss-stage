import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TeamMatchResultControl } from '../../../src/components/features/team/TeamMatchResultControl';
import { boardResultOf, teamMatchOf, teamSummaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

const team1 = teamSummaryOf({ id: 't1', name: 'Aチーム' });
const team2 = teamSummaryOf({ id: 't2', name: 'Bチーム' });

describe('TeamMatchResultControl', () => {
  it('TEAM-AC-019: 片方のみ申告済みのボードは「申告待ち」Chipと両者の申告内容を表示する', () => {
    renderWithProviders(
      <TeamMatchResultControl
        match={teamMatchOf({
          team1,
          team2,
          boardResults: [boardResultOf({ boardPosition: 1, team1ReportedResult: 'PLAYER1_WIN' })],
        })}
        editable
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('申告待ち')).toBeInTheDocument();
    expect(screen.getByText('Aチームの申告: Aチームの勝ち')).toBeInTheDocument();
    expect(screen.getByText('Bチームの申告: 未申告')).toBeInTheDocument();
  });

  it('TEAM-AC-019: ボードの申告が一致しない場合は「申告不一致」Chipと両者の具体的な申告内容を表示する', () => {
    renderWithProviders(
      <TeamMatchResultControl
        match={teamMatchOf({
          team1,
          team2,
          boardResults: [
            boardResultOf({
              boardPosition: 1,
              team1ReportedResult: 'PLAYER1_WIN',
              team2ReportedResult: 'PLAYER2_WIN',
            }),
          ],
        })}
        editable={false}
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('申告不一致')).toBeInTheDocument();
    expect(screen.getByText('Aチームの申告: Aチームの勝ち')).toBeInTheDocument();
    expect(screen.getByText('Bチームの申告: Bチームの勝ち')).toBeInTheDocument();
  });

  it('一部ボードのみ不一致でも、他ボードは影響を受けず個別に表示される', () => {
    renderWithProviders(
      <TeamMatchResultControl
        match={teamMatchOf({
          team1,
          team2,
          boardResults: [
            boardResultOf({
              boardPosition: 1,
              result: 'PLAYER1_WIN',
              team1ReportedResult: 'PLAYER1_WIN',
              team2ReportedResult: 'PLAYER1_WIN',
            }),
            boardResultOf({
              boardPosition: 2,
              team1ReportedResult: 'PLAYER1_WIN',
              team2ReportedResult: 'PLAYER2_WIN',
            }),
          ],
        })}
        editable={false}
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    // 主将戦(自己申告一致で自動確定)は「申告済み」Chipのみで申告詳細は出さない
    expect(screen.getByText('主将')).toBeInTheDocument();
    expect(screen.getByText('Aチームの勝ち')).toBeInTheDocument();
    expect(screen.getByText('申告済み')).toBeInTheDocument();
    // 副将戦(不一致)のみ警告を出す
    expect(screen.getByText('副将')).toBeInTheDocument();
    expect(screen.getByText('申告不一致')).toBeInTheDocument();
  });

  it('TEAM-AC-021: 運営者が申告なしで直接確定したボードは「申告済み」Chipを表示しない', () => {
    renderWithProviders(
      <TeamMatchResultControl
        match={teamMatchOf({
          team1,
          team2,
          boardResults: [boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' })],
        })}
        editable={false}
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('Aチームの勝ち')).toBeInTheDocument();
    expect(screen.queryByText('申告済み')).not.toBeInTheDocument();
  });

  it('不戦勝(team2がnull)は「不戦勝」表示のみで入力コントロールを出さない', () => {
    renderWithProviders(
      <TeamMatchResultControl
        match={teamMatchOf({ team1, team2: null, boardResults: [] })}
        editable
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('不戦勝')).toBeInTheDocument();
  });
});
