package org.keycloak.services.models.nosql.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NoSQLQuery {

    private Map<String, Object> queryAttributes = new HashMap<String, Object>();

    private NoSQLQuery() {};

    public static NoSQLQuery create() {
        return new NoSQLQuery();
    }

    public NoSQLQuery put(String name, Object value) {
        queryAttributes.put(name, value);
        return this;
    }

    public Map<String, Object> getQueryAttributes() {
        return Collections.unmodifiableMap(queryAttributes);
    }

    @Override
    public String toString() {
        return "NoSQLQuery [" + queryAttributes + "]";
    }

}
