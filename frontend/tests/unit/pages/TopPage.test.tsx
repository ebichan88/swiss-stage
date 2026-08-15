import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { TopPage } from '../../../src/pages/TopPage';
import { meOf } from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

function renderTop() {
  return renderWithProviders(
    <Routes>
      <Route path="/" element={<TopPage />} />
      <Route path="/tournaments" element={<div>大会一覧ページ</div>} />
    </Routes>,
    { route: '/' },
  );
}

describe('TopPage', () => {
  it('AUTH-AC-006: 未認証の訪問者に大会運営の主要な特徴が表示される', async () => {
    server.use(
      http.get('/api/v1/auth/me', () =>
        HttpResponse.json(apiError('UNAUTHORIZED', 'ログインしてください'), { status: 401 }),
      ),
    );
    renderTop();

    expect(await screen.findByText('スイス方式マッチングを自動生成')).toBeInTheDocument();
    expect(screen.getByText('結果集計・順位表示を自動化')).toBeInTheDocument();
    expect(screen.getByText('参加者はログイン不要')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '運営者ログイン' })).toHaveAttribute('href', '/login');
  });

  it('AUTH-AC-009: 認証済みの運営者が / を開くと大会一覧へリダイレクトされる', async () => {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json(apiSuccess(meOf()))));
    renderTop();

    expect(await screen.findByText('大会一覧ページ')).toBeInTheDocument();
  });
});
