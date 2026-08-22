package com.swiss_stage.domain.model;

/**
 * 大会に対する役割。OWNER=作成者、MAINTAINER=招待された共同管理者(14_tournament_collaboration.md §4.1)。
 * 将来のVIEWER追加等を非破壊にするため、権限判定は宣言順(ordinal)に依存せず明示的な述語メソッドで表現する (CLAUDE.md #13)。
 */
public enum TournamentRole {
  OWNER,
  MAINTAINER;

  /** 大会設定・削除・共有トークン再発行・招待/メンバー管理を行えるか */
  public boolean canManageSettings() {
    return this == OWNER;
  }
}
