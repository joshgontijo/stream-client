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

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Josh Gontijo on 3/31/17.
 */
public class EventStreamChannelListener implements ChannelListener<StreamSourceChannel> {

    private static final Logger logger = LoggerFactory.getLogger(EventStreamChannelListener.class);

    private final UTF8Output string;
    private final ByteBufferPool bufferPool;

    public EventStreamChannelListener(final ByteBufferPool bufferPool, UTF8Output utf8Output) {
        this.bufferPool = bufferPool;
        this.string = utf8Output;
    }

    @Override
    public void handleEvent(StreamSourceChannel channel) {
        process(channel);
    }

    public void setup(final StreamSourceChannel channel) {
        process(channel);
        channel.getReadSetter().set(this);
        channel.resumeReads();
    }

    private void process(final StreamSourceChannel channel) {
        PooledByteBuffer resource = bufferPool.allocate();
        ByteBuffer buffer = resource.getBuffer();
        try {
            int read;
            do {
                read = channel.read(buffer);
                if (read == 0) {
                    return;
                } else if (read == -1) {
                    IoUtils.safeClose(channel);
                } else {
                    buffer.flip();
                    string.write(buffer);
                }
            } while (read > 0);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            resource.close();
        }
    }

}
