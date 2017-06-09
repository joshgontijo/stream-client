package io.joshworks.stream.client.sse;

import io.joshworks.stream.client.StreamClient;
import io.joshworks.stream.client.ws.WsConnection;
import io.undertow.websockets.core.CloseMessage;

/**
 * Created by Josh Gontijo on 6/8/17.
 */
public class StreamHandler extends SseClientCallback {

    public static void main(final String[] args) {

        WsConnection connect = StreamClient.ws("http://my-service/sse")
                .onText((channel, message) -> System.out.println("Message received: " + message))
                .connect();


        connect.close(new CloseMessage(CloseMessage.NORMAL_CLOSURE, "Bye"));


        //...

    }

    @Override
    public void onEvent(EventData event) {
        System.out.println("Event received: " + event);
    }

    @Override
    public void onOpen() {
        System.out.println("Connected");
    }

    @Override
    public void onClose(String lastEventId) {
        System.out.println("Closed, last event id: " + lastEventId);
    }

    @Override
    public void onError(Exception e) {
        System.err.println(e.getMessage());
    }
}
