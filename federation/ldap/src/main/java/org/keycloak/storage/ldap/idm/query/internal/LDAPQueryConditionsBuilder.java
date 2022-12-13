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

import org.keycloak.models.ModelException;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.Sort;

/**
 * @author Pedro Igor
 */
public class LDAPQueryConditionsBuilder {

    public Condition equal(String parameter, Object value) {
        return new EqualCondition(parameter, value, EscapeStrategy.DEFAULT);
    }

    public Condition equal(String parameter, Object value, EscapeStrategy escapeStrategy) {
        return new EqualCondition(parameter, value, escapeStrategy);
    }

    public Condition greaterThan(String paramName, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(paramName, (Comparable) x, false);
    }

    public Condition greaterThanOrEqualTo(String paramName, Object x) {
        throwExceptionIfNotComparable(x);
        return new GreaterThanCondition(paramName, (Comparable) x, true);
    }

    public Condition lessThan(String paramName, Comparable x) {
        return new LessThanCondition(paramName, x, false);
    }

    public Condition lessThanOrEqualTo(String paramName, Comparable x) {
        return new LessThanCondition(paramName, x, true);
    }

    public Condition between(String paramName, Comparable x, Comparable y) {
        return new BetweenCondition(paramName, x, y);
    }

    public Condition andCondition(Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            throw new ModelException("At least one condition should be provided to AND query");
        }
        return new AndCondition(conditions);
    }

    public Condition orCondition(Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            throw new ModelException("At least one condition should be provided to OR query");
        }
        return new OrCondition(conditions);
    }

    public Condition notCondition(Condition condition) {
        if (condition == null) {
            throw new ModelException("One condition should be provided to NOT query");
        }
        return new NotCondition(condition);
    }

    public Condition addCustomLDAPFilter(String filter) {
        filter = filter.trim();
        return new CustomLDAPFilter(filter);
    }

    public Condition in(String paramName, Object... x) {
        return new InCondition(paramName, x);
    }

    public Sort asc(String paramName) {
        return new Sort(paramName, true);
    }

    public Sort desc(String paramName) {
        return new Sort(paramName, false);
    }

    private void throwExceptionIfNotComparable(Object x) {
        if (!Comparable.class.isInstance(x)) {
            throw new ModelException("Query parameter value [" + x + "] must be " + Comparable.class + ".");
        }
    }
}