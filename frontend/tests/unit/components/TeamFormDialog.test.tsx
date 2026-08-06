import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { TeamFormDialog } from '../../../src/components/features/team/TeamFormDialog';
import { renderWithProviders } from '../../testUtils';

describe('TeamFormDialog', () => {
  it('TRN-AC-019: 入力エラー時にhelperTextのエラーメッセージが表示され、入力欄にaria-describedbyで関連付けられる', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <TeamFormDialog open groups={[]} loading={false} onSubmit={() => {}} onClose={() => {}} />,
    );

    // チーム名を未入力のまま送信してバリデーションエラーを起こす
    await user.click(screen.getByRole('button', { name: '追加する' }));

    const nameInput = screen.getByRole('textbox', { name: /チーム名/ });
    const errorMessage = await screen.findByText('チーム名は必須です');

    const describedBy = nameInput.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(errorMessage.id).toBe(describedBy);
  });
});
