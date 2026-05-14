package com.ndobaak.backend.common.exception;

public class UnauthorizedException extends RuntimeException implements ApiException {

    private final ErrorCode errorCode;

    public UnauthorizedException(String message) {
        this(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
