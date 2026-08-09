import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { TournamentListPage } from '../../../src/pages/TournamentListPage';
import { tournamentOf } from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

describe('TournamentListPage', () => {
  it('大会一覧を表示する', async () => {
    server.use(
      http.get('/api/v1/tournaments', () =>
        HttpResponse.json(
          apiSuccess([
            tournamentOf({ id: 't1', name: '夏の囲碁大会' }),
            tournamentOf({
              id: 't2',
              name: '秋の将棋大会',
              gameType: 'SHOGI',
              status: 'IN_PROGRESS',
              currentRound: 2,
            }),
          ]),
        ),
      ),
    );

    renderWithProviders(<TournamentListPage />);

    expect(await screen.findByText('夏の囲碁大会')).toBeInTheDocument();
    expect(screen.getByText('秋の将棋大会')).toBeInTheDocument();
    expect(screen.getByText('開催中')).toBeInTheDocument();
  });

  it('0件のときは空状態とアクションを表示する', async () => {
    server.use(http.get('/api/v1/tournaments', () => HttpResponse.json(apiSuccess([]))));

    renderWithProviders(<TournamentListPage />);

    expect(await screen.findByText('大会がまだありません')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '最初の大会を作成する' })).toBeInTheDocument();
  });

  it('取得失敗時は再試行ボタン付きのエラーを表示する', async () => {
    server.use(
      http.get('/api/v1/tournaments', () =>
        HttpResponse.json(apiError('INTERNAL_ERROR', '予期しないエラーが発生しました'), {
          status: 500,
        }),
      ),
    );

    renderWithProviders(<TournamentListPage />);

    expect(await screen.findByText('大会一覧の取得に失敗しました')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '再試行' })).toBeInTheDocument();
  });

  const setUpFilterTest = () => {
    server.use(
      http.get('/api/v1/tournaments', () =>
        HttpResponse.json(
          apiSuccess([
            tournamentOf({ id: 't1', name: 'Summer Go Cup', status: 'PREPARING' }),
            tournamentOf({
              id: 't2',
              name: '秋の将棋大会',
              gameType: 'SHOGI',
              status: 'IN_PROGRESS',
              currentRound: 2,
            }),
            tournamentOf({
              id: 't3',
              name: '冬季将棋オープン',
              gameType: 'SHOGI',
              status: 'FINISHED',
            }),
          ]),
        ),
      ),
    );
  };

  it('TRN-AC-020: 大会名の部分一致検索(大文字小文字を区別しない)で、一致する大会だけが表示される', async () => {
    setUpFilterTest();
    const user = userEvent.setup();
    renderWithProviders(<TournamentListPage />);

    await screen.findByText('Summer Go Cup');
    await user.type(screen.getByRole('textbox', { name: '大会名で検索' }), 'go');

    expect(screen.getByText('Summer Go Cup')).toBeInTheDocument();
    expect(screen.queryByText('秋の将棋大会')).not.toBeInTheDocument();
    expect(screen.queryByText('冬季将棋オープン')).not.toBeInTheDocument();
  });

  it('検索欄が空白のみの場合は未フィルタと同じ全件表示になる', async () => {
    setUpFilterTest();
    const user = userEvent.setup();
    renderWithProviders(<TournamentListPage />);

    await screen.findByText('Summer Go Cup');
    await user.type(screen.getByRole('textbox', { name: '大会名で検索' }), '   ');

    expect(screen.getByText('Summer Go Cup')).toBeInTheDocument();
    expect(screen.getByText('秋の将棋大会')).toBeInTheDocument();
    expect(screen.getByText('冬季将棋オープン')).toBeInTheDocument();
  });

  it('TRN-AC-021: 状態フィルタで、選択した状態の大会だけが表示される', async () => {
    setUpFilterTest();
    const user = userEvent.setup();
    renderWithProviders(<TournamentListPage />);

    await screen.findByText('Summer Go Cup');
    await user.click(screen.getByRole('combobox', { name: '状態で絞り込み' }));
    await user.click(screen.getByRole('option', { name: '開催中' }));

    expect(screen.getByText('秋の将棋大会')).toBeInTheDocument();
    expect(screen.queryByText('Summer Go Cup')).not.toBeInTheDocument();
    expect(screen.queryByText('冬季将棋オープン')).not.toBeInTheDocument();
  });

  it('TRN-AC-022: 大会名検索と状態フィルタを同時に指定すると、両方を満たす大会だけが表示される(AND)', async () => {
    setUpFilterTest();
    const user = userEvent.setup();
    renderWithProviders(<TournamentListPage />);

    await screen.findByText('Summer Go Cup');
    await user.type(screen.getByRole('textbox', { name: '大会名で検索' }), '将棋');
    await user.click(screen.getByRole('combobox', { name: '状態で絞り込み' }));
    await user.click(screen.getByRole('option', { name: '終了' }));

    expect(screen.getByText('冬季将棋オープン')).toBeInTheDocument();
    expect(screen.queryByText('秋の将棋大会')).not.toBeInTheDocument();
    expect(screen.queryByText('Summer Go Cup')).not.toBeInTheDocument();
  });

  it('状態フィルタで「すべて」が選択されているとき、セレクトボックスが空白にならず「すべて」と表示される(回帰防止。TRN-AC-021に統合)', async () => {
    setUpFilterTest();
    const user = userEvent.setup();
    renderWithProviders(<TournamentListPage />);

    await screen.findByText('Summer Go Cup');
    const statusFilter = screen.getByRole('combobox', { name: '状態で絞り込み' });
    expect(statusFilter).toHaveTextContent('すべて');

    await user.click(statusFilter);
    await user.click(screen.getByRole('option', { name: '開催中' }));
    expect(statusFilter).toHaveTextContent('開催中');

    await user.click(statusFilter);
    await user.click(screen.getByRole('option', { name: 'すべて' }));
    expect(statusFilter).toHaveTextContent('すべて');
  });

  it('TRN-AC-023: 検索・フィルタの結果が0件のとき専用の空状態が表示され、「検索条件をクリア」で元の一覧に戻る', async () => {
    setUpFilterTest();
    const user = userEvent.setup();
    renderWithProviders(<TournamentListPage />);

    await screen.findByText('Summer Go Cup');
    await user.type(screen.getByRole('textbox', { name: '大会名で検索' }), '該当なし');

    expect(await screen.findByText('条件に一致する大会がありません')).toBeInTheDocument();
    expect(screen.queryByText('Summer Go Cup')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '検索条件をクリア' }));

    expect(await screen.findByText('Summer Go Cup')).toBeInTheDocument();
    expect(screen.getByText('秋の将棋大会')).toBeInTheDocument();
    expect(screen.getByText('冬季将棋オープン')).toBeInTheDocument();
  });

  it('大会が1件も無い状態では検索・フィルタのツールバー自体が表示されない', async () => {
    server.use(http.get('/api/v1/tournaments', () => HttpResponse.json(apiSuccess([]))));

    renderWithProviders(<TournamentListPage />);

    await screen.findByText('大会がまだありません');
    expect(screen.queryByRole('textbox', { name: '大会名で検索' })).not.toBeInTheDocument();
  });
});
