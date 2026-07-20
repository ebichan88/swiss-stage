import type { components } from './generated/api';

/** 順位表の1行。rank は同順位あり(1,2,2,4 形式)。グループ大会ではグループ内順位 */
export type Standing = components['schemas']['Standing'];

/** グループ別順位表。グループごとに1要素(group は常に非null) */
export type GroupStandings = components['schemas']['GroupStandings'];
