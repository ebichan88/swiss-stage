/**
 * 棋力帯グループ(backend: GroupDto)。作成順(ULID順)で返る。
 * 大会は常に1つ以上のグループを持つ(大会作成時に「A」を自動作成)
 */
export interface Group {
  id: string;
  name: string;
}

/** POST /groups(作成)・PATCH /groups/{gid}(改名)のリクエスト(backend: GroupInput) */
export interface GroupInput {
  name: string;
}
