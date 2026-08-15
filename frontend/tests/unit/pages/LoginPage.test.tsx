import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { LoginPage } from '../../../src/pages/LoginPage';
import { meOf } from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

function renderLogin(route = '/login') {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/tournaments" element={<div>大会一覧ページ</div>} />
    </Routes>,
    { route },
  );
}

function mockUnauthenticated() {
  server.use(
    http.get('/api/v1/auth/me', () =>
      HttpResponse.json(apiError('UNAUTHORIZED', 'ログインしてください'), { status: 401 }),
    ),
  );
}

describe('LoginPage', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('AUTH-AC-010: 通常表示でロゴ・見出し・Googleログインボタンが表示される', async () => {
    mockUnauthenticated();
    const { container } = renderLogin();

    expect(await screen.findByText('運営者としてログインしてください')).toBeInTheDocument();
    expect(container.querySelector('img[src="/swiss-stage.svg"]')).toBeInTheDocument();
    const googleLoginLink = screen.getByRole('link', { name: /Googleでログイン/ });
    expect(googleLoginLink).toHaveAttribute('href', '/api/v1/auth/login');
    expect(screen.queryByText('Googleログインに失敗しました。再度お試しください。')).toBeNull();
  });

  it('AUTH-AC-007: OAuth認可失敗時(?error=oauth)にエラーメッセージが表示され、再度Googleログインを試せる', async () => {
    mockUnauthenticated();
    renderLogin('/login?error=oauth');

    expect(
      await screen.findByText('Googleログインに失敗しました。再度お試しください。'),
    ).toBeInTheDocument();
    const googleLoginLink = screen.getByRole('link', { name: /Googleでログイン/ });
    expect(googleLoginLink).toHaveAttribute('href', '/api/v1/auth/login');
  });

  it('AUTH-AC-008: 開発ビルドでは開発用ログインボタンが表示される', async () => {
    mockUnauthenticated();
    vi.stubEnv('DEV', true);
    renderLogin();

    expect(await screen.findByRole('button', { name: '開発用ログイン' })).toBeInTheDocument();
  });

  it('AUTH-AC-008: 本番ビルドでは開発用ログインボタンが表示されない', async () => {
    mockUnauthenticated();
    vi.stubEnv('DEV', false);
    renderLogin();

    await screen.findByText('運営者としてログインしてください');
    expect(screen.queryByRole('button', { name: '開発用ログイン' })).toBeNull();
  });

  it('AUTH-AC-009: 認証済みの運営者が /login を開くと大会一覧へリダイレクトされる', async () => {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json(apiSuccess(meOf()))));
    renderLogin();

    expect(await screen.findByText('大会一覧ページ')).toBeInTheDocument();
  });
});
