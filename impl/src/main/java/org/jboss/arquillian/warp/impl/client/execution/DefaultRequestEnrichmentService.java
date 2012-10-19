/**
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.warp.impl.client.execution;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.TestResult.Status;
import org.jboss.arquillian.warp.client.filter.RequestFilter;
import org.jboss.arquillian.warp.exception.ClientWarpExecutionException;
import org.jboss.arquillian.warp.impl.client.enrichment.RequestEnrichmentService;
import org.jboss.arquillian.warp.impl.shared.RequestPayload;
import org.jboss.arquillian.warp.impl.shared.ResponsePayload;
import org.jboss.arquillian.warp.impl.utils.SerializationUtils;
import org.jboss.arquillian.warp.spi.WarpCommons;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class DefaultRequestEnrichmentService implements RequestEnrichmentService {

    private Logger log = Logger.getLogger("Warp");

    @Override
    public void enrichRequest(HttpRequest request, Collection<RequestPayload> payloads) {

        if (System.getProperty("arquillian.debug") != null) {
            System.out.println("                (W->) " + request.getUri());
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Warp request: " + request.getUri());
        }

        try {
            RequestPayload assertion = payloads.iterator().next();
            String requestEnrichment = SerializationUtils.serializeToBase64(assertion);
            request.setHeader(WarpCommons.ENRICHMENT_REQUEST, Arrays.asList(requestEnrichment));
        } catch (Throwable originalException) {
            ResponsePayload exceptionPayload = new ResponsePayload();
            ClientWarpExecutionException explainingException = new ClientWarpExecutionException("enriching request failed: "
                    + originalException.getMessage(), originalException);
            exceptionPayload.setTestResult(new TestResult(Status.FAILED, explainingException));
            AssertionHolder.addResponse(new ResponseEnrichment(exceptionPayload));
        }
    }

    @Override
    public Collection<RequestPayload> getMatchingPayloads(HttpRequest request) {
        final Set<RequestEnrichment> requests = AssertionHolder.getRequests();
        final org.jboss.arquillian.warp.client.filter.HttpRequest httpRequest = new HttpRequestWrapper(request);
        final Collection<RequestPayload> payloads = new LinkedList<RequestPayload>();

        for (RequestEnrichment enrichment : requests) {
            RequestFilter<?> filter = enrichment.getFilter();

            if (filter == null) {
                payloads.add(enrichment.getPayload());
                continue;
            }

            if (isType(filter, org.jboss.arquillian.warp.client.filter.HttpRequest.class)) {

                @SuppressWarnings("unchecked")
                RequestFilter<org.jboss.arquillian.warp.client.filter.HttpRequest> httpRequestFilter = (RequestFilter<org.jboss.arquillian.warp.client.filter.HttpRequest>) filter;

                if (httpRequestFilter.matches(httpRequest)) {
                    payloads.add(enrichment.getPayload());
                }
            }
        }

        return payloads;
    }

    private boolean isType(RequestFilter<?> filter, Type expectedType) {
        Type[] interfaces = filter.getClass().getGenericInterfaces();

        for (Type type : interfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parametrizedType = (ParameterizedType) type;
                if (parametrizedType.getRawType() == RequestFilter.class) {
                    return parametrizedType.getActualTypeArguments()[0] == expectedType;
                }
            }
        }

        return false;
    }

    private class HttpRequestWrapper implements org.jboss.arquillian.warp.client.filter.HttpRequest {

        private HttpRequest request;

        public HttpRequestWrapper(HttpRequest request) {
            this.request = request;
        }

        @Override
        public String getMethod() {
            return request.getMethod().getName();
        }

        @Override
        public String getUri() {
            return request.getUri();
        }

    }

}
