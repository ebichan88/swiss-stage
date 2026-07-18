import path from 'node:path';

import { expect, type APIResponse, type Page } from '@playwright/test';

/** 対局のAPI表現(検証用の最小限。types/round.ts と同期) */
export interface ApiMatch {
  id: string;
  tableNumber: number;
  group: { id: string; name: string } | null;
  player1: { id: string; name: string };
  player2: { id: string; name: string } | null;
  result: string;
  version: number;
}

export interface ApiRound {
  roundNumber: number;
  status: string;
  matches: ApiMatch[];
}

/** 開発用ログイン(local/testプロファイル限定エンドポイント)でログインする */
export async function loginAsOrganizer(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByRole('button', { name: '開発用ログイン' }).click();
  await expect(page).toHaveURL(/\/tournaments$/);
}

/** 大会を作成して大会概要(S05)へ遷移し、大会IDを返す */
export async function createTournament(
  page: Page,
  name: string,
  totalRounds: number,
): Promise<string> {
  await page.goto('/tournaments/new');
  await page.getByRole('textbox', { name: '大会名' }).fill(name);
  await page.getByRole('spinbutton', { name: 'ラウンド数' }).fill(String(totalRounds));
  await page.getByRole('button', { name: '作成する' }).click();
  await expect(page).toHaveURL(/\/tournaments\/[0-9A-Z]{26}$/);
  const id = page.url().split('/').pop();
  if (!id) throw new Error('大会IDが取得できませんでした');
  return id;
}

/** 参加者管理(S06)でCSVをインポートする */
export async function importParticipantsCsv(
  page: Page,
  tournamentId: string,
  fixtureFile: string,
  expectedCount: number,
): Promise<void> {
  await page.goto(`/tournaments/${tournamentId}/participants`);
  await page.getByRole('button', { name: 'CSVインポート' }).click();
  await page
    .locator('input[type="file"]')
    .setInputFiles(path.join(import.meta.dirname, 'fixtures', fixtureFile));
  await page.getByRole('button', { name: 'インポートする' }).click();
  await expect(page.getByText(`${expectedCount}名をインポートしました`)).toBeVisible();
}

/** 大会概要(S05)から大会を開始する */
export async function startTournament(page: Page, tournamentId: string): Promise<void> {
  await page.goto(`/tournaments/${tournamentId}`);
  await page.getByRole('button', { name: '大会を開始する' }).click();
  await page.getByRole('button', { name: '開始する' }).click();
  await expect(page.getByText('大会を開始しました')).toBeVisible();
}

/** ラウンド管理(S07)で次ラウンドの組み合わせを生成する */
export async function generateRound(
  page: Page,
  tournamentId: string,
  roundNumber: number,
): Promise<void> {
  await page.goto(`/tournaments/${tournamentId}/rounds`);
  await page.getByRole('button', { name: `第${roundNumber}ラウンドの組み合わせを生成` }).click();
  await expect(page.getByText(`第${roundNumber}ラウンドの組み合わせを生成しました`)).toBeVisible();
}

/** 表示中ラウンドの全対局に「player1の勝ち」を入力する(BYEは入力不可のため除外) */
export async function inputAllResults(page: Page): Promise<void> {
  // 卓表示はグループなし=「卓1」/ グループあり=「卓A-1」の両形式
  const combos = page.getByRole('combobox', { name: /卓.+の結果/ });
  const count = await combos.count();
  for (let i = 0; i < count; i++) {
    // 入力後もセレクトは残る(確定まで変更可)ため、毎回 nth(i) を開いて先頭の勝ちを選ぶ
    await combos.nth(i).click();
    await page.getByRole('option', { name: /^○/ }).first().click();
    await expect(page.getByRole('option', { name: /^○/ }).first()).toBeHidden();
  }
  await expect(page.getByText(/未入力 \d+件/)).toBeHidden();
}

/** 表示中のラウンドを確定する */
export async function confirmRound(page: Page, roundNumber: number): Promise<void> {
  await page.getByRole('button', { name: `第${roundNumber}ラウンドを確定する` }).click();
  await page.getByRole('button', { name: '確定する' }).click();
  await expect(page.getByText(`第${roundNumber}ラウンドを確定しました`)).toBeVisible();
}

/** ラウンド一覧をAPIから取得(ペア検証・BYE検証用。Cookieはページのものを使う) */
export async function fetchRounds(page: Page, tournamentId: string): Promise<ApiRound[]> {
  const response: APIResponse = await page.request.get(
    `/api/v1/tournaments/${tournamentId}/rounds`,
  );
  expect(response.ok()).toBe(true);
  const body = (await response.json()) as { data: ApiRound[] };
  return body.data;
}

/** 設定(S09)で公開範囲=共有URL・結果入力許可を有効化し、共有URLを発行してトークンを返す */
export async function publishShareUrl(
  page: Page,
  tournamentId: string,
  { allowResultInput }: { allowResultInput: boolean },
): Promise<string> {
  await page.goto(`/tournaments/${tournamentId}/settings`);
  await page.getByRole('combobox', { name: '公開範囲' }).click();
  await page.getByRole('option', { name: '共有URLを知っている人のみ' }).click();
  if (allowResultInput) {
    await page.getByRole('switch', { name: '参加者による結果入力を許可(共有URL経由)' }).check();
  }
  await page.getByRole('button', { name: '保存する' }).click();
  await expect(page.getByText('設定を保存しました')).toBeVisible();

  await page.getByRole('button', { name: '共有URLを発行する' }).click();
  await expect(page.getByText('共有URLを発行しました')).toBeVisible();
  const shareUrl = await page.getByRole('textbox', { name: '共有URL' }).inputValue();
  const token = shareUrl.split('/s/')[1];
  if (!token) throw new Error(`共有URLからトークンを取得できませんでした: ${shareUrl}`);
  return token;
}
