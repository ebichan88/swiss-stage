import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import App from '../../src/App';

describe('App', () => {
  it('トップページが表示される', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: 'Swiss Stage' })).toBeInTheDocument();
  });
});
