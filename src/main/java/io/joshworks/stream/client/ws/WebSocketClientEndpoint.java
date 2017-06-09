package io.joshworks.stream.client.ws;

import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class WebSocketClientEndpoint {

    protected void onConnect(WebSocketChannel channel, WebSocketHttpExchange exchange) {
    }

    protected void onClose(WebSocketChannel channel, CloseMessage message) {
    }

    protected void onPing(WebSocketChannel channel, BufferedBinaryMessage message) {
    }

    protected void onPong(WebSocketChannel channel, BufferedBinaryMessage message) {
    }

    protected void onText(WebSocketChannel channel, BufferedTextMessage message) {
    }

    protected void onBinary(WebSocketChannel channel, BufferedBinaryMessage message) {
    }

    protected void onError(WebSocketChannel channel, Exception error) {
    }

}
