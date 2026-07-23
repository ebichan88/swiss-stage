import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { TeamMemberManagerDialog } from '../../../src/components/features/team/TeamMemberManagerDialog';
import { teamMemberOf, teamOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOURNAMENT_ID = '01TESTTOURNAMENT0000000000';
const TEAM_ID = '01TESTTEAM00000000000000A';
const MEMBERS_URL = `/api/v1/tournaments/${TOURNAMENT_ID}/teams/${TEAM_ID}/members`;

function renderDialog(team = teamOf({ id: TEAM_ID })) {
  return renderWithProviders(
    <TeamMemberManagerDialog
      open
      tournamentId={TOURNAMENT_ID}
      teamSize={3}
      team={team}
      canEdit
      onClose={() => {}}
    />,
  );
}

describe('TeamMemberManagerDialog', () => {
  it('メンバーを役割・棋力とともに表示し、個人名以外は含めない役割ラベルを表示する', () => {
    const team = teamOf({
      id: TEAM_ID,
      members: [
        teamMemberOf({ id: 'm1', name: '主将 一郎', boardPosition: 1, rank: 'DAN_3' }),
        teamMemberOf({ id: 'm2', name: '補欠 次郎', boardPosition: null, rank: null }),
      ],
    });

    renderDialog(team);

    expect(screen.getByText('主将 一郎')).toBeInTheDocument();
    expect(screen.getByText('主将')).toBeInTheDocument();
    expect(screen.getByText('補欠 次郎')).toBeInTheDocument();
    expect(screen.getByText('補欠')).toBeInTheDocument();
  });

  it('メンバーを追加できる(役割省略時は補欠として送信される)', async () => {
    const team = teamOf({ id: TEAM_ID, members: [] });
    let requestBody: { name: string; boardPosition: number | null } | null = null;
    server.use(
      http.post(MEMBERS_URL, async ({ request }) => {
        requestBody = (await request.json()) as { name: string; boardPosition: number | null };
        return HttpResponse.json(
          apiSuccess({
            ...team,
            members: [teamMemberOf({ id: 'm1', name: requestBody.name })],
          }),
          { status: 201 },
        );
      }),
    );

    renderDialog(team);

    await userEvent.click(screen.getByRole('button', { name: 'メンバーを追加' }));
    await userEvent.type(screen.getByRole('textbox', { name: '氏名' }), '主将 一郎');
    await userEvent.click(screen.getByRole('button', { name: '追加する' }));

    await waitFor(() => expect(requestBody).not.toBeNull());
    expect(requestBody).toMatchObject({ name: '主将 一郎', boardPosition: null });
  });

  it('メンバーを削除できる', async () => {
    const team = teamOf({
      id: TEAM_ID,
      members: [teamMemberOf({ id: 'm1', name: '主将 一郎' })],
    });
    let deleted = false;
    server.use(
      http.delete(`${MEMBERS_URL}/m1`, () => {
        deleted = true;
        return HttpResponse.json(apiSuccess({ ...team, members: [] }));
      }),
    );

    renderDialog(team);

    await userEvent.click(screen.getByRole('button', { name: '主将 一郎を削除' }));
    await userEvent.click(screen.getByRole('button', { name: '削除する' }));

    await waitFor(() => expect(deleted).toBe(true));
  });

  it('canEdit=falseでは追加・編集・削除ボタンを表示しない(大会開始後)', () => {
    const team = teamOf({
      id: TEAM_ID,
      members: [teamMemberOf({ id: 'm1', name: '主将 一郎' })],
    });

    renderWithProviders(
      <TeamMemberManagerDialog
        open
        tournamentId={TOURNAMENT_ID}
        teamSize={3}
        team={team}
        canEdit={false}
        onClose={() => {}}
      />,
    );

    expect(screen.queryByRole('button', { name: 'メンバーを追加' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '主将 一郎を削除' })).not.toBeInTheDocument();
  });
});
