import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintMatchResultsTable } from '../../../../src/components/features/print/PrintMatchResultsTable';
import { participantOf } from '../../../fixtures';
import { renderWithProviders } from '../../../testUtils';

describe('PrintMatchResultsTable', () => {
  it('PRT-AC-005: 生成済みラウンド数によらず totalRounds 分すべてのラウンド列を出す', () => {
    renderWithProviders(
      <PrintMatchResultsTable participants={[participantOf()]} totalRounds={5} />,
    );
    for (let round = 1; round <= 5; round++) {
      expect(screen.getByRole('columnheader', { name: `${round}回戦` })).toBeInTheDocument();
    }
  });

  it('PRT-AC-005: No.・名前・段級位は入力済みで表示し、対戦相手・結果・勝点・SOS・SOSOS・順位は空欄で出力する', () => {
    renderWithProviders(
      <PrintMatchResultsTable
        participants={[
          participantOf({
            entryOrder: 1,
            name: '架空 太郎',
            rank: 'DAN_3',
            organization: 'テスト囲碁会',
          }),
        ]}
        totalRounds={2}
      />,
    );

    const rows = screen.getAllByRole('row');
    // ヘッダー2行 + データ1行
    expect(rows).toHaveLength(3);
    const dataRow = rows[2];
    expect(dataRow).toHaveTextContent('架空 太郎');
    expect(dataRow).toHaveTextContent('3段');
    // 所属は列として出さない
    expect(dataRow).not.toHaveTextContent('テスト囲碁会');

    // データ行のNo./名前/段級位を除く残り全セル(相手×2ラウンド分・結果×2ラウンド分・勝点・SOS・SOSOS・順位)は空欄
    const cells = screen.getAllByRole('cell');
    const blankCells = cells.slice(3); // 先頭3セル(No./名前/段級位)を除く
    expect(blankCells).toHaveLength(2 * 2 + 4);
    blankCells.forEach((cell) => expect(cell).toHaveTextContent(''));
  });

  it('棄権(WITHDRAWN)は出力しない', () => {
    renderWithProviders(
      <PrintMatchResultsTable
        participants={[
          participantOf({ id: 'p1' }),
          participantOf({ id: 'p2', status: 'WITHDRAWN' }),
        ]}
        totalRounds={3}
      />,
    );
    expect(screen.getAllByRole('row')).toHaveLength(3); // ヘッダー2行 + データ1行
  });

  it('データ行の高さは手書き用に writableRowHeight を確保する', () => {
    renderWithProviders(
      <PrintMatchResultsTable participants={[participantOf()]} totalRounds={1} />,
    );
    // jsdomはレイアウトを行わないため、注入されたCSS自体にheightが含まれることで確認する
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');
    expect(css).toMatch(/height:\s*14mm/);
  });

  it('手書き記入用に濃色の格子罫線を引く(既定の薄いdividerでは見えないため)', () => {
    renderWithProviders(
      <PrintMatchResultsTable participants={[participantOf()]} totalRounds={1} />,
    );
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');
    expect(css).toMatch(/border:1px solid/);
  });

  it('列ごとのalign指定を持たず、全セルを中央揃えに統一する(CSSで一括適用)', () => {
    renderWithProviders(
      <PrintMatchResultsTable participants={[participantOf()]} totalRounds={1} />,
    );
    // MUIのalign propは MuiTableCell-alignLeft/Right 等のクラスを付与する。
    // 個別指定を残していないことをクラス名の不在で確認する
    document.querySelectorAll('td, th').forEach((cell) => {
      expect(cell.className).not.toMatch(/MuiTableCell-align(Left|Right)/);
    });
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');
    expect(css).toMatch(/text-align:center/);
  });

  it('各ラウンドの「相手」列を狭める(結果列を相対的に広く目立たせる)', () => {
    renderWithProviders(
      <PrintMatchResultsTable participants={[participantOf()]} totalRounds={1} />,
    );
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');
    expect(css).toMatch(/width:10mm/);
  });
});
