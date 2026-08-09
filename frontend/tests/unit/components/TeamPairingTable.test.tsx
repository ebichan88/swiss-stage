import { screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TeamPairingTable } from '../../../src/components/features/team/TeamPairingTable';
import { boardResultOf, groupOf, teamMatchOf, teamSummaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

/** `theme.breakpoints.down('sm')` の一致/不一致を固定するmatchMediaモック */
function mockMatchMedia(matchesMobile: boolean) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: query.includes('max-width') ? matchesMobile : !matchesMobile,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
}

describe('TeamPairingTable', () => {
  const originalMatchMedia = window.matchMedia;

  afterEach(() => {
    vi.restoreAllMocks();
    window.matchMedia = originalMatchMedia;
  });

  it('TRN-AC-018: スマホ表示の卓番号は共有ページの卓番号タイル(「卓」キャプション付き)ではなく、プレーンなテキストのまま表示する', () => {
    mockMatchMedia(true);
    renderWithProviders(
      <TeamPairingTable
        matches={[teamMatchOf({ id: 'm1', tableNumber: 3 })]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByText('卓')).not.toBeInTheDocument();
  });

  it('TRN-AC-028: テーブルをoutlined枠線+角丸のPaperで囲む(ページ背景と偶数行の背景色が同化する境界不明瞭の回帰防止)', () => {
    renderWithProviders(
      <TeamPairingTable
        matches={[teamMatchOf({ id: 'm1', tableNumber: 1 })]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const paper = screen.getByRole('table').closest('.MuiPaper-root');
    expect(paper).toHaveClass('MuiPaper-outlined');
  });

  it('卓番号とチーム名を表示し、全ボード決着済みの対局は○/●が付く(個人名は含めない)', () => {
    const teamA = teamSummaryOf({ id: 't1', name: 'Aチーム' });
    const teamB = teamSummaryOf({ id: 't2', name: 'Bチーム' });
    renderWithProviders(
      <TeamPairingTable
        matches={[
          teamMatchOf({
            id: 'm1',
            tableNumber: 1,
            team1: teamA,
            team2: teamB,
            boardResults: [
              boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 2, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 3, result: 'PLAYER2_WIN' }),
            ],
          }),
          teamMatchOf({
            id: 'm2',
            tableNumber: 2,
            team1: teamSummaryOf({ id: 't3', name: 'Cチーム' }),
            team2: null,
            boardResults: [],
          }),
        ]}
        editable={false}
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    // header(0) + m1のボード3件(1-3行) + m2(4行目)
    expect(rows[1]).toHaveTextContent('○ Aチーム');
    expect(rows[1]).toHaveTextContent('● Bチーム');
    expect(rows[4]).toHaveTextContent('Cチーム');
    expect(within(rows[4]).getByText('(不戦勝)')).toBeInTheDocument();
    expect(screen.queryByText('山田太郎')).not.toBeInTheDocument();
  });

  it('TEAM-AC-019: ボード単位の申告不一致は「結果」列と別の「申告ステータス」列に、そのボードの行として表示される', () => {
    const teamA = teamSummaryOf({ id: 't1', name: 'Aチーム' });
    const teamB = teamSummaryOf({ id: 't2', name: 'Bチーム' });
    renderWithProviders(
      <TeamPairingTable
        matches={[
          teamMatchOf({
            id: 'm1',
            tableNumber: 1,
            team1: teamA,
            team2: teamB,
            boardResults: [
              boardResultOf({ boardPosition: 1 }),
              boardResultOf({
                boardPosition: 2,
                team1ReportedResult: 'PLAYER1_WIN',
                team2ReportedResult: 'PLAYER2_WIN',
              }),
            ],
          }),
        ]}
        editable={false}
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const headerCells = screen.getAllByRole('columnheader');
    expect(headerCells.map((cell) => cell.textContent)).toEqual([
      '卓',
      'チーム1',
      'チーム2',
      '結果',
      '申告ステータス',
    ]);

    const rows = screen.getAllByRole('row');
    // 主将(不一致なし)の行には申告不一致バッジが出ない
    expect(within(rows[1]).queryByText('申告不一致')).not.toBeInTheDocument();
    // 副将(不一致)の行にのみ申告不一致バッジが出る
    expect(within(rows[2]).getByText('申告不一致')).toBeInTheDocument();
  });

  it('グループ大会の卓番号は「A-1」形式で表示する', () => {
    renderWithProviders(
      <TeamPairingTable
        matches={[teamMatchOf({ id: 'm1', tableNumber: 1, group: groupOf({ name: 'A' }) })]}
        editable={false}
        multiGroup
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('A-1');
  });
});
