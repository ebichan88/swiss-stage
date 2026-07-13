package com.swiss_stage.application.exception;

/** 409: 状態遷移違反 */
public class InvalidStateException extends AppException {

    public InvalidStateException(String userMessage) {
        super(ErrorCode.INVALID_STATE, userMessage);
    }
}
