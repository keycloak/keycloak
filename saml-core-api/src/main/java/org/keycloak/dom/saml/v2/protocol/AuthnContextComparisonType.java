/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.dom.saml.v2.protocol;

/**
 * <p>
 * Java class for AuthnContextComparisonType.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 *
 * <pre>
 * &lt;simpleType name="AuthnContextComparisonType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="exact"/>
 *     &lt;enumeration value="minimum"/>
 *     &lt;enumeration value="maximum"/>
 *     &lt;enumeration value="better"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
public enum AuthnContextComparisonType {
    EXACT("exact"), MINIMUM("minimum"), MAXIMUM("maximum"), BETTER("better");

    private final String value;

    AuthnContextComparisonType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AuthnContextComparisonType fromValue(String v) {
        for (AuthnContextComparisonType c : AuthnContextComparisonType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}