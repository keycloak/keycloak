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
package org.keycloak.dom.saml.v2.metadata;

/**
 * <p>
 * Java class for ContactTypeType.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 *
 * <pre>
 * &lt;simpleType name="ContactTypeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="technical"/>
 *     &lt;enumeration value="support"/>
 *     &lt;enumeration value="administrative"/>
 *     &lt;enumeration value="billing"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
public enum ContactTypeType {
    TECHNICAL("technical"), SUPPORT("support"), ADMINISTRATIVE("administrative"), BILLING("billing"), OTHER("other");
    private final String value;

    ContactTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ContactTypeType fromValue(String v) {
        for (ContactTypeType c : ContactTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
