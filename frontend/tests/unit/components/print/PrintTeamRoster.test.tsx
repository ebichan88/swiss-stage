import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintTeamRoster } from '../../../../src/components/features/print/PrintTeamRoster';
import { groupOf, teamMemberOf, teamOf } from '../../../fixtures';
import { renderWithProviders } from '../../../testUtils';

describe('PrintTeamRoster', () => {
  it('運営専用であることを明記する', () => {
    renderWithProviders(
      <PrintTeamRoster teams={[teamOf({ members: [teamMemberOf()] })]} groups={[groupOf()]} />,
    );
    expect(screen.getByText('運営用(掲示・配布しないでください)')).toBeInTheDocument();
  });

  it('メンバーの氏名・役割を1行ずつ表示する(運営専用帳票なので個人名を出してよい)', () => {
    renderWithProviders(
      <PrintTeamRoster
        teams={[
          teamOf({
            members: [
              teamMemberOf({ id: 'm1', name: '架空 主将', boardPosition: 1 }),
              teamMemberOf({ id: 'm2', name: '架空 副将', boardPosition: 2 }),
            ],
          }),
        ]}
        groups={[groupOf()]}
      />,
    );
    expect(screen.getByRole('cell', { name: '架空 主将' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '架空 副将' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '役割' })).toBeInTheDocument();
  });

  it('単一グループ大会はグループ列を出さない', () => {
    renderWithProviders(
      <PrintTeamRoster teams={[teamOf({ members: [teamMemberOf()] })]} groups={[groupOf()]} />,
    );
    expect(screen.queryByRole('columnheader', { name: 'グループ' })).not.toBeInTheDocument();
  });
});
