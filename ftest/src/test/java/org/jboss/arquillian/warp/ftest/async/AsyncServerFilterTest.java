package org.jboss.arquillian.warp.ftest.async;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.StringBuilderWriter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.warp.utils.SerializationUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class AsyncServerFilterTest {

    @ArquillianResource(value = AsyncServlet.class)
    URL contextPath;

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "async.war").addPackage(AsyncServlet.class.getPackage())
                .addPackage(SerializationUtils.class.getPackage())
                .addClasses(IOUtils.class, ByteArrayOutputStream.class, StringBuilderWriter.class)
                .addAsWebResource(new StringAsset("some-content"), "source.txt");
    }

    @Test
    public void testClientRunSync() throws IOException, InterruptedException, ExecutionException {
        MessengerClient client = new MessengerClientImpl(contextPath + "/async");
        client.runSync(new PutRunnable());
    }

    @Test
    public void testClientRunAsync() throws IOException, InterruptedException, ExecutionException {
        MessengerClient client = new MessengerClientImpl(contextPath + "/async");
        Future<ServerRunnable> future = client.runAsync(new PutRunnable());
        future.get();
    }

    @Test
    public void testConnectServer() {
        MessengerClientImpl client = new MessengerClientImpl(contextPath + "/async");
        client.connect();
        try {
            client.runSync(new PostRunnable());
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.disconnect();
    }

    private static class PutRunnable implements ServerRunnable {
        @Override
        public void run() {
            System.out.println("xyz");
        }
    }

    private static class PostRunnable implements ServerRunnable {

        @Override
        public void run() {
            MessengerServer messenger = AsyncServlet.getMessenger();
            System.out.println("triggering client action");
            try {
                messenger.runSync(new ClientPostRunnable());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("client action ended");
        }
    }

    private static class ClientPostRunnable implements ClientRunnable {

        @Override
        public void run() {
            System.out.println("client action!!");
        }
    }

}
