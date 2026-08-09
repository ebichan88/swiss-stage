import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { PairingTable } from '../../../src/components/features/round/PairingTable';
import { groupOf, matchOf, summaryOf } from '../../fixtures';
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

describe('PairingTable', () => {
  const originalMatchMedia = window.matchMedia;

  afterEach(() => {
    vi.restoreAllMocks();
    window.matchMedia = originalMatchMedia;
  });

  it('スマホ表示の卓番号は共有ページの卓番号タイル(「卓」キャプション付き)ではなく、プレーンなテキストのまま表示する(回帰防止)', () => {
    mockMatchMedia(true);
    renderWithProviders(
      <PairingTable
        matches={[matchOf({ id: 'm1', tableNumber: 3 })]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    expect(screen.getByText('3')).toBeInTheDocument();
    // 共有ページの卓番号タイルは数字の下に「卓」キャプションを添える。管理画面はプレーンテキストのまま
    expect(screen.queryByText('卓')).not.toBeInTheDocument();
  });

  it('卓番号と対局者を表示し、勝敗入力済みは○/●が付く', () => {
    renderWithProviders(
      <PairingTable
        matches={[
          matchOf({ id: 'm1', tableNumber: 1, result: 'PLAYER1_WIN' }),
          matchOf({
            id: 'm2',
            tableNumber: 2,
            player1: summaryOf({ id: 'p3', name: '試験 次郎', organization: null }),
            player2: null,
            result: 'BYE',
          }),
        ]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('○ 架空 太郎(テスト囲碁会)');
    expect(rows[1]).toHaveTextContent('● 仮名 花子');
    // BYE(不戦勝)は入力不可で「不戦勝」表示
    expect(rows[2]).toHaveTextContent('(不戦勝)');
    expect(within(rows[2]).getByText('不戦勝')).toBeInTheDocument();
  });

  it('テーブルをoutlined枠線+角丸のPaperで囲む(ページ背景と偶数行の背景色が同化する境界不明瞭の回帰防止)', () => {
    renderWithProviders(
      <PairingTable
        matches={[matchOf({ id: 'm1', tableNumber: 1 })]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const paper = screen.getByRole('table').closest('.MuiPaper-root');
    expect(paper).toHaveClass('MuiPaper-outlined');
  });

  it('グループ大会の卓番号は「A-1」形式で表示する', () => {
    renderWithProviders(
      <PairingTable
        matches={[matchOf({ id: 'm1', tableNumber: 1, group: groupOf({ name: 'A' }) })]}
        editable
        multiGroup
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('A-1');
    expect(screen.getByRole('combobox', { name: '卓A-1の結果' })).toBeInTheDocument();
  });

  it('結果を選ぶと onInputResult が呼ばれる', async () => {
    const user = userEvent.setup();
    const onInputResult = vi.fn();
    const match = matchOf();
    renderWithProviders(
      <PairingTable
        matches={[match]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={onInputResult}
      />,
    );

    await user.click(screen.getByRole('combobox'));
    await user.click(screen.getByRole('option', { name: '○ 仮名 花子 の勝ち' }));

    expect(onInputResult).toHaveBeenCalledWith(match, 'PLAYER2_WIN');
  });

  it('SHR-AC-015: 申告不一致のバッジと申告詳細は「結果」列ではなく「申告ステータス」列にまとめて表示される', () => {
    renderWithProviders(
      <PairingTable
        matches={[
          matchOf({
            id: 'm1',
            player1ReportedResult: 'PLAYER1_WIN',
            player2ReportedResult: 'PLAYER2_WIN',
          }),
        ]}
        editable
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    const cells = within(rows[1]).getAllByRole('cell');
    const resultCell = cells[3];
    const statusCell = cells[4];

    expect(within(statusCell).getByText('申告不一致')).toBeInTheDocument();
    expect(within(statusCell).getAllByText(/の申告:/)).toHaveLength(2);
    expect(within(resultCell).queryByText(/の申告:/)).not.toBeInTheDocument();
  });

  it('確定済み(editable=false)は入力コントロールを出さない', () => {
    renderWithProviders(
      <PairingTable
        matches={[matchOf({ result: 'DRAW' })]}
        editable={false}
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.getByText('引き分け')).toBeInTheDocument();
  });
});
