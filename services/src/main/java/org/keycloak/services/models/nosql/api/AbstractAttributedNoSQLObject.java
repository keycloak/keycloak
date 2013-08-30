package org.keycloak.services.models.nosql.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractAttributedNoSQLObject implements AttributedNoSQLObject {

    // Simple hashMap for now (no thread-safe)
    private Map<String, String> attributes = new HashMap<String, String>();

    @Override
    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        // attributes.remove(name);

        // ensure that particular attribute has null value, so it will be deleted in DB. TODO: needs to be improved
        attributes.put(name, null);
    }

    @Override
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
