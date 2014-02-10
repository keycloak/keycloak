package org.keycloak.models.mongo.impl;

import org.keycloak.models.mongo.api.MongoEntity;
import org.picketlink.common.properties.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EntityInfo {

    private final Class<? extends MongoEntity> objectClass;

    private final String dbCollectionName;

    private final Map<String, Property<Object>> properties;

    public EntityInfo(Class<? extends MongoEntity> objectClass, String dbCollectionName, List<Property<Object>> properties) {
        this.objectClass = objectClass;
        this.dbCollectionName = dbCollectionName;

        Map<String, Property<Object>> props= new HashMap<String, Property<Object>>();
        for (Property<Object> property : properties) {
            props.put(property.getName(), property);
        }
        this.properties = Collections.unmodifiableMap(props);
    }

    public Class<? extends MongoEntity> getObjectClass() {
        return objectClass;
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
