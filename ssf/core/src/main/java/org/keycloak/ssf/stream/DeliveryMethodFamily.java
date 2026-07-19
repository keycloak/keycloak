/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

/**
 * Coarse-grained classification of {@link DeliveryMethod} into the two
 * delivery families operators reason about: PUSH (transmitter calls out
 * to receiver) and POLL (receiver pulls from transmitter). Used by the
 * per-receiver {@code ssf.allowedDeliveryMethods} client-attribute gate
 * so admins configure one of two values rather than enumerating the
 * spec-standard plus legacy SSE-CAEP RISC URI variants individually.
 *
 * <p>Use {@link DeliveryMethod#family()} to map a concrete method onto
 * its family or {@link #ofMethodValue(String)} to parse a wire/attribute
 * value (lowercase {@code push} / {@code poll}, case-insensitive on input).
 */
public enum DeliveryMethodFamily {

    /** Transmitter-initiated HTTP POST (RFC 8935 + legacy RISC PUSH). */
    PUSH,

    /** Receiver-initiated HTTP GET (RFC 8936 + legacy RISC POLL). */
    POLL;

    /**
     * Parses an attribute / wire value into a {@link DeliveryMethodFamily}.
     * Case-insensitive; whitespace-tolerant. Returns {@code null} for
     * unrecognised input rather than throwing — callers typically treat
     * unknown entries as "skip this entry" when reading the per-client
     * allowed-methods list.
     */
    public static DeliveryMethodFamily ofMethodValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "push" -> PUSH;
            case "poll" -> POLL;
            default -> null;
        };
    }

    /** Canonical lowercase string used in client-attribute storage. */
    public String getValue() {
        return name().toLowerCase();
    }
}
