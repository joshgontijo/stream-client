package io.joshworks.stream.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;

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

    protected final String url;
    protected final XnioWorker worker;
    protected final String uuid;
    protected final ConnectionMonitor monitor;
    private final ScheduledExecutorService scheduler;

    private final long retryInterval;
    private final int maxRetries;

    private final Runnable onFailedAttempt;
    private final Runnable onRetriesExceeded;

    protected boolean shuttingDown = false;
    private int retries = 0;

    public StreamConnection(ClientConfiguration clientConfiguration) {
        this.uuid = UUID.randomUUID().toString().substring(0, 8);
        this.url = clientConfiguration.url;
        this.scheduler = clientConfiguration.scheduler;
        this.monitor = clientConfiguration.monitor;
        this.retryInterval = clientConfiguration.retryInterval;
        this.maxRetries = clientConfiguration.maxRetries;
        this.worker = clientConfiguration.worker;
        this.onFailedAttempt = clientConfiguration.onFailedAttempt;
        this.onRetriesExceeded = clientConfiguration.onRetriesExceeded;
    }

    protected abstract void tryConnect() throws Exception;

    protected abstract void closeChannel();

    public void connect() {
        retries = 0;
        shuttingDown = false;
        this.tryConnect(0);
    }

    protected static void closeChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("Error while closing channel", e);
        }
    }

    protected void reconnect() {
        reconnect(retryInterval);
    }

    protected void reconnect(long delay) {
        if (shuttingDown || maxRetries == 0) {
            return;
        }
        if (++retries > maxRetries && maxRetries > 0) {
            onRetriesExceeded.run();
            MaxRetryExceeded maxRetryExceeded = new MaxRetryExceeded("Max retries (" + maxRetries + ") exceeded, not reconnecting");
            logger.error("Max retries exceeded", maxRetryExceeded);
            closeChannel();
            return;
        }
        this.tryConnect(delay);
    }

    private void tryConnect(long delay) {
        String maxRetriesLabel = maxRetries < 0 ? "-" : "" + maxRetries;
        logger.info("Trying to connect to {} in {}ms. {} of {}", url, retryInterval, retries, maxRetriesLabel);
        try {
            if (scheduler.isTerminated() || scheduler.isShutdown()) {
                logger.warn("Scheduler service shutdown, not reconnecting");
                return;
            }
            scheduler.schedule(() -> {
                try {
                    this.tryConnect();
                    retries = 0;
                    logger.info("Connected to {}", url);
                } catch (Exception e) {
                    logger.warn("Could not connect to {}: {}", url, e.getMessage());
                    onFailedAttempt.run();
                    closeChannel();
                    reconnect();
                }

            }, delay, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Error while scheduling reconnection", e);
        }
    }


}
