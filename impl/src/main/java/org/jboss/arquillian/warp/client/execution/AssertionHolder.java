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
package org.jboss.arquillian.warp.client.execution;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.arquillian.core.spi.Validate;
import org.jboss.arquillian.warp.ServerAssertion;
import org.jboss.arquillian.warp.shared.RequestPayload;
import org.jboss.arquillian.warp.shared.ResponsePayload;

/**
 * The holder for {@link ServerAssertion} object.
 * 
 * Provides methods for settings up the server assertion and its retrieval.
 * 
 * @author Lukas Fryc
 */
class AssertionHolder {
    
    private static volatile int counter = 0;

    private static final long WAIT_TIMEOUT_MILISECONDS = 5000;
    private static final long THREAD_SLEEP = 50;
    private static final long NUMBER_OF_WAIT_LOOPS = WAIT_TIMEOUT_MILISECONDS / THREAD_SLEEP;

    private static final AtomicBoolean advertisement = new AtomicBoolean();
    private static final AtomicReference<RequestPayload> request = new AtomicReference<RequestPayload>(null);
    private static final AtomicReference<ResponsePayload> response = new AtomicReference<ResponsePayload>(null);

    /**
     * Advertizes that there will be taken client action which will lead into request.
     */
    public static void advertise() {
        advertisement.set(true);
    }

    /**
     * Returns true if there is client action advertised, see {@link #advertise()}.
     * 
     * @return true if there is client action advertised, see {@link #advertise()}.
     */
    private static boolean isAdvertised() {
        return advertisement.get();
    }

    /**
     * Returns true if there is {@link ServerAssertion} pushed for current request.
     * 
     * @return true if there is {@link ServerAssertion} pushed for current request.
     */
    private static boolean isEnriched() {
        return request.get() != null;
    }

    /**
     * Returns true if the {@link ServerAssertion} is waiting for verification or the client action which should cause request
     * is advertised.
     * 
     * @return true if the {@link ServerAssertion} is waiting for verification or the client action which should cause request
     *         is advertised.
     */
    static boolean isWaitingForProcessing() {
        return isAdvertised() || isEnriched();
    }

    /**
     * <p>
     * Pushes the {@link RequestPayload} to verify on the server.
     * </p>
     * 
     * <p>
     * This method cancels flag set by {@link #advertise()}.
     * 
     * @param payload to verify on the server
     */
    public static void pushRequest(RequestPayload payload) {
        Validate.notNull(payload, "payload can't be null");
        System.out.println("pushRequest " + ++counter);

        Object oldRequest = request.getAndSet(payload);
        Object oldResponse = response.getAndSet(null);
        advertisement.set(false);

        assertNull(oldRequest, "request must be cleared after last successful request");
        assertNull(oldResponse, "response must be cleared after last successful request");
    }

    private static final void assertNull(Object object, String message) {
        if (object != null) {
            throw new IllegalStateException(message);
        }
    }

    private static final void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Waits until the {@link ServerAssertion} for request is available and returns it.
     * 
     * @return the associated {@link ServerAssertion}
     * @throws SettingRequestTimeoutException when {@link ServerAssertion} isn't setup in time
     */
    static RequestPayload popRequest() {
        System.out.println("popRequest");

        awaitRequest();
        RequestPayload payload = request.getAndSet(null);

        assertNotNull(payload, "request payload can't be null");
        System.out.println("- poppedRequest " + counter);

        return payload;
    }

    /**
     * Pushes the verified {@link ResponsePayload} to be obtained by test.
     * 
     * @param payload verified {@link ResponsePayload} to be obtained by test.
     */
    static void pushResponse(ResponsePayload payload) {
        Validate.notNull(payload, "payload can't be null");
        System.out.println("pushResponse " + counter);

        Object oldResponse = response.getAndSet(payload);

        assertNull(oldResponse, "response must be null when pushing new response");
    }

    /**
     * Waits until the for response is available and returns it.
     * 
     * @return the {@link ResponsePayload}
     * @throws ServerResponseTimeoutException when the response wasn't returned in time
     */
    public static ResponsePayload popResponse() {
        System.out.println("popResponse");
        awaitResponse();
        ResponsePayload payload = response.getAndSet(null);

        assertNotNull(payload, "request payload can't be null");
        System.out.println("- poppedResponse " + counter);

        return payload;
    }

    private static void awaitRequest() {
        if (!isAdvertised()) {
            return;
        }
        for (int i = 0; i < NUMBER_OF_WAIT_LOOPS; i++) {
            try {
                Thread.sleep(THREAD_SLEEP);
                if (!isAdvertised()) {
                    return;
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new SettingRequestTimeoutException();
    }

    private static void awaitResponse() {
        if (response.get() != null) {
            return;
        }
        for (int i = 0; i < NUMBER_OF_WAIT_LOOPS; i++) {
            try {
                Thread.sleep(THREAD_SLEEP);
                if (response.get() != null) {
                    return;
                }
            } catch (InterruptedException e) {

            }
        }
        throw new ServerResponseTimeoutException();
    }

    public static class SettingRequestTimeoutException extends RuntimeException {
        private static final long serialVersionUID = -6743564150233628034L;
    }

    public static class ServerResponseTimeoutException extends RuntimeException {
        private static final long serialVersionUID = 7267806785171391801L;
    }

    public static class RequestPayloadAlreadySetException extends RuntimeException {
        private static final long serialVersionUID = 8333157142743791135L;
    }
}
