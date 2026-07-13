import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ConfirmDialog } from '../../../src/components/ui/ConfirmDialog';

describe('ConfirmDialog', () => {
  it('確定・キャンセルのコールバックが動く', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(
      <ConfirmDialog
        open
        title="削除しますか?"
        message="元に戻せません。"
        confirmLabel="削除する"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    await user.click(screen.getByRole('button', { name: '削除する' }));
    expect(onConfirm).toHaveBeenCalledOnce();

    await user.click(screen.getByRole('button', { name: 'キャンセル' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('requiredText 指定時は一致するまで確定できない(重大確認)', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        open
        title="大会を削除しますか?"
        message="元に戻せません。"
        confirmLabel="削除する"
        requiredText="夏の大会"
        onConfirm={onConfirm}
        onCancel={() => {}}
      />,
    );

    const confirmButton = screen.getByRole('button', { name: '削除する' });
    expect(confirmButton).toBeDisabled();

    await user.type(screen.getByRole('textbox'), '夏の大');
    expect(confirmButton).toBeDisabled();

    await user.type(screen.getByRole('textbox'), '会');
    expect(confirmButton).toBeEnabled();
  });

  it('loading 中は確定ボタンが無効(二重送信防止)', () => {
    render(
      <ConfirmDialog
        open
        title="確定しますか?"
        message="確定後は変更できません。"
        confirmLabel="確定する"
        loading
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: '確定する' })).toBeDisabled();
  });
});
