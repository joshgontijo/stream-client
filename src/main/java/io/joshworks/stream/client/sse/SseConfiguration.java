package io.joshworks.stream.client.sse;

import org.xnio.XnioWorker;

import java.util.function.Consumer;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class SseConfiguration {
    private final String url;
    private final XnioWorker worker;
    private String lastEventId;

    private int reconnectInterval = 2000;
    private int maxRetries = -1;

    private Runnable onOpen = () -> {};
    private Consumer<EventData> onEvent = (eventData) -> {};
    private Consumer<String> onClose = (lastEventId) -> {};
    private Consumer<Exception> onError = (e) -> {};

    public SseConfiguration(String url, XnioWorker worker) {
        this.url = url;
        this.worker = worker;
    }

    public SseConfiguration(String url, XnioWorker worker, SseClientCallback clientCallback) {
        this(url, worker);
        this.onOpen = clientCallback::onOpen;
        this.onEvent = clientCallback::onEvent;
        this.onError = clientCallback::onError;
    }

    public SseConfiguration onOpen(Runnable onOpen) {
        this.onOpen = onOpen;
        return this;
    }

    public SseConfiguration onEvent(Consumer<EventData> onEvent) {
        this.onEvent = onEvent;
        return this;
    }

    public SseConfiguration onClose(Consumer<String> onClose) {
        this.onClose = onClose;
        return this;
    }

    public SseConfiguration onError(Consumer<Exception> onError) {
        this.onError = onError;
        return this;
    }

    public SseConfiguration lastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
        return this;
    }

    public SseConfiguration reconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
        return this;
    }

    public SseConfiguration maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }


    public SSEConnection connect() {
        SseClientCallback callback = new SseClientCallback() {
            @Override
            public void onEvent(EventData event) {
                onEvent.accept(event);
            }

            @Override
            public void onOpen() {
                onOpen.run();
            }

            @Override
            public void onClose(String lastEventId) {
                onClose.accept(lastEventId);
            }

            @Override
            public void onError(Exception e) {
                onError.accept(e);
            }
        };

        SSEConnection connection = new SSEConnection(url, callback, worker, reconnectInterval, maxRetries);
        connection.connect(lastEventId);
        return connection;
    }



}
