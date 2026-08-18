import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

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

  it('BYE・確定・対局中の3分岐で結果欄の最小高さが揃う(行高統一の回帰防止)', () => {
    const cases: Array<{ label: string; match: ReturnType<typeof matchOf>; editable: boolean }> = [
      { label: 'BYE', match: matchOf({ player1, player2: null, result: 'BYE' }), editable: false },
      {
        label: '確定',
        match: matchOf({ player1, player2, result: 'PLAYER1_WIN' }),
        editable: false,
      },
      { label: '対局中', match: matchOf({ player1, player2 }), editable: true },
    ];

    const minHeights = cases.map(({ label, match, editable }) => {
      const { container, unmount } = renderWithProviders(
        <MatchResultControl
          match={match}
          editable={editable}
          multiGroup={false}
          saving={false}
          onInput={() => {}}
        />,
      );
      const root = container.firstElementChild as HTMLElement;
      const target = match.result === 'BYE' ? root : (root.firstElementChild as HTMLElement);
      const minHeight = window.getComputedStyle(target).minHeight;
      unmount();
      return { label, minHeight };
    });

    expect(minHeights.every(({ minHeight }) => minHeight === '40px')).toBe(true);
  });

  it('結果セレクトの背景色をテーマ既定に統合した後も、結果を選ぶとonInputが呼ばれる(回帰防止)', async () => {
    const user = userEvent.setup();
    const onInput = vi.fn();
    renderWithProviders(
      <MatchResultControl
        match={matchOf({ player1, player2 })}
        editable
        multiGroup={false}
        saving={false}
        onInput={onInput}
      />,
    );

    await user.click(screen.getByRole('combobox'));
    await user.click(screen.getByRole('option', { name: '○ 仮名 花子 の勝ち' }));

    expect(onInput).toHaveBeenCalledWith('PLAYER2_WIN');
  });
});
