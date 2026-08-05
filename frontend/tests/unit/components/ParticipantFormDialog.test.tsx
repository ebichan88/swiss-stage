import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { ParticipantFormDialog } from '../../../src/components/features/participant/ParticipantFormDialog';
import { renderWithProviders } from '../../testUtils';

describe('ParticipantFormDialog', () => {
  it('TRN-AC-019: 入力エラー時にhelperTextのエラーメッセージが表示され、入力欄にaria-describedbyで関連付けられる', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ParticipantFormDialog
        open
        groups={[]}
        loading={false}
        onSubmit={() => {}}
        onClose={() => {}}
      />,
    );

    // 氏名を未入力のまま送信してバリデーションエラーを起こす
    await user.click(screen.getByRole('button', { name: '追加する' }));

    const nameInput = screen.getByRole('textbox', { name: /氏名/ });
    const errorMessage = await screen.findByText('氏名は必須です');

    const describedBy = nameInput.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(errorMessage.id).toBe(describedBy);
  });
});
