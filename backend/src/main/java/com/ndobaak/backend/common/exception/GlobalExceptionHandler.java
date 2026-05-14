package com.ndobaak.backend.common.exception;

import com.ndobaak.backend.common.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<ApiError.FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(fe -> ApiError.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message(ErrorCode.VALIDATION_ERROR.getMessage())
                .retryable(ErrorCode.VALIDATION_ERROR.isRetryable())
                .errors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code(ex.getErrorCode().name())
                .message(ex.getErrorCode().getMessage())
                .retryable(ex.isRetryable())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex) {
        ApiError error = ApiError.builder()
                .code(ex.getErrorCode().name())
                .message(ex.getErrorCode().getMessage())
                .retryable(ex.isRetryable())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        ApiError error = ApiError.builder()
                .code(ex.getErrorCode().name())
                .message(resolveMessage(ex))
                .retryable(ex.isRetryable())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
        ErrorCode code = ex instanceof UnauthorizedException unauthorized
                ? unauthorized.getErrorCode()
                : ErrorCode.INVALID_CREDENTIALS;
        ApiError error = ApiError.builder()
                .code(code.name())
                .message(code.getMessage())
                .retryable(code.isRetryable())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleForbidden(RuntimeException ex) {
        ErrorCode code = ex instanceof ForbiddenException forbidden
                ? forbidden.getErrorCode()
                : ErrorCode.FORBIDDEN;
        ApiError error = ApiError.builder()
                .code(code.name())
                .message(resolveMessage(code, ex))
                .retryable(code.isRetryable())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ApiError error = ApiError.builder()
                .code(ErrorCode.UPLOAD_TOO_LARGE.name())
                .message(ErrorCode.UPLOAD_TOO_LARGE.getMessage())
                .retryable(ErrorCode.UPLOAD_TOO_LARGE.isRetryable())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        ApiError error = ApiError.builder()
                .code(ErrorCode.BAD_REQUEST.name())
                .message(ErrorCode.BAD_REQUEST.getMessage())
                .retryable(ErrorCode.BAD_REQUEST.isRetryable())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError error = ApiError.builder()
                .code(ErrorCode.INTERNAL_ERROR.name())
                .message(ErrorCode.INTERNAL_ERROR.getMessage())
                .retryable(ErrorCode.INTERNAL_ERROR.isRetryable())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String resolveMessage(ApiException ex) {
        return resolveMessage(ex.getErrorCode(), (RuntimeException) ex);
    }

    private String resolveMessage(ErrorCode code, RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? code.getMessage() : message;
    }
}
