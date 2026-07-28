import { afterEach, describe, expect, it, vi } from 'vitest';

import { printPage } from '../../../src/utils/printPage';

describe('printPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('PRT-AC-004: window.print() を呼び出す', () => {
    const printSpy = vi.spyOn(window, 'print').mockImplementation(() => {});
    printPage();
    expect(printSpy).toHaveBeenCalledTimes(1);
  });
});
