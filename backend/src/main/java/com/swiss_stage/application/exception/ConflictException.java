package com.swiss_stage.application.exception;

/** 409: 更新競合(楽観ロック)・ラウンド二重生成 */
public class ConflictException extends AppException {

    public ConflictException() {
        super(ErrorCode.CONFLICT);
    }

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
