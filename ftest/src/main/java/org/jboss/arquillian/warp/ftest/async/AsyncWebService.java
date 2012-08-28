package org.jboss.arquillian.warp.ftest.async;

import javax.servlet.AsyncContext;

public class AsyncWebService implements Runnable {
    private AsyncContext ctx;

    public AsyncWebService(AsyncContext ctx) {
        this.ctx = ctx;
    }

    public void run() {
        ctx.dispatch("/source.txt");
    }
}