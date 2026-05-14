package com.ndobaak.backend.common.exception;

public interface ApiException {

    ErrorCode getErrorCode();

    default boolean isRetryable() {
        return getErrorCode().isRetryable();
    }
}
