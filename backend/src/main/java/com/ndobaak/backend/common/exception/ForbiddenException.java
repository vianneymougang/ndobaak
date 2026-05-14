package com.ndobaak.backend.common.exception;

public class ForbiddenException extends RuntimeException implements ApiException {

    private final ErrorCode errorCode;

    public ForbiddenException(String message) {
        super(message);
        this.errorCode = ErrorCode.FORBIDDEN;
    }

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN.getMessage());
        this.errorCode = ErrorCode.FORBIDDEN;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
