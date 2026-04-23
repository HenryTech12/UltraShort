package org.app.UltraShort.exceptions;

public class ServerFailedException extends RuntimeException {

    public ServerFailedException() {
        super("an error occurred!!, server is down");
    }
    public ServerFailedException(String message) {
        super(message);
    }
}
