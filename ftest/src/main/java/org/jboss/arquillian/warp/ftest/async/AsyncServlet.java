package org.jboss.arquillian.warp.ftest.async;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.warp.utils.SerializationUtils;

@WebServlet(urlPatterns = "/async", asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private ExecutorService executor = Executors.newCachedThreadPool();
    private static MessengerServerImpl messenger = new MessengerServerImpl();
    
    public static MessengerServer getMessenger() {
        return messenger;
    }
    
    @Override
    public void destroy() {
        executor.shutdown();
        messenger.destroy();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout(10000L);
        PostService getService = new PostService(asyncContext);
        asyncContext.addListener(getService);
        executor.execute(getService);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AsyncContext asyncContext = request.startAsync(request, response);
        executor.execute(new PutService(asyncContext));
    }
    
    private class PostService implements Runnable, AsyncListener {

        private AsyncContext asyncContext;
        private boolean active = true;

        public PostService(AsyncContext ctx) {
            this.asyncContext = ctx;
        }
        
        @Override
        public void run() {
            HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
            HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
            
            pass(request);
            take(response);
        }
        
        private void pass(HttpServletRequest request) {
            try {
                ServletInputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    String serialized = IOUtils.toString(inputStream);
                    
                    if (!"".equals(serialized)) {
                        ReverseRequest put = SerializationUtils.deserializeFromBase64(serialized);
                        messenger.put(put);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private void take(HttpServletResponse response) {
            ReverseRequest take = null;
            
            while (active && take == null) {
                take = messenger.take();
            }
            
            if (take != null) {
                String serialized = SerializationUtils.serializeToBase64(take);
                
                try {
                    response.getWriter().write(serialized);
                    response.getWriter().close();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
        
        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            this.active = false;
        }
        
        @Override
        public void onComplete(AsyncEvent event) throws IOException {}
        
        @Override
        public void onError(AsyncEvent event) throws IOException {}
        
        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {}
        
    }

    private class PutService implements Runnable {

        private AsyncContext asyncContext;

        public PutService(AsyncContext ctx) {
            this.asyncContext = ctx;
        }

        public void run() {
            HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
            HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();

            String answer;
            
            try {
                String serialized = IOUtils.toString(request.getInputStream());
                
                ServerRunnable runnable = SerializationUtils.deserializeFromBase64(serialized);
                
                messenger.process(runnable);
                
                serialized = SerializationUtils.serializeToBase64(runnable);
                
                answer = serialized;
            } catch (Exception e) {
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ioe) {
                }
                answer = e.getMessage();
            }
            
            try {
                response.getWriter().write(answer);
                response.getWriter().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

}
