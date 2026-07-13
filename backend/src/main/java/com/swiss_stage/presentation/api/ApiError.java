package com.swiss_stage.presentation.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swiss_stage.application.dto.FieldErrorDto;
import com.swiss_stage.application.exception.ErrorCode;
import java.util.List;

/** エラーレスポンスの統一フォーマット(03_api_design.md §2)。detailsは空なら出力しない */
public record ApiError(boolean success, Body error) {

    public record Body(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorDto> details) {}

    public static ApiError of(ErrorCode code, String message, List<FieldErrorDto> details) {
        return new ApiError(false, new Body(code.name(), message, details));
    }
}
