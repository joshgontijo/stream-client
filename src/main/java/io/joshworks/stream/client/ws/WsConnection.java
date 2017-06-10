package io.joshworks.stream.client.ws;

import io.joshworks.stream.client.ConnectionMonitor;
import io.joshworks.stream.client.StreamConnection;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.protocol.framed.AbstractFramedChannel;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class WsConnection extends StreamConnection {

    private static final Logger logger = LoggerFactory.getLogger(WsConnection.class);

    private final String url;
    private final XnioWorker worker;
    private final boolean autoReconnect;
    private final WebSocketClientEndpoint endpoint;

    private boolean clientClose = false;
    private WebSocketChannel webSocketChannel;


    WsConnection(String url, XnioWorker worker, int maxRetries, int retryInterval, boolean autoReconnect,
                 ScheduledExecutorService scheduler, ConnectionMonitor monitor, WebSocketClientEndpoint endpoint) {
        super(url, scheduler, monitor, retryInterval, maxRetries, autoReconnect);
        this.url = url;
        this.worker = worker;
        this.autoReconnect = autoReconnect;
        this.endpoint = endpoint;
    }

    public synchronized void connect() {
        try {
            if (webSocketChannel != null) {
                return;
            }
            shuttingDown = false;

            logger.info("Connecting to {}", url);
            webSocketChannel = new WebSocketClient.ConnectionBuilder(
                    worker,
                    new DefaultByteBufferPool(false, 2048), //TODO configurable ?
                    URI.create(url))
                    .connect()
                    .get();


            ProxyClientEndpoint proxyClientEndpoint = new ProxyClientEndpoint(endpoint);

            webSocketChannel.getReceiveSetter().set(proxyClientEndpoint);
            webSocketChannel.getCloseSetter().set((ChannelListener<AbstractFramedChannel>) channel -> {
                if(!clientClose) {
                    closeChannel();
                    proxyClientEndpoint.onCloseMessage(null, webSocketChannel);
                    retry(true);
                }
            });

            proxyClientEndpoint.onConnect(webSocketChannel);
            webSocketChannel.resumeReceives();

            monitor.add(uuid, this::closeChannel);
            logger.info("Connected to {}", url);
            clientClose = false;

        } catch (Exception e) {
            logger.warn("Could not connect to " + url, e);
            close();
            retry(false);
        }
    }

    public boolean isOpen() {
        return webSocketChannel != null && webSocketChannel.isOpen();
    }

    public void close() {
        close(new CloseMessage(CloseMessage.NORMAL_CLOSURE, "Client disconnected"));
    }

    public void close(CloseMessage closeMessage) {
        sendClose(closeMessage);
        closeChannel();
    }

    private synchronized void closeChannel() {
        if (webSocketChannel != null) {
            super.closeChannel(webSocketChannel);
            clientClose = true;
            webSocketChannel = null;
            monitor.remove(uuid);
        }
    }

    private void sendClose(CloseMessage closeMessage) {
        try {
            if (webSocketChannel != null && webSocketChannel.isOpen()) {
                webSocketChannel.setCloseCode(closeMessage.getCode());
                webSocketChannel.setCloseReason(closeMessage.getReason());
                webSocketChannel.sendClose();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while sending shutdown message", e);
        }
    }

    public WebSocketChannel channel() {
        return webSocketChannel;
    }

    public void sendText(String message) {
        WebSockets.sendText(message, webSocketChannel, null);
    }

    public void sendBinary(ByteBuffer byteBuffer) {
        WebSockets.sendBinary(byteBuffer, webSocketChannel, null);
    }

    public void sendBinary(byte[] bytes) {
        WebSockets.sendBinary(ByteBuffer.wrap(bytes), webSocketChannel, null);
    }


}
