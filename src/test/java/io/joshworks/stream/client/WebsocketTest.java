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

package io.joshworks.stream.client;

import io.joshworks.snappy.websocket.WebsocketEndpoint;
import io.joshworks.stream.client.ws.WebSocketClientEndpoint;
import io.joshworks.stream.client.ws.WsConnection;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.joshworks.snappy.SnappyServer.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by josh on 3/10/17.
 */
public class WebsocketTest {

    private static final String WS_ENDPOINT = "ws://localhost:9000/ws";

    private static final String message = "Yolo";

    private static final int CLOSE_CODE = 1001;

    @Before
    public void setup() {
        websocket("/ws", (exchange, channel) -> {
            WebSockets.sendText(message, channel, null);
        });

        websocket("/ws-close", new WebsocketEndpoint() {
            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                try {
                    channel.sendClose();
                    channel.setCloseCode(CLOSE_CODE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        start();
    }

    @After
    public void shutdown() {
        StreamClient.shutdown();
        stop();
    }

    @Test
    public void connection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();

        WsConnection connection = StreamClient.connect(WS_ENDPOINT, new WebSocketClientEndpoint() {
            @Override
            protected void onText(WebSocketChannel channel, BufferedTextMessage message) {
                try {
                    result.set(message.getData());
                    channel.sendClose();
                    latch.countDown();

                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }


            @Override
            protected void onError(WebSocketChannel channel, Exception error) {
                error.printStackTrace();
                latch.countDown();
            }
        });

        assertTrue(connection.isOpen());
        latch.await(10, TimeUnit.SECONDS);
        assertNotNull(result.get());
    }

    @Test
    public void onConnect() throws Exception {

        final CountDownLatch connected = new CountDownLatch(1);

        WsConnection connection = StreamClient.ws("ws://localhost:9000/ws").onConnect(channel -> {
            assertNotNull(channel);
            connected.countDown();
        }).connect();


        if (!connected.await(10, TimeUnit.SECONDS)) {
            fail("Client did not reconnect");
        }

        assertTrue(connection.isOpen());
    }

    @Test
    public void onClose_byClient() throws Exception {

        final CountDownLatch connected = new CountDownLatch(1);

        //onClose only works for messages sent by the server
        WsConnection connection = StreamClient.ws("ws://localhost:9000/ws")
                .onConnect((channel) -> {
                    connected.countDown();
                }).connect();

        if (!connected.await(10, TimeUnit.SECONDS)) {
            fail("Client did not connect");
        }

        connection.close();
        assertChannelClosed(connection);
    }

    @Test
    public void onClose_byServer() throws Exception {

        final CountDownLatch closed = new CountDownLatch(1);

        WsConnection connection = StreamClient.ws("ws://localhost:9000/ws-close")
                .onClose((channel, exchange) -> {
                    closed.countDown();
                }).connect();

        if (!closed.await(10, TimeUnit.SECONDS)) {
            fail("Client did not reconnect");
        }

        assertChannelClosed(connection);
    }

    @Test
    public void retry() throws Exception {
        stop();

        final CountDownLatch connected = new CountDownLatch(1);

        WsConnection connection = StreamClient.ws("ws://localhost:9000/ws")
                .onConnect(channel -> connected.countDown()).connect();

        assertFalse(connection.isOpen());

        setup(); //start

        if (!connected.await(10, TimeUnit.SECONDS)) {
            fail("Client did not reconnect");
        }

        assertTrue(connection.isOpen());
    }

    @Test
    public void reconnect() throws Exception {
        final CountDownLatch firstConnection = new CountDownLatch(1);
        final CountDownLatch secondConnection = new CountDownLatch(2);

        WsConnection connection = StreamClient.ws("ws://localhost:9000/ws")
                .onConnect(channel -> {
                    firstConnection.countDown();
                    secondConnection.countDown();
                })
                .onClose((channel, message) -> {

                })
                .connect();

        if (!firstConnection.await(10, TimeUnit.SECONDS)) {
            fail("Client did not connect");
        }

        assertTrue(connection.isOpen());

        stop();
        assertChannelClosed(connection);

        setup(); //start
        if (!secondConnection.await(10, TimeUnit.SECONDS)) {
            fail("Client did not reconnect");
        }

        assertTrue(connection.isOpen());
    }

    @Test
    public void do_not_reconnect() throws Exception {
        final CountDownLatch firstConnection = new CountDownLatch(1);
        final CountDownLatch secondConnection = new CountDownLatch(2);

        WsConnection connection = StreamClient.ws("ws://localhost:9000/ws")
                .autoReconnect(false)
                .retryInterval(1000) //must be less than waiting time
                .onConnect(channel -> {
                    firstConnection.countDown();
                    secondConnection.countDown();
                })
                .onClose((channel, message) -> {

                }).connect();

        if (!firstConnection.await(10, TimeUnit.SECONDS)) {
            fail("Client did not connect");
        }

        assertTrue(connection.isOpen());

        stop();
        assertChannelClosed(connection);

        start();

        if (secondConnection.await(5, TimeUnit.SECONDS)) {
            fail("Client shouldn't have connected");
        }

        assertFalse(connection.isOpen());
    }

    private void assertChannelClosed(WsConnection connection) throws InterruptedException {
        boolean isOpen = true;
        int maxTries = 10;
        int count = 0;

        while (isOpen || count++ > maxTries) {
            isOpen = connection.isOpen();
            if (isOpen) {
                Thread.sleep(1000);
            }
        }
        assertFalse(connection.isOpen());
    }

}
