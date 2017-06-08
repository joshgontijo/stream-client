package io.joshworks.stream.client.ws;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class WsConnection {

    private static final Logger logger = LoggerFactory.getLogger(WsConnection.class);

    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final WsConfiguration configuration;
    private WebSocketChannel webSocketChannel;

    private int retries = 0;

    WsConnection(WsConfiguration configuration) {
        this.configuration = configuration;
    }

    private void reconnect() {
        int maxRetries = configuration.maxRetries;
        if (retries++ > maxRetries && maxRetries >= 0) {
            logger.warn("Max retries ({}) exceeded, not reconnecting");
            return;
        }
        executor.schedule(this::connect, configuration.reconnectInterval, TimeUnit.MILLISECONDS);
    }

    void connect() {
        String url = configuration.url;
        try {
            webSocketChannel = new WebSocketClient.ConnectionBuilder(
                    configuration.worker,
                    new DefaultByteBufferPool(false, 2048), //TODO configurable ?
                    URI.create(url))
                    .connect()
                    .get();


            ProxyClientEndpoint proxyClientEndpoint = new ProxyClientEndpoint(configuration, this::reconnect);
            webSocketChannel.getReceiveSetter().set(proxyClientEndpoint);
            webSocketChannel.resumeReceives();

            retries = 0;

        } catch (Exception e) {
            logger.warn("Could not connect to " + url, e);
            reconnect();
        }
    }

    public void close() {
        close(new CloseMessage(CloseMessage.NORMAL_CLOSURE, "Client disconnected"));
    }

    public void close(CloseMessage closeMessage) {
        if (webSocketChannel != null) {
            sendClose(closeMessage);
            IoUtils.safeClose(webSocketChannel);
        }
    }

    private void sendClose(CloseMessage closeMessage) {
        try {
            if (webSocketChannel.isOpen()) {
                webSocketChannel.setCloseCode(closeMessage.getCode());
                webSocketChannel.setCloseReason(closeMessage.getReason());
                webSocketChannel.sendClose();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error while sending close message", e);
        }
    }

    public WebSocketChannel channel() {
        return webSocketChannel;
    }

}
