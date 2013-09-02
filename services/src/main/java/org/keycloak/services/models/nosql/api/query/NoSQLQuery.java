package org.keycloak.services.models.nosql.api.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NoSQLQuery {

    private final Map<String, Object> queryAttributes;

    NoSQLQuery(Map<String, Object> queryAttributes) {
        this.queryAttributes = queryAttributes;
    };

    public Map<String, Object> getQueryAttributes() {
        return Collections.unmodifiableMap(queryAttributes);
    }

    @Override
    public String toString() {
        return "NoSQLQuery [" + queryAttributes + "]";
    }

}
