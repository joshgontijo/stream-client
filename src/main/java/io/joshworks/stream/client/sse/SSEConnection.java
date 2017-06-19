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


import io.joshworks.stream.client.ClientConfiguration;
import io.joshworks.stream.client.ClientException;
import io.joshworks.stream.client.StreamConnection;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientStatistics;
import io.undertow.client.UndertowClient;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channel;


/**
 * Created by Josh Gontijo on 4/1/17.
 */
public class SSEConnection extends StreamConnection {

    private static final Logger logger = LoggerFactory.getLogger(SSEConnection.class);

    final SseClientCallback callback;
    private ClientConnection connection;
    String lastEventId; //updated from EventStreamParser

    public SSEConnection(ClientConfiguration clientConfiguration, String lastEventId, SseClientCallback callback) {
        super(clientConfiguration);
        this.lastEventId = lastEventId;
        this.callback = callback;
    }

    @Override
    protected synchronized void tryConnect() throws Exception {
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
            if (this.lastEventId != null && !this.lastEventId.isEmpty()) {
                request.getRequestHeaders().put(HttpString.tryFromString("Last-Event-ID"), this.lastEventId);
            }

            connection.sendRequest(request, createClientCallback());

            //TODO Connection exception and then retry
        } catch (Exception e) {
            logger.warn("Could not connect to " + url, e);
            try {
                callback.onError(e);
            } catch (Exception ex) {
                logger.error(e.getMessage(), e);
            }
            throw e;
        }
    }


    /**
     * Close the this connection and return the Last-Event-ID
     *
     * @return Last-Event-ID if any
     */
    public String close() {
        shuttingDown = true;
        closeChannel();
        return lastEventId;
    }

    public ClientStatistics statistics() {
        return connection == null ? new DisconnectedStatistics() : connection.getStatistics();
    }

    protected void closeChannel() {
        if (connection != null) {
            StreamConnection.closeChannel(connection);
            connection = null;
            callback.onClose(lastEventId);
        }
        monitor.remove(uuid);
    }


    public boolean isOpen() {
        return connection != null;
    }

    //Used only for Retry-After header
    void retryAfter(long timeMilli) {
        logger.info("Reconnecting after {}ms", timeMilli);
        reconnect(timeMilli);
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
                reconnect();
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
                closeChannel();
                return;
            }

            callback.onOpen();

            result.getResponseChannel().getCloseSetter().set((ChannelListener<Channel>) channel -> {
                closeChannel();
                reconnect();
            });

            listener.setup(result.getResponseChannel());

        }

        @Override
        public void failed(IOException e) {
            callback.onError(e);
        }
    }

    public class DisconnectedStatistics implements ClientStatistics {

        @Override
        public long getRequests() {
            return 0;
        }

        @Override
        public long getRead() {
            return 0;
        }

        @Override
        public long getWritten() {
            return 0;
        }

        @Override
        public void reset() {

        }
    }
}
