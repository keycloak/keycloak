package org.keycloak.connections.mongo.impl;

import org.keycloak.models.utils.reflection.Property;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EntityInfo {

    private final Class<?> entityClass;

    private final String dbCollectionName;

    private final Map<String, Property<Object>> properties;

    public EntityInfo(Class<?> entityClass, String dbCollectionName, Map<String, Property<Object>> properties) {
        this.entityClass = entityClass;
        this.dbCollectionName = dbCollectionName;
        this.properties = properties;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getDbCollectionName() {
        return dbCollectionName;
    }

    public Collection<Property<Object>> getProperties() {
        return properties.values();
    }

    public Property<Object> getPropertyByName(String propertyName) {
        return properties.get(propertyName);
    }
}
