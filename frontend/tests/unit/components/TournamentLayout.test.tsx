import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TournamentLayout } from '../../../src/components/layouts/TournamentLayout';
import { tournamentOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOURNAMENT_ID = '01TESTTOURNAMENT0000000000';

/** `theme.breakpoints.up('md')` の一致/不一致を固定するmatchMediaモック */
function mockMatchMedia(matchesDesktop: boolean) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: query.includes('min-width') ? matchesDesktop : !matchesDesktop,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
}

function renderLayout() {
  return renderWithProviders(
    <Routes>
      <Route path="/tournaments/:id" element={<TournamentLayout />}>
        <Route path="rounds" element={<div>ラウンド画面本体</div>} />
        <Route path="standings" element={<div>順位画面本体</div>} />
      </Route>
    </Routes>,
    { route: `/tournaments/${TOURNAMENT_ID}/rounds` },
  );
}

describe('TournamentLayout', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('TRN-AC-017: PC(サイドバー)で現在のページに対応する項目にaria-current="page"が設定され、他の項目へキーボードでフォーカス移動できる', async () => {
    mockMatchMedia(true);
    server.use(
      http.get(`/api/v1/tournaments/${TOURNAMENT_ID}`, () =>
        HttpResponse.json(apiSuccess(tournamentOf({ id: TOURNAMENT_ID, status: 'IN_PROGRESS' }))),
      ),
    );

    renderLayout();

    const currentLink = await screen.findByRole('link', { name: 'ラウンド' });
    expect(currentLink).toHaveAttribute('aria-current', 'page');
    const otherLink = screen.getByRole('link', { name: '順位' });
    expect(otherLink).not.toHaveAttribute('aria-current');

    // Tab移動でナビゲーション項目へ順番にフォーカスが到達すること(キーボード操作性)を検証する
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('link', { name: '概要' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('link', { name: '参加者' })).toHaveFocus();
    await user.tab();
    expect(currentLink).toHaveFocus();
    await user.tab();
    expect(otherLink).toHaveFocus();
  });

  it('TRN-AC-017: スマホ(下部タブ)で現在のページに対応する項目にaria-current="page"が設定される', async () => {
    mockMatchMedia(false);
    server.use(
      http.get(`/api/v1/tournaments/${TOURNAMENT_ID}`, () =>
        HttpResponse.json(apiSuccess(tournamentOf({ id: TOURNAMENT_ID, status: 'IN_PROGRESS' }))),
      ),
    );

    renderLayout();

    const currentTab = await screen.findByRole('button', { name: 'ラウンド' });
    expect(currentTab).toHaveAttribute('aria-current', 'page');
    const otherTab = screen.getByRole('button', { name: '順位' });
    expect(otherTab).not.toHaveAttribute('aria-current');
  });
});
