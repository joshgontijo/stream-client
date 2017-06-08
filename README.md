# Stream client
Simple client for Server sent events (SSE) and Websockets

## Features

- Connection retry
- Based on Undertow NIO
- Simple interface



### Maven

```xml
    <dependency>
        <groupId>io.joshworks.stream</groupId>
        <artifactId>stream-client</artifactId>
        <version>0.1</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>1.7.25</version>
    </dependency>

```


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

### Connection retry and retry interval
```java
public class App {

    public static void main(final String[] args) {
      
        SSEConnection connection = StreamClient.sse("http://my-service/sse")
                              .reconnectInterval(5000)
                              .maxRetries(10)
                              .onEvent((data) -> System.out.println("New event: " + data))
                              .connect();
        
        
                //then...
                String lastEventId = connection.close();
        
    }
}
```

### Configuring worker threads
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
        
        StreamClient.sse("http://my-service/sse")
                              .onEvent((data) -> System.out.println("New event: " + data))
                              .connect();
        
        //...
        
    }
}
```

##Websockets

