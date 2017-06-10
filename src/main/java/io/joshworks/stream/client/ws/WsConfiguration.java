package io.joshworks.stream.client.ws;

import io.joshworks.stream.client.ClientConfiguration;
import io.joshworks.stream.client.ConnectionMonitor;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.xnio.XnioWorker;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class WsConfiguration extends ClientConfiguration {

    private Consumer<WebSocketChannel> onConnect = (channel) -> {
    };
    private BiConsumer<WebSocketChannel, BufferedBinaryMessage> onPing = (wsChannel, channel) -> {
    };
    private BiConsumer<WebSocketChannel, BufferedBinaryMessage> onPong = (wsChannel, channel) -> {
    };
    private BiConsumer<WebSocketChannel, BufferedTextMessage> onText = (wsChannel, channel) -> {
    };
    private BiConsumer<WebSocketChannel, BufferedBinaryMessage> onBinary = (wsChannel, channel) -> {
    };
    private BiConsumer<WebSocketChannel, Exception> onError = (wsChannel, error) -> {
    };
    private BiConsumer<WebSocketChannel, CloseMessage> onClose = (wsChannel, message) -> {
    };

    private WebSocketClientEndpoint endpoint;


    public WsConfiguration(String url, XnioWorker worker, ScheduledExecutorService scheduler, ConnectionMonitor monitor) {
        super(url, worker, scheduler, monitor);
    }

    public WsConfiguration(String url, XnioWorker worker, ScheduledExecutorService scheduler,
                           ConnectionMonitor monitor, WebSocketClientEndpoint endpoint) {
        super(url, worker, scheduler, monitor);
        this.endpoint = endpoint;
    }

    public WsConfiguration onConnect(Consumer<WebSocketChannel> onConnect) {
        this.onConnect = onConnect;
        return this;
    }

    public WsConfiguration onClose(BiConsumer<WebSocketChannel, CloseMessage> onClose) {
        this.onClose = onClose;
        return this;
    }

    public WsConfiguration onPing(BiConsumer<WebSocketChannel, BufferedBinaryMessage> onPing) {
        this.onPing = onPing;
        return this;
    }

    public WsConfiguration onPong(BiConsumer<WebSocketChannel, BufferedBinaryMessage> onPong) {
        this.onPong = onPong;
        return this;
    }

    public WsConfiguration onText(BiConsumer<WebSocketChannel, BufferedTextMessage> onText) {
        this.onText = onText;
        return this;
    }

    public WsConfiguration onBinary(BiConsumer<WebSocketChannel, BufferedBinaryMessage> onBinary) {
        this.onBinary = onBinary;
        return this;
    }

    public WsConfiguration onError(BiConsumer<WebSocketChannel, Exception> onError) {
        this.onError = onError;
        return this;
    }

    public WsConfiguration retryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public WsConfiguration maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public WsConfiguration autoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    public WsConnection connect() {
        endpoint = endpoint == null ? createEndpoint() : endpoint;
        WsConnection wsConnection = new WsConnection(url, worker, maxRetries, retryInterval, autoReconnect, scheduler, monitor, endpoint);
        wsConnection.connect();

        return wsConnection;
    }

    private WebSocketClientEndpoint createEndpoint() {
        return new WebSocketClientEndpoint() {
            @Override
            protected void onConnect(WebSocketChannel channel) {
                onConnect.accept(channel);
            }

            @Override
            protected void onClose(WebSocketChannel channel, CloseMessage message) {
                onClose.accept(channel, message);
            }

            @Override
            protected void onPing(WebSocketChannel channel, BufferedBinaryMessage message) {
                onPing.accept(channel, message);
            }

            @Override
            protected void onPong(WebSocketChannel channel, BufferedBinaryMessage message) {
                onPong.accept(channel, message);
            }

            @Override
            protected void onText(WebSocketChannel channel, BufferedTextMessage message) {
                onText.accept(channel, message);
            }

            @Override
            protected void onBinary(WebSocketChannel channel, BufferedBinaryMessage message) {
                onBinary.accept(channel, message);
            }

            @Override
            protected void onError(WebSocketChannel channel, Exception error) {
                onError.accept(channel, error);
            }
        };
    }

}
