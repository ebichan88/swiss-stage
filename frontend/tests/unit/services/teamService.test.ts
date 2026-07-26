import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { ApiError } from '../../../src/services/apiClient';
import { exportTeamsCsv } from '../../../src/services/teamService';
import { apiError, server } from '../../msw/server';

describe('teamService', () => {
  describe('exportTeamsCsv', () => {
    it('CSVのBlobとContent-Dispositionから取り出したファイル名を返す', async () => {
      const csv = 'チーム名,氏名,段級位,ポジション,グループ\r\nAチーム,主将 一郎,3段,主将,A\r\n';
      server.use(
        http.get('/api/v1/tournaments/t1/teams/csv-export', () =>
          HttpResponse.text(csv, {
            headers: {
              'Content-Type': 'text/csv;charset=UTF-8',
              'Content-Disposition':
                "attachment; filename*=UTF-8''%E5%9B%A3%E4%BD%93%E6%88%A6_teams.csv",
            },
          }),
        ),
      );

      const result = await exportTeamsCsv('t1');

      expect(result.filename).toBe('団体戦_teams.csv');
      await expect(result.blob.text()).resolves.toBe(csv);
    });

    it('エラーレスポンスは code と message を持つ ApiError になる', async () => {
      server.use(
        http.get('/api/v1/tournaments/t1/teams/csv-export', () =>
          HttpResponse.json(apiError('TOURNAMENT_NOT_FOUND', '大会が見つかりません'), {
            status: 404,
          }),
        ),
      );

      const promise = exportTeamsCsv('t1');
      await expect(promise).rejects.toBeInstanceOf(ApiError);
      await expect(promise).rejects.toMatchObject({
        code: 'TOURNAMENT_NOT_FOUND',
        message: '大会が見つかりません',
      });
    });
  });
});
