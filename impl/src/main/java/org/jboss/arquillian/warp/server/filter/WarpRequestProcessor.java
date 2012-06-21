package org.jboss.arquillian.warp.server.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.spi.Manager;
import org.jboss.arquillian.core.spi.ManagerBuilder;
import org.jboss.arquillian.warp.ServerAssertion;
import org.jboss.arquillian.warp.shared.ResponsePayload;
import org.jboss.arquillian.warp.spi.WarpCommons;
import org.jboss.arquillian.warp.utils.SerializationUtils;

public class WarpRequestProcessor {

    private static final String DEFAULT_EXTENSION_CLASS = "org.jboss.arquillian.core.impl.loadable.LoadableExtensionLoader";

    private HttpServletRequest request;
    private HttpServletResponse response;

    private NonWritingServletOutputStream stream;
    private NonWritingPrintWriter writer;

    private HttpServletResponseWrapper nonWritingResponse;

    public WarpRequestProcessor(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.nonWritingResponse = new NonWritingResponseWrapper(response);
    }

    public void process(WarpRequest warpRequest, DoFilterCommand filterCommand) throws IOException {

        ResponsePayload responsePayload;
        boolean requestFailed = false;

        try {
            ManagerBuilder builder = ManagerBuilder.from().extension(Class.forName(DEFAULT_EXTENSION_CLASS));
            Manager manager = builder.create();
            manager.start();
            manager.bind(ApplicationScoped.class, Manager.class, manager);

            WarpLifecycle warpRoot = new WarpLifecycle();
            manager.inject(warpRoot);

            ServerAssertion serverAssertion = warpRequest.getServerAssertion();

            filterCommand.setRequest(request);
            filterCommand.setResponse(nonWritingResponse);

            System.out.println(request.getServletPath());
            responsePayload = warpRoot.execute(manager, request, filterCommand, serverAssertion);
        } catch (Throwable e) {
            // TODO log exception properly
            e.printStackTrace();
            responsePayload = new ResponsePayload(e);
            requestFailed = true;
        }

        
        if (responsePayload.getThrowable() != null) {
            responsePayload.getThrowable().printStackTrace();
        }
        
        enrichResponse(response, responsePayload);

        if (writer != null) {
            writer.finallyWriteAndClose(response.getOutputStream());
        }
        if (stream != null) {
            stream.finallyWriteAndClose(response.getOutputStream());
        }

        if (requestFailed) {
            response.sendError(500);
        }
    }

    private void enrichResponse(HttpServletResponse httpResp, ResponsePayload payload) {
        String enrichment = SerializationUtils.serializeToBase64(payload);
        httpResp.setHeader(WarpCommons.ENRICHMENT_RESPONSE, enrichment);
    }

    private class NonWritingResponseWrapper extends HttpServletResponseWrapper {

        public NonWritingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            stream = new NonWritingServletOutputStream();
            return stream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            writer = NonWritingPrintWriter.newInstance();
            return writer;
        }
    }
}
