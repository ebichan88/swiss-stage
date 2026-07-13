package com.swiss_stage.application.exception;

/** 403: 権限なし・無効な共有トークン */
public class ForbiddenException extends AppException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
