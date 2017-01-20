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

import org.keycloak.storage.ldap.idm.store.ldap.LDAPUtil;

import java.util.Date;

/**
 * @author Pedro Igor
 */
class BetweenCondition extends NamedParameterCondition {

    private final Comparable x;
    private final Comparable y;

    public BetweenCondition(String name, Comparable x, Comparable y) {
        super(name);
        this.x = x;
        this.y = y;
    }

    @Override
    public void applyCondition(StringBuilder filter) {
        Comparable x = this.x;
        Comparable y = this.y;

        if (Date.class.isInstance(x)) {
            x = LDAPUtil.formatDate((Date) x);
        }

        if (Date.class.isInstance(y)) {
            y = LDAPUtil.formatDate((Date) y);
        }

        filter.append("(").append(x).append("<=").append(getParameterName()).append("<=").append(y).append(")");
    }
}
