package io.joshworks.stream.client.sse;

import io.joshworks.stream.client.ClientConfiguration;
import io.joshworks.stream.client.ConnectionMonitor;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.xnio.XnioWorker;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class SseConfiguration extends ClientConfiguration {

    private SseClientCallback clientCallback;
    private String lastEventId;

    private Runnable onOpen = () -> {};
    private Consumer<EventData> onEvent = (eventData) -> {};
    private Consumer<String> onClose = (lastEventId) -> {};
    private Consumer<Exception> onError = (e) -> {};
    protected HeaderMap headers = new HeaderMap();


    public SseConfiguration(String url, XnioWorker worker, ScheduledExecutorService scheduler, ConnectionMonitor register) {
        super(url, worker, scheduler, register);
    }

    public SseConfiguration(String url, XnioWorker worker, ScheduledExecutorService scheduler,
                            ConnectionMonitor register, SseClientCallback clientCallback) {
        super(url, worker, scheduler, register);
        this.clientCallback = clientCallback;
    }

    public SseConfiguration header(HttpString name, String value) {
        headers.put(name, value);
        return this;
    }

    public SseConfiguration header(HttpString name, long value) {
        headers.put(name, value);
        return this;
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

    public SseConfiguration onFailedAttempt(Runnable onFailedAttempt) {
        this.onFailedAttempt = onFailedAttempt;
        return this;
    }

    public SseConfiguration onRetriesExceeded(Runnable onRetriesExceeded) {
        this.onRetriesExceeded = onRetriesExceeded;
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

    public SseConfiguration maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public SseConfiguration retryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public SseConfiguration clientCallback(SseClientCallback callback) {
        this.clientCallback = callback;
        return this;
    }

    public SSEConnection connect() {
        clientCallback = clientCallback == null ? createClientCallback() : clientCallback;

        SSEConnection connection = new SSEConnection(this, lastEventId, clientCallback);
        connection.connect();
        return connection;
    }

    public SSEConnection connect(String lastEventId) {
        this.lastEventId = lastEventId;
        return connect();
    }

    private SseClientCallback createClientCallback() {
        return new SseClientCallback() {
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
    }


}
