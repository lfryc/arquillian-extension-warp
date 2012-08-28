package org.jboss.arquillian.warp.ftest.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface MessengerServer {

    ClientRunnable runSync(ClientRunnable sync) throws InterruptedException, ExecutionException;

    Future<ClientRunnable> runAsync(ClientRunnable async);

    void process(ServerRunnable runnable);
}
