package com.gmasdata.common.exception;

public class FailedToResolveConfigException extends RuntimeException {
    public FailedToResolveConfigException(String msg) {
        super(msg);
    }

    public FailedToResolveConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
