package com.swiss_stage.presentation;

import com.swiss_stage.application.dto.FieldErrorDto;
import com.swiss_stage.application.exception.AppException;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.DuplicateRoundException;
import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.presentation.api.ApiError;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 例外の一元処理(06_error_handling_design.md §3)。
 * 内部情報(スタックトレース・キー構造)と個人情報(氏名・所属)はレスポンス・ログに出さない。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleApp(AppException e) {
        logByLevel(e.errorCode(), e.userMessage(), e);
        return respond(e.errorCode(), e.userMessage(), e.details());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        List<FieldErrorDto> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(),
                        fe.getDefaultMessage() == null ? "不正な値です" : fe.getDefaultMessage()))
                .toList();
        log.info("errorCode={} fields={}", ErrorCode.VALIDATION_ERROR,
                details.stream().map(FieldErrorDto::field).toList());
        return respond(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception e) {
        log.info("errorCode={} type={}", ErrorCode.VALIDATION_ERROR, e.getClass().getSimpleName());
        return respond(ErrorCode.VALIDATION_ERROR, "リクエストの形式が不正です", List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadSize(MaxUploadSizeExceededException e) {
        log.info("errorCode={} uploadSizeExceeded", ErrorCode.CSV_INVALID_FORMAT);
        return respond(ErrorCode.CSV_INVALID_FORMAT,
                "ファイルサイズが上限(1MB)を超えています", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.defaultMessage(), List.of());
    }

    /** 楽観ロック競合(リポジトリ実装から到達した場合の最終防衛線) */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockException e) {
        log.info("errorCode={} {}", ErrorCode.CONFLICT, e.getMessage());
        return respond(ErrorCode.CONFLICT, ErrorCode.CONFLICT.defaultMessage(), List.of());
    }

    @ExceptionHandler(DuplicateRoundException.class)
    public ResponseEntity<ApiError> handleDuplicateRound(DuplicateRoundException e) {
        log.info("errorCode={} {}", ErrorCode.ROUND_ALREADY_EXISTS, e.getMessage());
        return respond(ErrorCode.ROUND_ALREADY_EXISTS, e.getMessage(), List.of());
    }

    /** ドメインルール違反(状態遷移等)。メッセージはそのままUI表示可能な日本語 */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException e) {
        log.info("errorCode={} {}", ErrorCode.INVALID_STATE, e.getMessage());
        return respond(ErrorCode.INVALID_STATE, e.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        log.error("errorCode={} unexpected error", ErrorCode.INTERNAL_ERROR, e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), List.of());
    }

    private static void logByLevel(ErrorCode code, String message, AppException e) {
        if (code.httpStatus() >= 500) {
            log.error("errorCode={} {}", code, message, e);
        } else if (code == ErrorCode.FORBIDDEN
                || code == ErrorCode.RATE_LIMITED
                || code == ErrorCode.PAIRING_FAILED) {
            log.warn("errorCode={} {}", code, message);
        } else {
            log.info("errorCode={} {}", code, message);
        }
    }

    private static ResponseEntity<ApiError> respond(
            ErrorCode code, String message, List<FieldErrorDto> details) {
        return ResponseEntity.status(HttpStatus.valueOf(code.httpStatus()))
                .body(ApiError.of(code, message, details));
    }
}
