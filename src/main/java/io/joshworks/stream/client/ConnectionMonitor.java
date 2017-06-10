package io.joshworks.stream.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Josh Gontijo on 6/9/17.
 */
public class ConnectionMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionMonitor.class);
    private static final Map<String, Runnable> connections = new ConcurrentHashMap<>();

    public void add(String uuid, Runnable close) {
        connections.put(uuid, close);
    }

    public void remove(String uuid) {
        connections.remove(uuid);
    }

    public void closeAll() {
        for (Map.Entry<String, Runnable> entry : connections.entrySet()) {
            try {
                logger.info("Shutting down client connection with uuid: {}", entry.getKey());
                entry.getValue().run();
            } catch (Exception ignore) {

            }
        }

    }

}
