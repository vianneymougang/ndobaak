package com.ndobaak.backend.common.exception;

public class DuplicateResourceException extends RuntimeException implements ApiException {

    private final ErrorCode errorCode;

    public DuplicateResourceException(String message) {
        super(message);
        this.errorCode = ErrorCode.DUPLICATE_RESOURCE;
    }

    public DuplicateResourceException() {
        super(ErrorCode.DUPLICATE_RESOURCE.getMessage());
        this.errorCode = ErrorCode.DUPLICATE_RESOURCE;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
