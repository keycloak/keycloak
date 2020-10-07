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

package org.keycloak.storage.ldap.idm.query.internal;

import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPUtil;

import java.util.Date;

/**
 * @author Pedro Igor
 */
public class EqualCondition extends NamedParameterCondition {

    private final EscapeStrategy escapeStrategy;
    private Object value;

    public EqualCondition(String name, Object value, EscapeStrategy escapeStrategy) {
        super(name);
        this.value = value;
        this.escapeStrategy = escapeStrategy;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public EscapeStrategy getEscapeStrategy() {
        return escapeStrategy;
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        Object parameterValue = value;
        if (Date.class.isInstance(value)) {
            parameterValue = LDAPUtil.formatDate((Date) parameterValue);
        }

        String escaped = new OctetStringEncoder(escapeStrategy).encode(parameterValue, isBinary());

        filter.append("(").append(getParameterName()).append(LDAPConstants.EQUAL).append(escaped).append(")");
    }

    @Override
    public String toString() {
        return "EqualCondition{" +
                "paramName=" + getParameterName() +
                ", value=" + value +
                ", escapeStrategy=" + escapeStrategy +
                '}';
    }
}
