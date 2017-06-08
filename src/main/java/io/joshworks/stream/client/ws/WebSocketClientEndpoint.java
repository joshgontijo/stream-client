package io.joshworks.stream.client.ws;

import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public abstract class WebSocketClientEndpoint {

    protected abstract void onConnect(WebSocketChannel channel, WebSocketHttpExchange exchange);

    protected abstract void onClose(WebSocketChannel channel, CloseMessage message);

    protected abstract void onPing(WebSocketChannel channel, BufferedBinaryMessage message);

    protected abstract void onPong(WebSocketChannel channel, BufferedBinaryMessage message);

    protected abstract void onText(WebSocketChannel channel, BufferedTextMessage message);

    protected abstract void onBinary(WebSocketChannel channel, BufferedBinaryMessage message);

    protected abstract void onError(WebSocketChannel channel, Exception error);

}
