package com.swiss_stage.domain;

/**
 * ラウンド確定と結果編集の競合(TOCTOU)。対局の保存とラウンドの未確定チェックを同一トランザクションで
 * 行うリポジトリ実装が、書き込み直前にラウンドが確定済みへ変わっていた場合に送出する。application層で
 * 「確定済みラウンドの結果は変更できません」の409に変換する(14_tournament_collaboration.md §4.9)。
 */
public class RoundConfirmedException extends DomainException {

  public RoundConfirmedException(String message) {
    super(message);
  }
}
