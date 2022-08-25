/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.store.LdapMapUtil;
import org.keycloak.models.map.storage.ldap.store.LdapMapEscapeStrategy;
import org.keycloak.models.map.storage.ldap.store.LdapMapOctetStringEncoder;

import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract class containing methods common to all Ldap*ModelCriteriaBuilder implementations
 * 
 * @param <E> Entity
 * @param <M> Model
 * @param <Self> specific implementation of this class
 */
public abstract class LdapModelCriteriaBuilder<E, M, Self extends LdapModelCriteriaBuilder<E, M, Self>> implements ModelCriteriaBuilder<M, Self> {

    private final Function<Supplier<StringBuilder>, Self> instantiator;
    private Supplier<StringBuilder> predicateFunc = null;

    public LdapModelCriteriaBuilder(Function<Supplier<StringBuilder>, Self> instantiator) {
        this.instantiator = instantiator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Self and(Self... builders) {
        return instantiator.apply(() -> {
            StringBuilder filter = new StringBuilder();
            for (Self builder : builders) {
                filter.append(builder.getPredicateFunc().get());
            }
            if (filter.length() > 0) {
                filter.insert(0, "(&");
                filter.append(")");
            }
            return filter;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Self or(Self... builders) {
        return instantiator.apply(() -> {
            StringBuilder filter = new StringBuilder();
            filter.append("(|");
            for (Self builder : builders) {
                filter.append(builder.getPredicateFunc().get());
            }
            filter.append(")");
            return filter;
        });
    }

    @Override
    public Self not(Self builder) {
        return instantiator.apply(() -> {
            StringBuilder filter = new StringBuilder();
            filter.append("(!");
            filter.append(builder.getPredicateFunc().get());
            filter.append(")");
            return filter;
        });
    }

    public Supplier<StringBuilder> getPredicateFunc() {
        return predicateFunc;
    }

    public LdapModelCriteriaBuilder(Function<Supplier<StringBuilder>, Self> instantiator,
                                    Supplier<StringBuilder> predicateFunc) {
        this.instantiator = instantiator;
        this.predicateFunc = predicateFunc;
    }

    protected StringBuilder equal(String field, Object value, LdapMapEscapeStrategy ldapMapEscapeStrategy, boolean isBinary) {
        Object parameterValue = value;
        if (value instanceof Date) {
            parameterValue = LdapMapUtil.formatDate((Date) parameterValue);
        }

        String escaped = new LdapMapOctetStringEncoder(ldapMapEscapeStrategy).encode(parameterValue, isBinary);

        return new StringBuilder().append("(").append(field).append(LDAPConstants.EQUAL).append(escaped).append(")");
    }

    protected StringBuilder in(String name, Object[] valuesToCompare, boolean isBinary) {
        StringBuilder filter = new StringBuilder();
        filter.append("(|(");

        for (Object o : valuesToCompare) {
            Object value = new LdapMapOctetStringEncoder().encode(o, false);

            filter.append("(").append(name).append(LDAPConstants.EQUAL).append(value).append(")");
        }

        filter.append("))");
        return filter;
    }

}
