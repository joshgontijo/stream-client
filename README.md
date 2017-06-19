# Stream client
Resilient client for Server sent events (SSE) and WebSockets based on Undertow


### Maven

```xml
    <dependency>
        <groupId>io.joshworks.stream</groupId>
        <artifactId>stream-client</artifactId>
        <version>0.3</version>
    </dependency>
```


## Server sent events ##

### Using fluent interface to connect to a SSE endpoint
```java
public class App {

    public static void main(final String[] args) {
      
        SSEConnection connection = StreamClient.sse("http://my-service/sse")
                        .onEvent((data) -> System.out.println("New event: " + data))
                        .connect();
                        
                        //onClose
                        //onOpen
                        //OnError
                        
        
        
        //then...
        String lastEventId = connection.close();
        
    }
}
```

### Using dedicated handler to connect to a SSE endpoint
```java

public class StreamHandler extends SseClientCallback {

        @Override
        public void onEvent(EventData event) {
            System.out.println("New event: " + event);
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

public class App {

    public static void main(final String[] args) {
      
        SSEConnection connection = StreamClient.connect("http://my-service/sse", new StreamHandler());
                             
        //then...
        String lastEventId = connection.close();
        
    }
}
```

## Websockets ##

### Using fluent interface to connect to a WebSocket endpoint
```java
public class App {

    public static void main(final String[] args) {
        
     WsConnection ws = StreamClient.ws("http://my-service/ws")
                    .onText((channel, message) -> System.out.println("Message received: " + message))
                    .connect();
    
            //...
            ws.close(new CloseMessage(CloseMessage.NORMAL_CLOSURE, "Bye"));
    
        }
}
```

### Using dedicated handler to connect to a WebSocket endpoint
```java

public class MyEndpoint extends WebSocketClientEndpoint {

    //..method signatures for brevity
    protected void onConnect(WebSocketChannel channel, WebSocketHttpExchange exchange);
    protected void onClose(WebSocketChannel channel, CloseMessage message) ;
    protected void onPing(WebSocketChannel channel, BufferedBinaryMessage message);
    protected void onPong(WebSocketChannel channel, BufferedBinaryMessage message);
    protected void onText(WebSocketChannel channel, BufferedTextMessage message);
    protected void onBinary(WebSocketChannel channel, BufferedBinaryMessage message);
    protected void onError(WebSocketChannel channel, Exception error);

}

public class App {

    public static void main(final String[] args) {
    
           StreamClient.connect("http://my-service/ws", new MyEndpoint());
    
    }
}
```

### Sending message to a server
```java

public class App {

    public static void main(final String[] args) {
    
           WsConnection ws = StreamClient.connect(...);
           
           //undertow API
           WebSockets.sendText("Hello", ws.channel(), null);
           WebSockets.sendBinary(new byte[]{1}, ws.channel(), null);
           //...etc
    
    }
}
```

### Connection retry
Disabled by default, to enable use `maxRetries(int retries)`, optionally use `.retryInterval(long intervalMillis)` (default is 2000) 
```java

public class App {

    public static void main(final String[] args) {
    
           StreamClient.ws("http://my-service/ws")
                    .maxRetries(100)
                    .retryInterval(5000)
                    .connect();
    
    }
}
```


## XnioWorker configuration ##
The XnioWorker is shared across all clients (SSE and WS), in case of many connections, the thread pool can be tuned

```java
public class App {

    public static void main(final String[] args) {
      
         OptionMap options = OptionMap.builder()
                    .set(Options.WORKER_IO_THREADS, 10)
                    .set(Options.TCP_NODELAY, true)
                    .set(Options.WORKER_NAME, "client-threads")
                    .set(Options.KEEP_ALIVE, true)
                    .getMap();
        
        StreamClient.configure(options);
        
        StreamClient.sse(...);
        StreamClient.ws(...);
        
        //...
    }
}
```

### Closing all connections ###

```java
public class App {

    public static void main(final String[] args) {
     
        //...
        StreamClient.shutdown();
    }
}
```