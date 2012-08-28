package org.jboss.arquillian.warp.ftest.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface MessengerClient {

    ServerRunnable runSync(ServerRunnable sync) throws InterruptedException, ExecutionException;

    Future<ServerRunnable> runAsync(ServerRunnable async);
    
    void process(ClientRunnable runnable);
}
