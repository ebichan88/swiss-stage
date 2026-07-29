import { http, HttpResponse } from 'msw';
import type { Meta, StoryObj } from '@storybook/react-vite';

import { matchOf, roundOf, sharedSummaryOf, sharedTournamentOf } from '../../tests/fixtures';
import { apiSuccess } from '../../tests/msw/apiResponse';
import { SharedResultPage } from './SharedResultPage';

const API = '/api/v1';
/** SharedPageのfixtureと揃えるため、初期matchOf()と同じidを使う(SharedResultPageはmidで対局を検索する) */
const MATCH_ID = '01TESTMATCH000000000000000';

/**
 * S11 共有・結果入力(スマホ優先)。押し間違い防止・確認ステップが最重要画面の1つ(00_basic_design.md §4)。
 * 送信後は組み合わせタブへ遷移するため(この画面には留まらない)、送信後の状態は
 * 「両者の申告済み(=結果確定済み)の対局を開いた場合の見え方」で代替する
 */
const meta: Meta<typeof SharedResultPage> = {
  component: SharedResultPage,
  parameters: {
    routePath: '/s/:token/matches/:mid',
    route: `/s/01TESTSHARETOKEN00000001/matches/${MATCH_ID}`,
    globals: { viewport: { value: '375-812' } },
  },
};

export default meta;

type Story = StoryObj<typeof SharedResultPage>;

/** 通常: 未申告。「あなたはどちらですか?」から選択する */
export const Default: Story = {
  parameters: {
    msw: {
      handlers: [
        http.get(`${API}/shared/:token`, () =>
          HttpResponse.json(
            apiSuccess(
              sharedTournamentOf({
                tournament: sharedSummaryOf({ resultInputEnabled: true }),
                rounds: [
                  roundOf({
                    roundNumber: 1,
                    status: 'PLAYING',
                    matches: [matchOf({ id: MATCH_ID })],
                  }),
                ],
              }),
            ),
          ),
        ),
      ],
    },
  },
};

/** 両者の申告が一致し結果が確定済みの対局を開いた場合 */
export const AlreadyDecided: Story = {
  parameters: {
    msw: {
      handlers: [
        http.get(`${API}/shared/:token`, () =>
          HttpResponse.json(
            apiSuccess(
              sharedTournamentOf({
                tournament: sharedSummaryOf({ resultInputEnabled: true }),
                rounds: [
                  roundOf({
                    roundNumber: 1,
                    status: 'PLAYING',
                    matches: [
                      matchOf({
                        id: MATCH_ID,
                        player1ReportedResult: 'PLAYER1_WIN',
                        player2ReportedResult: 'PLAYER1_WIN',
                        result: 'PLAYER1_WIN',
                      }),
                    ],
                  }),
                ],
              }),
            ),
          ),
        ),
      ],
    },
  },
};
