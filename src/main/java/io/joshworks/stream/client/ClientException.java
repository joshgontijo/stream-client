package io.joshworks.stream.client;

/**
 * Created by Josh Gontijo on 6/2/17.
 */
public class ClientException extends RuntimeException {

    private final int status;

    public ClientException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
