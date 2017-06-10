package io.joshworks.stream.client.ws;

import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class WebSocketClientEndpoint {

    protected void onConnect(WebSocketChannel channel) {
    }

    /**
     * Called when the server closes the connection
     * @param channel The client channel
     * @param message the close message sent by the server, may be null
     */
    protected void onClose(WebSocketChannel channel, CloseMessage message) {
    }

    protected void onPing(WebSocketChannel channel, BufferedBinaryMessage message) {
    }

    protected void onPong(WebSocketChannel channel, BufferedBinaryMessage message) {
    }

    protected void onText(WebSocketChannel channel, BufferedTextMessage message)  {
    }

    protected void onBinary(WebSocketChannel channel, BufferedBinaryMessage message) {
    }

    protected void onError(WebSocketChannel channel, Exception error)  {
    }

}
