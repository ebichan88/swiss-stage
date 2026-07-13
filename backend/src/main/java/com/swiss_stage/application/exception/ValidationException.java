package com.swiss_stage.application.exception;

import com.swiss_stage.application.dto.FieldErrorDto;
import java.util.List;

/** 400: 入力不正(CSV形式不正 CSV_INVALID_FORMAT を含む) */
public class ValidationException extends AppException {

    public ValidationException(String userMessage) {
        super(ErrorCode.VALIDATION_ERROR, userMessage);
    }

    public ValidationException(ErrorCode errorCode, String userMessage, List<FieldErrorDto> details) {
        super(errorCode, userMessage, details);
    }
}
