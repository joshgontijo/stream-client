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

package io.joshworks.stream.client.sse;


import io.joshworks.stream.client.ClientException;
import io.joshworks.stream.client.ConnectionMonitor;
import io.joshworks.stream.client.StreamConnection;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.UndertowClient;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.channels.Channel;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by Josh Gontijo on 4/1/17.
 */
public class SSEConnection extends StreamConnection {

    private static final Logger logger = LoggerFactory.getLogger(SSEConnection.class);

    private final String url;

    final SseClientCallback callback;
    private XnioWorker worker;
    private ClientConnection connection;
    String lastEventId; //updated from EventStreamParser

    public SSEConnection(String url, XnioWorker worker, ScheduledExecutorService scheduler,
                         ConnectionMonitor register, int reconnectInterval, int maxRetries, boolean autoReconnect, SseClientCallback callback) {

        super(url, scheduler, register, reconnectInterval, maxRetries, autoReconnect);
        this.url = url;
        this.callback = callback;
        this.worker = worker;
    }

    @Override
    public void connect() {
        connect(lastEventId);
    }


    public synchronized void connect(String lastEvent) {
        try {
            shuttingDown = false;
            logger.info("Connecting to {}", url);

            if (connection != null) {
                return;
            }
            connection = UndertowClient.getInstance().connect(
                    URI.create(url),
                    worker,
                    new DefaultByteBufferPool(false, 8192),
                    OptionMap.EMPTY)
                    .get();


            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(url);
            request.getRequestHeaders().put(Headers.CONNECTION, "keep-alive");
            request.getRequestHeaders().put(Headers.ACCEPT, "text/event-stream");
            request.getRequestHeaders().put(Headers.HOST, url);
//            request.getRequestHeaders().put(Headers.ORIGIN, "http://localhost");
            if (lastEventId != null && !lastEventId.isEmpty()) {
                request.getRequestHeaders().put(HttpString.tryFromString("Last-Event-ID"), this.lastEventId);
            }

            connection.sendRequest(request, createClientCallback());

        } catch (ConnectException e) {
            logger.warn("Could not connect to " + url, e);
            try {
                callback.onError(e);
            } catch (Exception ex) {
                logger.error(e.getMessage(), e);
            }
            closeChannel();
            retry(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Close the this connection and return the Last-Event-ID
     *
     * @return Last-Event-ID if any
     */
    public String close() {
        shuttingDown = true;
        return closeChannel();
    }

    private String closeChannel() {
        if (connection != null) {
            super.closeChannel(connection);
            connection = null;
            callback.onClose(lastEventId);
        }
        monitor.remove(uuid);
        return lastEventId;
    }


    public boolean isOpen() {
        return connection != null && connection.isOpen();
    }

    //Used only for Retry-After header
    void retryAfter(long timeMilli) {
        logger.info("Reconnecting after {}ms", timeMilli);
        scheduler.schedule(() -> this.connect(lastEventId), timeMilli, TimeUnit.MILLISECONDS);
    }

    private ClientCallback<ClientExchange> createClientCallback() {
        final EventStreamParser eventStreamParser = new EventStreamParser(this);

        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange connectedExchange) {
                connectedExchange.setResponseListener(new StreamHandler(callback, eventStreamParser));
                monitor.add(uuid, () -> close());
                logger.info("Connected to {}", url);
            }

            @Override
            public void failed(IOException e) {
                callback.onError(e);
                closeChannel();
                retry(true);
            }
        };
    }

    private class StreamHandler implements ClientCallback<ClientExchange> {

        private final SseClientCallback callback;
        private final EventStreamChannelListener listener;
        private final UTF8Output dataReader;

        StreamHandler(SseClientCallback callback, EventStreamParser streamParser) {
            this.callback = callback;
            this.dataReader = new UTF8Output(streamParser);
            this.listener = new EventStreamChannelListener(new DefaultByteBufferPool(false, 8192), dataReader);
        }

        @Override
        public void completed(ClientExchange result) {
            int responseCode = result.getResponse().getResponseCode();
            if (responseCode != 200) {
                String status = result.getResponse().getStatus();
                callback.onError(new ClientException(responseCode, "Server returned [" + responseCode + " - " + status + "] after connecting"));
            }
            callback.onOpen();

            result.getResponseChannel().getCloseSetter().set((ChannelListener<Channel>) channel -> {
                closeChannel();
                retry(true);
            });
            listener.setup(result.getResponseChannel());

            result.getResponseChannel().resumeReads();
        }

        @Override
        public void failed(IOException e) {
            callback.onError(e);
        }
    }
}
