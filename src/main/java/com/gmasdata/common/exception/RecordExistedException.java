package com.gmasdata.common.exception;


public class RecordExistedException extends RuntimeException {
    public RecordExistedException(String message) {
        super(message);
    }
}
