package io.joshworks.stream.client;

import org.xnio.XnioWorker;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Josh Gontijo on 6/9/17.
 */
public class ClientConfiguration {

    protected final String url;
    protected final XnioWorker worker;
    protected final ScheduledExecutorService scheduler;
    protected final ConnectionMonitor monitor;

    protected int retryInterval = 2000;
    protected int maxRetries = -1;
    protected boolean autoReconnect = true;

    public ClientConfiguration(String url, XnioWorker worker, ScheduledExecutorService scheduler, ConnectionMonitor monitor) {
        this.url = url;
        this.worker = worker;
        this.scheduler = scheduler;
        this.monitor = monitor;
    }
}
