package com.gmasdata.common.exception;

public class ParseException extends RuntimeException {

    public ParseException(String msg) {
        super(msg);
    }

    public ParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
