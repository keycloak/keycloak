package org.keycloak.services.models.nosql.impl;

import java.util.List;

import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.picketlink.common.properties.Property;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ObjectInfo {

    private final Class<? extends NoSQLObject> objectClass;

    private final String dbCollectionName;

    private final Property<String> oidProperty;

    private final List<Property<Object>> properties;

    public ObjectInfo(Class<? extends NoSQLObject> objectClass, String dbCollectionName, Property<String> oidProperty, List<Property<Object>> properties) {
        this.objectClass = objectClass;
        this.dbCollectionName = dbCollectionName;
        this.oidProperty = oidProperty;
        this.properties = properties;
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

    public List<Property<Object>> getProperties() {
        return properties;
    }

    public Property<Object> getPropertyByName(String propertyName) {
        for (Property<Object> property : properties) {
            if (propertyName.equals(property.getName())) {
                return property;
            }
        }

        return null;
    }
}
