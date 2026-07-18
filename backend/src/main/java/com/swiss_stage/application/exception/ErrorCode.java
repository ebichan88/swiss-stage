package com.swiss_stage.application.exception;

/**
 * エラーコード一覧(.claude/01_development_docs/06_error_handling_design.md §2)。
 * 新しいコードを追加する場合は必ず設計ドキュメントの表にも追記する。
 */
public enum ErrorCode {
    VALIDATION_ERROR(400, "入力内容を確認してください"),
    CSV_INVALID_FORMAT(400, "CSVの内容に誤りがあります"),
    UNAUTHORIZED(401, "ログインしてください"),
    FORBIDDEN(403, "この操作を行う権限がありません"),
    INVALID_SHARE_TOKEN(403, "このURLは無効になっています。運営者に確認してください"),
    NOT_FOUND(404, "リソースが見つかりません"),
    TOURNAMENT_NOT_FOUND(404, "大会が見つかりません"),
    PARTICIPANT_NOT_FOUND(404, "参加者が見つかりません"),
    ROUND_NOT_FOUND(404, "ラウンドが見つかりません"),
    MATCH_NOT_FOUND(404, "対局が見つかりません"),
    GROUP_NOT_FOUND(404, "グループが見つかりません"),
    INVALID_STATE(409, "現在の状態ではこの操作はできません"),
    CONFLICT(409, "ほかの端末で更新されました。画面を更新して再度お試しください"),
    ROUND_ALREADY_EXISTS(409, "このラウンドは既に生成されています"),
    PAIRING_FAILED(422, "組み合わせを生成できませんでした"),
    RATE_LIMITED(429, "しばらく時間をおいて再度お試しください"),
    INTERNAL_ERROR(500, "予期しないエラーが発生しました");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
