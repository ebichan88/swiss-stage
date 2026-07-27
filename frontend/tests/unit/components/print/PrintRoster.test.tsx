import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintRoster } from '../../../../src/components/features/print/PrintRoster';
import { groupOf, participantOf } from '../../../fixtures';
import { renderWithProviders } from '../../../testUtils';

describe('PrintRoster', () => {
  it('単一グループ大会はグループ列を出さない', () => {
    renderWithProviders(<PrintRoster participants={[participantOf()]} groups={[groupOf()]} />);
    expect(screen.queryByRole('columnheader', { name: 'グループ' })).not.toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '架空 太郎' })).toBeInTheDocument();
  });

  it('複数グループ大会はグループ列を出す', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    renderWithProviders(
      <PrintRoster participants={[participantOf({ groupId: 'gA' })]} groups={[groupA, groupB]} />,
    );
    expect(screen.getByRole('columnheader', { name: 'グループ' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'A' })).toBeInTheDocument();
  });

  it('棄権者は状態列に「棄権」と表示する', () => {
    renderWithProviders(
      <PrintRoster participants={[participantOf({ status: 'WITHDRAWN' })]} groups={[groupOf()]} />,
    );
    expect(screen.getByRole('cell', { name: '棄権' })).toBeInTheDocument();
  });
});
