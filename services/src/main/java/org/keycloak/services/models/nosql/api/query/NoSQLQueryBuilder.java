package org.keycloak.services.models.nosql.api.query;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class NoSQLQueryBuilder {

    private Map<String, Object> queryAttributes = new HashMap<String, Object>();

    protected NoSQLQueryBuilder() {};

    public NoSQLQuery build() {
        return new NoSQLQuery(queryAttributes);
    }

    public NoSQLQueryBuilder andCondition(String name, Object value) {
        this.put(name, value);
        return this;
    }

    public abstract NoSQLQueryBuilder inCondition(String name, Object[] values);

    protected void put(String name, Object value) {
        queryAttributes.put(name, value);
    }

}
