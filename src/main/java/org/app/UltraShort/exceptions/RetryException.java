package org.app.UltraShort.exceptions;

public class RetryException extends RuntimeException {


    public RetryException() {
        super("retry mechanism failed..");
    }

    public RetryException(String message) {
        super(message);
    }
}
