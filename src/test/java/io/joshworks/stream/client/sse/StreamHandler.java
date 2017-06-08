package io.joshworks.stream.client.sse;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class StreamHandler extends SseClientCallback {

    @Override
    public void onEvent(EventData event) {
        System.out.println("Event received: " + event);
    }

    @Override
    public void onOpen() {
        System.out.println("Connected");
    }

    @Override
    public void onClose(String lastEventId) {
        System.out.println("Closed, last event id: " + lastEventId);
    }

    @Override
    public void onError(Exception e) {
        System.err.println(e.getMessage());
    }
}
