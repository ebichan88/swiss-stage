/** 棋力帯グループ(backend: GroupDto)。作成順(ULID順)で返る */
export interface Group {
  id: string;
  name: string;
}

/** POST /groups(作成)・PATCH /groups/{gid}(改名)のリクエスト(backend: GroupInput) */
export interface GroupInput {
  name: string;
}
