import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { downloadBlob } from '../../../src/utils/downloadBlob';

describe('downloadBlob', () => {
  const createObjectURL = vi.fn(() => 'blob:mock-url');
  const revokeObjectURL = vi.fn();

  beforeEach(() => {
    URL.createObjectURL = createObjectURL;
    URL.revokeObjectURL = revokeObjectURL;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('a要素を生成してクリックし、object URLを解放する', () => {
    const clickSpy = vi.fn();
    const originalCreateElement = document.createElement.bind(document);
    const createElementSpy = vi.spyOn(document, 'createElement').mockImplementation((tag) => {
      const element = originalCreateElement(tag);
      if (tag === 'a') {
        element.click = clickSpy;
      }
      return element;
    });

    const blob = new Blob(['氏名,所属,段級位,グループ\r\n'], { type: 'text/csv' });
    downloadBlob(blob, 'participants.csv');

    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');

    const anchor = createElementSpy.mock.results[0]?.value as HTMLAnchorElement;
    expect(anchor.download).toBe('participants.csv');
    expect(anchor.href).toBe('blob:mock-url');
  });
});
