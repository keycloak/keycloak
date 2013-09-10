package org.keycloak.services.models.nosql.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.picketlink.common.properties.Property;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ObjectInfo {

    private final Class<? extends NoSQLObject> objectClass;

    private final String dbCollectionName;

    private final Property<String> oidProperty;

    private final Map<String, Property<Object>> properties;

    public ObjectInfo(Class<? extends NoSQLObject> objectClass, String dbCollectionName, Property<String> oidProperty, List<Property<Object>> properties) {
        this.objectClass = objectClass;
        this.dbCollectionName = dbCollectionName;
        this.oidProperty = oidProperty;

        Map<String, Property<Object>> props= new HashMap<String, Property<Object>>();
        for (Property<Object> property : properties) {
            props.put(property.getName(), property);
        }
        this.properties = Collections.unmodifiableMap(props);
    }

    public Class<? extends NoSQLObject> getObjectClass() {
        return objectClass;
    }

    public String getDbCollectionName() {
        return dbCollectionName;
    }

    public Property<String> getOidProperty() {
        return oidProperty;
    }

    public Collection<Property<Object>> getProperties() {
        return properties.values();
    }

    public Property<Object> getPropertyByName(String propertyName) {
        return properties.get(propertyName);
    }
}
