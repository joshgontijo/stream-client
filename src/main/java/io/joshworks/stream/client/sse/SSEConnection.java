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
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.channels.Channel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by Josh Gontijo on 4/1/17.
 */
public class SSEConnection {

    private static final Logger logger = LoggerFactory.getLogger(SSEConnection.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final String url;
    private long reconnectInterval;
    private int maxRetries;
    private int retries = 0;

    final SseClientCallback callback;
    private XnioWorker worker;
    private ClientConnection connection;
    String lastEventId; //updated from EventStreamParser

    public SSEConnection(String url, SseClientCallback callback, XnioWorker worker, int reconnectInterval, int maxRetries) {
        this.url = url;
        this.callback = callback;
        this.worker = worker;
        this.reconnectInterval = reconnectInterval;
        this.maxRetries = maxRetries;
    }


    void connect(String lastEvent) {
        try {
            //avoid override on reconnection
            this.lastEventId = lastEvent == null ? this.lastEventId : lastEvent;

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
            callback.onError(e);
            close(true);
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
        return close(false);
    }

    private String close(boolean retry) {
        if (connection != null) {
            IoUtils.safeClose(connection);
            connection = null;
            callback.onClose(lastEventId);
        }
        if (retry) {
            retry();
        }
        return lastEventId;
    }

    private void retry() {
        if(retries++ > maxRetries && maxRetries >= 0) {
            logger.warn("Max retries ({}) exceeded, not reconnecting");
            return;
        }
        retryAfter(reconnectInterval);
    }

    public boolean isOpen() {
        return connection != null && connection.isOpen();
    }

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
                retries = 0; //successful, reset counter
            }

            @Override
            public void failed(IOException e) {
                callback.onError(e);
                close(true);
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

            result.getResponseChannel().getCloseSetter().set((ChannelListener<Channel>) channel -> close(true));
            listener.setup(result.getResponseChannel());


            result.getResponseChannel().resumeReads();
        }

        @Override
        public void failed(IOException e) {
            callback.onError(e);
        }
    }
}
