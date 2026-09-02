/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.keycloak.ssf.stream;

import org.keycloak.ssf.Ssf;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeliveryMethod {

    // Standard SSF Delivery Methods
    PUSH(Ssf.DELIVERY_METHOD_PUSH_URI),
    POLL(Ssf.DELIVERY_METHOD_POLL_URI),

    RISC_PUSH(Ssf.DELIVERY_METHOD_RISC_PUSH_URI),
    RISC_POLL(Ssf.DELIVERY_METHOD_RISC_POLL_URI);

    private final String specUrn;

    DeliveryMethod(String specUrn) {
        this.specUrn = specUrn;
    }

    public static DeliveryMethod valueOfUri(String deliveryMethod) {
        for(DeliveryMethod dm : values()) {
            if (dm.specUrn.equals(deliveryMethod)) {
                return dm;
            }
        }
        throw new IllegalArgumentException("Unknown delivery method: " + deliveryMethod);
    }

    /**
     * Coarse-grained PUSH/POLL family. Both spec variants of each transport
     * (RFC 8935 + legacy RISC PUSH; RFC 8936 + legacy RISC POLL) collapse to
     * the same family so per-client allow-listing operates on the operator's
     * mental model rather than four separate URIs.
     */
    public DeliveryMethodFamily family() {
        return switch (this) {
            case PUSH, RISC_PUSH -> DeliveryMethodFamily.PUSH;
            case POLL, RISC_POLL -> DeliveryMethodFamily.POLL;
        };
    }

    @JsonValue
    public String getSpecUri() {
        return specUrn;
    }

    @Override
    public String toString() {
        return specUrn;
    }
}
