/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.ssf.transmitter.delivery.push;

/**
 * Structured outcome of a single push attempt against a receiver
 * endpoint. Replaces the prior boolean return so the
 * {@link org.keycloak.ssf.transmitter.outbox.SsfPushDeliveryHandler
 * SsfPushDeliveryHandler} can surface the receiver's HTTP status and
 * body — or the underlying transport exception — into the outbox row's
 * {@code last_error} summary and {@code metadata.lastFailure}
 * structured detail.
 *
 * <p>Three terminal shapes:
 * <ul>
 *   <li>{@link #delivered(int, String) delivered}: receiver replied
 *       2xx. Carries the status for completeness; no error fields set.</li>
 *   <li>{@link #httpFailure(int, String, String) httpFailure}: receiver
 *       replied non-2xx. Carries status + (optionally truncated)
 *       response body for the operator's view.</li>
 *   <li>{@link #transportFailure(Throwable, String) transportFailure}:
 *       no HTTP response — DNS lookup failed, connection refused,
 *       socket timeout, etc. Carries the exception class name +
 *       message; status / body are null.</li>
 * </ul>
 */
public record PushDeliveryOutcome(boolean delivered,
                                  Integer status,
                                  String responseBody,
                                  String exceptionClass,
                                  String exceptionMessage,
                                  String endpointUrl) {

    public static PushDeliveryOutcome delivered(int status, String endpointUrl) {
        return new PushDeliveryOutcome(true, status, null, null, null, endpointUrl);
    }

    public static PushDeliveryOutcome httpFailure(int status, String responseBody, String endpointUrl) {
        return new PushDeliveryOutcome(false, status, responseBody, null, null, endpointUrl);
    }

    public static PushDeliveryOutcome transportFailure(Throwable t, String endpointUrl) {
        String exClass = t.getClass().getName();
        String exMessage = t.getMessage();
        return new PushDeliveryOutcome(false, null, null, exClass, exMessage, endpointUrl);
    }

    /**
     * Used when the stream config itself is malformed (no endpoint URL,
     * no delivery section). No HTTP attempt is made; the outcome
     * carries a synthetic {@code exceptionClass} marker so the handler
     * can recognize the case and route it to ORPHANED rather than
     * RETRY.
     */
    public static PushDeliveryOutcome invalidConfig(String reason) {
        return new PushDeliveryOutcome(false, null, null, "InvalidStreamConfig", reason, null);
    }
}
