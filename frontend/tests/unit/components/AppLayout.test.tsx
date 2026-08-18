import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { AppLayout } from '../../../src/components/layouts/AppLayout';
import { meOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

function renderLayout() {
  return renderWithProviders(
    <Routes>
      <Route path="/tournaments" element={<AppLayout />}>
        <Route index element={<div>大会一覧本体</div>} />
      </Route>
    </Routes>,
    { route: '/tournaments' },
  );
}

describe('AppLayout', () => {
  it('ログアウトボタンがヘッダー背景から区別できる背景色・文字色を持つ', async () => {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json(apiSuccess(meOf()))));
    renderLayout();

    const logoutButton = await screen.findByRole('button', { name: 'ログアウト' });
    expect(logoutButton).toHaveStyle({
      backgroundColor: 'rgb(232, 242, 237)', // primary.light (#E8F2ED)
      color: 'rgb(15, 61, 42)', // primary.dark (#0F3D2A)
    });
  });
});
