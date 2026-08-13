/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.representations.idm;

import java.util.Objects;

import org.keycloak.common.util.ObjectUtil;

/**
 * Value object to represent an OID (object identifier) as used to describe LDAP schema, extension and features.
 * See <a href="https://ldap.com/ldap-oid-reference-guide/">LDAP OID Reference Guide</a>.
 *
 * @author Lars Uffmann, 2020-05-13
 * @since 11.0
 */
public class LDAPCapabilityRepresentation {

    public enum CapabilityType {
        CONTROL,
        EXTENSION,
        FEATURE,
        UNKNOWN;

        public static CapabilityType fromRootDseAttributeName(String attributeName) {
            switch (attributeName) {
                case "supportedExtension": return CapabilityType.EXTENSION;
                case "supportedControl": return CapabilityType.CONTROL;
                case "supportedFeatures": return CapabilityType.FEATURE;
                default: return CapabilityType.UNKNOWN;
            }
        }
    };

    private Object oid;

    private CapabilityType type;

    public LDAPCapabilityRepresentation() {
    }

    public LDAPCapabilityRepresentation(Object oidValue, CapabilityType type) {
        this.oid = Objects.requireNonNull(oidValue);
        this.type = type;
    }

    public String getOid() {
        return oid instanceof String ? (String) oid : String.valueOf(oid);
    }

    public CapabilityType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LDAPCapabilityRepresentation ldapOid = (LDAPCapabilityRepresentation) o;
        return ObjectUtil.isEqualOrBothNull(oid, ldapOid.oid) && ObjectUtil.isEqualOrBothNull(type, ldapOid.type);
    }

    @Override
    public int hashCode() {
        return oid.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(LDAPCapabilityRepresentation.class.getSimpleName() + "[ ")
                .append("oid=" + oid + ", ")
                .append("type=" + type + " ]")
                .toString();
    }
}
