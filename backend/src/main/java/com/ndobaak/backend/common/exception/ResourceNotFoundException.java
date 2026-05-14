package com.ndobaak.backend.common.exception;

public class ResourceNotFoundException extends RuntimeException implements ApiException {

    private final ErrorCode errorCode;

    public ResourceNotFoundException(String resource, String field, Object value) {
        this();
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    }

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
        this.errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
