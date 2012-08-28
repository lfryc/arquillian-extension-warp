package org.jboss.arquillian.warp.ftest.async;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.arquillian.warp.utils.SerializationUtils;

public class MessengerServerImpl implements MessengerServer {

    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private Queue<ReverseRequest> queue = new LinkedBlockingQueue<ReverseRequest>();
    private Map<Long, String> returnedBack = new ConcurrentHashMap<Long, String>();
    private long serial;

    @Override
    public ClientRunnable runSync(ClientRunnable sync) throws InterruptedException, ExecutionException {
        Future<ClientRunnable> async = runAsync(sync);
        return async.get();
    }

    @Override
    public Future<ClientRunnable> runAsync(ClientRunnable async) {

        final String serialized = SerializationUtils.serializeToBase64(async);

        return executor.submit(new Callable<ClientRunnable>() {
            @Override
            public ClientRunnable call() throws Exception {
                long assignedSerial = serial++;
                ReverseRequest reverseRequest = new ReverseRequest(assignedSerial, serialized);

                MessengerServerImpl.this.queue.add(reverseRequest);

                while (!MessengerServerImpl.this.returnedBack.containsKey(assignedSerial)) {
                    Thread.sleep(15);
                }

                String returned = returnedBack.get(assignedSerial);
                
                ClientRunnable runnable = SerializationUtils.deserializeFromBase64(returned);

                return runnable;
            }
        });
    }

    @Override
    public void process(ServerRunnable runnable) {
        runnable.run();
    }

    public ReverseRequest take() {
        return this.queue.poll();
    }

    public void put(ReverseRequest message) {
        this.returnedBack.put(message.getSerial(), message.getPayload());
    }
    
    public void destroy() {
        this.executor.shutdown();
    }

}
