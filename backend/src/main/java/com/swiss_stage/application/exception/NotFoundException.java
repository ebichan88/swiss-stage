package com.swiss_stage.application.exception;

/** 404: リソース未存在(他人の大会へのアクセスも存在を漏らさないため404にする) */
public class NotFoundException extends AppException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
