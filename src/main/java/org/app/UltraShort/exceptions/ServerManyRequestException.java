package org.app.UltraShort.exceptions;

public class ServerManyRequestException extends RuntimeException {

    public ServerManyRequestException() {
        super("Too many requests..");
    }
    public ServerManyRequestException(String message) {
        super(message);
    }
}
