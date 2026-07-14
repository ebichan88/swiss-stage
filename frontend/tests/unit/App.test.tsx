import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import App from '../../src/App';

describe('App', () => {
  it('トップページが表示される', async () => {
    render(<App />);
    // ルートはコード分割(lazy)のため初回表示は非同期
    expect(
      await screen.findByRole('heading', { name: 'Swiss Stage' }, { timeout: 10_000 }),
    ).toBeInTheDocument();
  });
});
