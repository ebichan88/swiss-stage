import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintReportHeader } from '../../../../src/components/features/print/PrintReportHeader';
import { renderWithProviders } from '../../../testUtils';

describe('PrintReportHeader', () => {
  it('PRT-AC-013: 大会名・帳票名・開催日を表示する', () => {
    renderWithProviders(
      <PrintReportHeader
        tournamentName="第1回テスト囲碁大会"
        eventDate="2026-08-15"
        reportTitle="参加者名簿"
      />,
    );
    expect(screen.getByText('第1回テスト囲碁大会')).toBeInTheDocument();
    expect(screen.getByText('参加者名簿')).toBeInTheDocument();
    expect(screen.getByText('開催日:')).toBeInTheDocument();
    expect(screen.getByText('2026/8/15')).toBeInTheDocument();
  });

  it('PRT-AC-013: 開催日が未設定(null)でも手書き用の記入枠(下線)を残す', () => {
    renderWithProviders(
      <PrintReportHeader tournamentName="大会" eventDate={null} reportTitle="参加者名簿" />,
    );
    expect(screen.getByText('開催日:')).toBeInTheDocument();
    // 記入枠は下線(border-bottom)+固定幅で確保する。jsdomはレイアウトしないためCSSで確認する
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');
    expect(css).toMatch(/border-bottom:1px solid/);
    expect(css).toMatch(/width:30mm/);
  });

  it('複数グループ大会はグループ名を帳票名に併記する', () => {
    renderWithProviders(
      <PrintReportHeader
        tournamentName="大会"
        eventDate={null}
        reportTitle="対戦結果表"
        groupName="A"
      />,
    );
    expect(screen.getByText('対戦結果表(A)')).toBeInTheDocument();
  });
});
