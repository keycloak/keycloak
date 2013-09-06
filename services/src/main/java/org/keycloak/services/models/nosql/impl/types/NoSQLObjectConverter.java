package org.keycloak.services.models.nosql.impl.types;

import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import org.keycloak.services.models.nosql.api.AttributedNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.types.Converter;
import org.keycloak.services.models.nosql.api.types.TypeConverter;
import org.keycloak.services.models.nosql.impl.MongoDBImpl;
import org.keycloak.services.models.nosql.impl.ObjectInfo;
import org.picketlink.common.properties.Property;
import org.picketlink.common.reflection.Types;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NoSQLObjectConverter<T extends NoSQLObject> implements Converter<T, BasicDBObject> {

    private final MongoDBImpl mongoDBImpl;
    private final TypeConverter typeConverter;
    private final Class<T> expectedNoSQLObjectType;

    public NoSQLObjectConverter(MongoDBImpl mongoDBImpl, TypeConverter typeConverter, Class<T> expectedNoSQLObjectType) {
        this.mongoDBImpl = mongoDBImpl;
        this.typeConverter = typeConverter;
        this.expectedNoSQLObjectType = expectedNoSQLObjectType;
    }

    @Override
    public T convertDBObjectToApplicationObject(BasicDBObject dbObject) {
        if (dbObject == null) {
            return null;
        }

        ObjectInfo objectInfo = mongoDBImpl.getObjectInfo(expectedNoSQLObjectType);

        T object;
        try {
            object = expectedNoSQLObjectType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (String key : dbObject.keySet()) {
            Object value = dbObject.get(key);
            Property<Object> property;

            if ("_id".equals(key)) {
                // Current property is "id"
                Property<String> idProperty = objectInfo.getOidProperty();
                if (idProperty != null) {
                    idProperty.setValue(object, value.toString());
                }

            } else if ((property = objectInfo.getPropertyByName(key)) != null) {
                // It's declared property with @DBField annotation
                setPropertyValue(object, value, property);

            } else if (object instanceof AttributedNoSQLObject) {
                // It's attributed object and property is not declared, so we will call setAttribute
                ((AttributedNoSQLObject)object).setAttribute(key, value.toString());

            } else {
                // Show warning if it's unknown
                // TODO: logging
                // logger.warn("Property with key " + key + " not known for type " + type);
                System.err.println("Property with key " + key + " not known for type " + expectedNoSQLObjectType);
            }
        }

        return object;
    }

    private void setPropertyValue(NoSQLObject object, Object valueFromDB, Property property) {
        Class<?> expectedType = property.getJavaClass();
        Class actualType = valueFromDB != null ? valueFromDB.getClass() : expectedType;

        // handle primitives
        expectedType = Types.boxedClass(expectedType);
        actualType = Types.boxedClass(actualType);

        if (actualType.isAssignableFrom(expectedType)) {
            property.setValue(object, valueFromDB);
        } else {
            // we need to convert
            Object convertedValue = typeConverter.convertDBObjectToApplicationObject(valueFromDB, expectedType);
            property.setValue(object, convertedValue);
        }
    }

    @Override
    public BasicDBObject convertApplicationObjectToDBObject(T applicationObject) {
        ObjectInfo objectInfo = mongoDBImpl.getObjectInfo(applicationObject.getClass());

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = new BasicDBObject();
        List<Property<Object>> props = objectInfo.getProperties();
        for (Property<Object> property : props) {
            String propName = property.getName();
            Object propValue = property.getValue(applicationObject);

            // Check if we have noSQLObject, which is indication that we need to convert recursively
            if (propValue instanceof NoSQLObject) {
                propValue = typeConverter.convertApplicationObjectToDBObject(propValue, BasicDBObject.class);
            }

            dbObject.append(propName, propValue);
        }

        // Adding attributes
        if (applicationObject instanceof AttributedNoSQLObject) {
            AttributedNoSQLObject attributedObject = (AttributedNoSQLObject)applicationObject;
            Map<String, String> attributes = attributedObject.getAttributes();
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                dbObject.append(attribute.getKey(), attribute.getValue());
            }
        }

        return dbObject;
    }

    @Override
    public Class<T> getApplicationObjectType() {
        return expectedNoSQLObjectType;
    }

    @Override
    public Class<BasicDBObject> getDBObjectType() {
        return BasicDBObject.class;
    }
}
