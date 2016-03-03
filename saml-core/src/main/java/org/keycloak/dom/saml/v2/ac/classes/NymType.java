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

package org.keycloak.dom.saml.v2.ac.classes;

/**
 * <p>
 * Java class for nymType.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 *
 * <pre>
 * &lt;simpleType name="nymType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="anonymity"/>
 *     &lt;enumeration value="verinymity"/>
 *     &lt;enumeration value="pseudonymity"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
public enum NymType {

    ANONYMITY("anonymity"), VERINYMITY("verinymity"), PSEUDONYMITY("pseudonymity");
    private final String value;

    NymType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static NymType fromValue(String v) {
        for (NymType c : NymType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
