import type { components } from './generated/api';

/**
 * 棋力帯グループ。作成順(ULID順)で返る。
 * 大会は常に1つ以上のグループを持つ(大会作成時に「A」を自動作成)
 */
export type Group = components['schemas']['Group'];

/** POST /groups(作成)・PATCH /groups/{gid}(改名)のリクエスト */
export type GroupInput = components['schemas']['GroupRequest'];
