package com.swiss_stage.application.exception;

/** 422: マッチング不能(絶対制約を緩和しても解がない等) */
public class PairingException extends AppException {

    public PairingException(String userMessage) {
        super(ErrorCode.PAIRING_FAILED, userMessage);
    }
}
