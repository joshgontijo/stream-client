package io.joshworks.stream.client;

/**
 * Created by Josh Gontijo on 6/9/17.
 */
public class MaxRetryExceeded extends RuntimeException {
    MaxRetryExceeded(String message) {
        super(message);
    }
}
