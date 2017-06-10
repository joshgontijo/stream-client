package io.joshworks.stream.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Josh Gontijo on 6/9/17.
 */
public abstract class StreamConnection {

    private static final Logger logger = LoggerFactory.getLogger(StreamConnection.class);

    protected String url;
    protected final ScheduledExecutorService scheduler;
    protected final String uuid;

    protected final ConnectionMonitor monitor;
    private final long reconnectInterval;
    private final int maxRetries;
    private int retries = 0;
    protected boolean shuttingDown = false;
    private boolean autoReconnect = false;

    public StreamConnection(String url, ScheduledExecutorService scheduler, ConnectionMonitor monitor,
                            long reconnectInterval, int maxRetries, boolean autoReconnect) {
        this.url = url;
        this.scheduler = scheduler;
        this.monitor = monitor;
        this.reconnectInterval = reconnectInterval;
        this.maxRetries = maxRetries;
        this.autoReconnect = autoReconnect;
        this.uuid = UUID.randomUUID().toString().substring(0, 8);
    }

    public abstract void connect();

    protected void closeChannel(Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("Error while closing channel", e);
            }
        }
    }

    protected void retry(boolean isReconnection) {
        if (retries++ > maxRetries && maxRetries >= 0) {
            throw new MaxRetryExceeded("Max retries (" + maxRetries + ") exceeded, not reconnecting");
        }
        if (shuttingDown || (isReconnection && !autoReconnect)) {
            return;
        }
        logger.info("Trying to connect to {} in {}ms, retry {} of {}", url, reconnectInterval, retries, maxRetries);
        try {
            scheduler.schedule(() -> {
                this.connect();
                retries = 0;
            }, reconnectInterval, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Error while scheduling reconnection", e);
        }

    }

}
