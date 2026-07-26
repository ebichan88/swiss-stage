import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { ApiError } from '../../../src/services/apiClient';
import { exportParticipantsCsv } from '../../../src/services/participantService';
import { apiError, server } from '../../msw/server';

describe('participantService', () => {
  describe('exportParticipantsCsv', () => {
    it('CSVのBlobとContent-Dispositionから取り出したファイル名を返す', async () => {
      const csv = '氏名,所属,段級位,グループ\r\n蛯名 隆,〇〇株式会社,3級,A\r\n';
      server.use(
        http.get('/api/v1/tournaments/t1/participants/export', () =>
          HttpResponse.text(csv, {
            headers: {
              'Content-Type': 'text/csv;charset=UTF-8',
              'Content-Disposition':
                "attachment; filename*=UTF-8''%E5%8F%82%E5%8A%A0%E8%80%85%E5%A4%A7%E4%BC%9A_participants.csv",
            },
          }),
        ),
      );

      const result = await exportParticipantsCsv('t1');

      expect(result.filename).toBe('参加者大会_participants.csv');
      await expect(result.blob.text()).resolves.toBe(csv);
    });

    it('エラーレスポンスは code と message を持つ ApiError になる', async () => {
      server.use(
        http.get('/api/v1/tournaments/t1/participants/export', () =>
          HttpResponse.json(apiError('TOURNAMENT_NOT_FOUND', '大会が見つかりません'), {
            status: 404,
          }),
        ),
      );

      const promise = exportParticipantsCsv('t1');
      await expect(promise).rejects.toBeInstanceOf(ApiError);
      await expect(promise).rejects.toMatchObject({
        code: 'TOURNAMENT_NOT_FOUND',
        message: '大会が見つかりません',
      });
    });
  });
});
