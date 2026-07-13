package com.swiss_stage.application.exception;

import com.swiss_stage.application.dto.FieldErrorDto;
import java.util.List;

/**
 * アプリケーション例外の基底(06_error_handling_design.md §3)。
 * userMessage はそのままUIに表示できる日本語にする。
 */
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient List<FieldErrorDto> details;

    protected AppException(ErrorCode errorCode, String userMessage, List<FieldErrorDto> details) {
        super(userMessage);
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }

    protected AppException(ErrorCode errorCode, String userMessage) {
        this(errorCode, userMessage, List.of());
    }

    protected AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), List.of());
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String userMessage() {
        return getMessage();
    }

    public List<FieldErrorDto> details() {
        return details;
    }
}
