/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Type of credential offer uri to be returned.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public enum OfferUriType {

    URI("uri"),

    QR_CODE("qr-code");

    private final String value;

    OfferUriType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OfferUriType fromString(String value) {
        return Optional.ofNullable(value)
                .map(v -> {
                    if (v.equals(URI.getValue())) {
                        return URI;
                    } else if (v.equals(QR_CODE.getValue())) {
                        return QR_CODE;
                    } else return null;
                })
                .orElseThrow(() -> new IllegalArgumentException(String.format("%s is not a supported OfferUriType.", value)));
    }
}