/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.joshworks.stream.client.ws;

import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;

import java.io.IOException;

/**
 * Created by josh on 3/8/17.
 * Internal use only, delegates to parent class to free consume bytes and so on
 */
public class ProxyClientEndpoint extends AbstractReceiveListener {

    private final WebSocketClientEndpoint endpoint;

    public ProxyClientEndpoint(WebSocketClientEndpoint endpoint) {
        this.endpoint = endpoint;
    }


    public void onConnect(WebSocketChannel channel) {
        endpoint.onConnect(channel);
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
        endpoint.onText(channel, message);
        super.onFullTextMessage(channel, message);
    }

    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        endpoint.onBinary(channel, message);
        super.onFullBinaryMessage(channel, message);
    }

    @Override
    protected void onFullPingMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        endpoint.onPing(channel, message);
        super.onFullPingMessage(channel, message);
    }

    @Override
    protected void onFullPongMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        endpoint.onPong(channel, message);
        super.onFullPongMessage(channel, message);
    }

    @Override
    protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
        endpoint.onClose(channel, cm);
        super.onCloseMessage(cm, channel);
    }

    @Override
    protected void onError(WebSocketChannel channel, Throwable error) {
        endpoint.onError(channel, (Exception) error);
        super.onError(channel, error);
    }
}
