package io.joshworks.stream.client;

/**
 * Created by Josh Gontijo on 6/9/17.
 */
public class StreamConnectionError extends RuntimeException {
    public StreamConnectionError(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamConnectionError(Throwable cause) {
        super(cause);
    }
}
