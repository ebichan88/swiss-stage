import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { GroupManagerDialog } from '../../../src/components/features/participant/GroupManagerDialog';
import { groupOf, participantOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOURNAMENT_ID = '01TESTTOURNAMENT0000000000';
const GROUPS_URL = `/api/v1/tournaments/${TOURNAMENT_ID}/groups`;

function renderDialog() {
  return renderWithProviders(
    <GroupManagerDialog open tournamentId={TOURNAMENT_ID} onClose={() => {}} />,
  );
}

describe('GroupManagerDialog', () => {
  it('グループ一覧を表示し、新しいグループを追加できる', async () => {
    const groups = [groupOf({ id: 'g1', name: 'A' })];
    server.use(
      http.get(GROUPS_URL, () => HttpResponse.json(apiSuccess(groups))),
      http.post(GROUPS_URL, async ({ request }) => {
        const body = (await request.json()) as { name: string };
        groups.push(groupOf({ id: 'g2', name: body.name }));
        return HttpResponse.json(apiSuccess(groups.at(-1)), { status: 201 });
      }),
    );

    renderDialog();

    expect(await screen.findByText('A')).toBeInTheDocument();

    await userEvent.type(screen.getByRole('textbox', { name: '新しいグループ名' }), 'B');
    await userEvent.click(screen.getByRole('button', { name: '追加' }));

    expect(await screen.findByText('B')).toBeInTheDocument();
  });

  it('自動振り分けは確認ダイアログを経て実行され、成功を通知する', async () => {
    let assigned = false;
    server.use(
      http.get(GROUPS_URL, () =>
        HttpResponse.json(
          apiSuccess([groupOf({ id: 'g1', name: 'A' }), groupOf({ id: 'g2', name: 'B' })]),
        ),
      ),
      http.post(`${GROUPS_URL}/auto-assign`, () => {
        assigned = true;
        return HttpResponse.json(apiSuccess([participantOf({ groupId: 'g1' })]));
      }),
    );

    renderDialog();

    await screen.findByText('A');
    await userEvent.click(screen.getByRole('button', { name: '段級位で自動振り分け' }));
    // 確認ダイアログで承認して初めて実行される
    await userEvent.click(screen.getByRole('button', { name: '振り分ける' }));

    await waitFor(() => expect(assigned).toBe(true));
    expect(
      await screen.findByText('段級位でグループを振り分けました。参加者一覧で個別調整できます'),
    ).toBeInTheDocument();
  });

  it('グループ削除は確認ダイアログで参加者の移動先の注意を表示する', async () => {
    let deleted = false;
    server.use(
      http.get(GROUPS_URL, () =>
        HttpResponse.json(
          apiSuccess([groupOf({ id: 'g1', name: 'A' }), groupOf({ id: 'g2', name: 'B' })]),
        ),
      ),
      http.delete(`${GROUPS_URL}/g2`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderDialog();

    await screen.findByText('B');
    await userEvent.click(screen.getByRole('button', { name: 'Bを削除' }));
    expect(
      screen.getByText('グループ「B」を削除します。割当済みの参加者は隣のグループへ移動します。'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '削除する' }));

    await waitFor(() => expect(deleted).toBe(true));
  });

  it('最後の1グループは削除ボタンが無効になる', async () => {
    server.use(
      http.get(GROUPS_URL, () => HttpResponse.json(apiSuccess([groupOf({ id: 'g1', name: 'A' })]))),
    );

    renderDialog();

    await screen.findByText('A');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Aを削除' })).toBeDisabled());
  });
});
