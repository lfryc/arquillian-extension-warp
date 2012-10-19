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

import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.warp.impl.client.enrichment.ResponseDeenrichmentService;
import org.jboss.arquillian.warp.impl.client.event.FilterHttpResponse;
import org.jboss.arquillian.warp.impl.client.proxy.ResponseDeenrichmentFilter;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class DefaultResponseDeenrichmentFilter implements ResponseDeenrichmentFilter {

    @Inject
    private Event<FilterHttpResponse> tryDeenrichResponse;

    private ResponseDeenrichmentService deenrichmentService;

    @Override
    public int getMaxResponseSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean filterResponses(HttpRequest httpRequest) {
        return true;
    }

    @Override
    public HttpResponse filterResponse(HttpRequest request, HttpResponse response) {
        tryDeenrichResponse.fire(new FilterHttpResponse(request, response, deenrichmentService));

        return response;
    }

    @Override
    public void setDeenrichmentService(ResponseDeenrichmentService deenrichmentService) {
        this.deenrichmentService = deenrichmentService;
    }
}
