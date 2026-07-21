import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { MatchResultControl } from '../../../src/components/features/round/MatchResultControl';
import { matchOf, summaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

const player1 = summaryOf({ id: 'p1', name: '架空 太郎', organization: null });
const player2 = summaryOf({ id: 'p2', name: '仮名 花子', organization: null });

describe('MatchResultControl', () => {
  it('SHR-AC-015: 片方のみ申告済みは「申告待ち」Chipと両者の申告内容を表示する', () => {
    renderWithProviders(
      <MatchResultControl
        match={matchOf({ player1, player2, player1ReportedResult: 'PLAYER1_WIN' })}
        editable
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('申告待ち')).toBeInTheDocument();
    expect(screen.getByText('架空 太郎の申告: 架空 太郎 の勝ち')).toBeInTheDocument();
    expect(screen.getByText('仮名 花子の申告: 未申告')).toBeInTheDocument();
  });

  it('SHR-AC-015: 申告が一致しない場合は「申告不一致」Chipと両者の具体的な申告内容を表示する', () => {
    renderWithProviders(
      <MatchResultControl
        match={matchOf({
          player1,
          player2,
          player1ReportedResult: 'PLAYER1_WIN',
          player2ReportedResult: 'PLAYER2_WIN',
        })}
        editable={false}
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('申告不一致')).toBeInTheDocument();
    expect(screen.getByText('架空 太郎の申告: 架空 太郎 の勝ち')).toBeInTheDocument();
    expect(screen.getByText('仮名 花子の申告: 仮名 花子 の勝ち')).toBeInTheDocument();
  });

  it('SHR-AC-015: 確定済みの結果と自己申告が食い違う場合は警告Chipと申告内容を表示する', () => {
    renderWithProviders(
      <MatchResultControl
        match={matchOf({
          player1,
          player2,
          result: 'PLAYER1_WIN',
          player1ReportedResult: 'PLAYER2_WIN',
          player2ReportedResult: 'PLAYER2_WIN',
        })}
        editable
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.getByText('確定結果と申告が異なる')).toBeInTheDocument();
    expect(screen.getByText('架空 太郎の申告: 仮名 花子 の勝ち')).toBeInTheDocument();
    expect(screen.getByText('仮名 花子の申告: 仮名 花子 の勝ち')).toBeInTheDocument();
  });

  it('確定済みで食い違いがない対局は申告内容を表示しない', () => {
    renderWithProviders(
      <MatchResultControl
        match={matchOf({
          player1,
          player2,
          result: 'PLAYER1_WIN',
          player1ReportedResult: 'PLAYER1_WIN',
          player2ReportedResult: 'PLAYER1_WIN',
        })}
        editable={false}
        multiGroup={false}
        saving={false}
        onInput={() => {}}
      />,
    );

    expect(screen.queryByText(/の申告:/)).not.toBeInTheDocument();
  });
});
