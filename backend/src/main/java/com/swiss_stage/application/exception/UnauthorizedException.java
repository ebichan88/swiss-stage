package com.swiss_stage.application.exception;

/** 401: 未認証 */
public class UnauthorizedException extends AppException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
