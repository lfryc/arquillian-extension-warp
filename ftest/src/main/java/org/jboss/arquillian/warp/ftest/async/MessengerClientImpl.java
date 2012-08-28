package org.jboss.arquillian.warp.ftest.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.arquillian.warp.utils.SerializationUtils;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class MessengerClientImpl implements MessengerClient {

    private String url;
    private AsyncHttpClient client = new AsyncHttpClient();
    private ExecutorService executor = Executors.newScheduledThreadPool(10);
    private boolean active = false;

    public MessengerClientImpl(String url) {
        this.url = url;
    }

    @Override
    public ServerRunnable runSync(ServerRunnable sync) throws InterruptedException, ExecutionException {
        Future<ServerRunnable> f = runAsync(sync);
        return f.get();
    }

    @Override
    public Future<ServerRunnable> runAsync(ServerRunnable async) {

        final String serialized = SerializationUtils.serializeToBase64(async);

        return executor.submit(new Callable<ServerRunnable>() {
            @Override
            public ServerRunnable call() throws Exception {
                Future<Response> f = client.preparePut(url).setBody(serialized).execute();

                Response r = f.get();
                if (r.getStatusCode() != 200) {
                    throw new IllegalStateException("Server runnable execution failed");
                }

                ServerRunnable deserialized = SerializationUtils.deserializeFromBase64(r.getResponseBody());

                return deserialized;
            }
        });
    }

    @Override
    public void process(ClientRunnable runnable) {
        runnable.run();
    }
    
    public void connect() {
        
        executor.submit(new Runnable() {
            
            @Override
            public void run() {
                active = true;
                String body = "";
                
                try {
                    while (active) {
                        Future<Response> f = client.preparePost(url).setBody(body).execute();
                        Response response = f.get();
                        String serialized = response.getResponseBody();
                        
                        if (!"".equals(serialized)) {
                            ReverseRequest reverseRequest = SerializationUtils.deserializeFromBase64(serialized);
                            if (reverseRequest != null) {
                                ClientRunnable runnable = SerializationUtils.deserializeFromBase64(reverseRequest.getPayload());
                                
                                process(runnable);
                                
                                String payload = SerializationUtils.serializeToBase64(runnable);
                                reverseRequest = new ReverseRequest(reverseRequest.getSerial(), payload);
                                
                                body = SerializationUtils.serializeToBase64(reverseRequest);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public void disconnect() {
        this.active = false;
        client.closeAsynchronously();
    }
}
