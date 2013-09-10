package org.keycloak.services.models.nosql.impl.types;

import com.mongodb.BasicDBObject;
import org.jboss.resteasy.logging.Logger;
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
public class BasicDBObjectConverter<S extends NoSQLObject> implements Converter<BasicDBObject, S> {

    private static final Logger logger = Logger.getLogger(BasicDBObjectConverter.class);

    private final MongoDBImpl mongoDBImpl;
    private final TypeConverter typeConverter;
    private final Class<S> expectedNoSQLObjectType;

    public BasicDBObjectConverter(MongoDBImpl mongoDBImpl, TypeConverter typeConverter, Class<S> expectedNoSQLObjectType) {
        this.mongoDBImpl = mongoDBImpl;
        this.typeConverter = typeConverter;
        this.expectedNoSQLObjectType = expectedNoSQLObjectType;
    }

    @Override
    public S convertObject(BasicDBObject dbObject) {
        if (dbObject == null) {
            return null;
        }

        ObjectInfo objectInfo = mongoDBImpl.getObjectInfo(expectedNoSQLObjectType);

        S object;
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
                logger.warn("Property with key " + key + " not known for type " + expectedNoSQLObjectType);
            }
        }

        return object;
    }

    private void setPropertyValue(NoSQLObject object, Object valueFromDB, Property property) {
        if (valueFromDB == null) {
            property.setValue(object, null);
            return;
        }

        Class<?> expectedReturnType = property.getJavaClass();
        // handle primitives
        expectedReturnType = Types.boxedClass(expectedReturnType);

        Object appObject = typeConverter.convertDBObjectToApplicationObject(valueFromDB, expectedReturnType);
        if (Types.boxedClass(property.getJavaClass()).isAssignableFrom(appObject.getClass())) {
            property.setValue(object, appObject);
        } else {
            throw new IllegalStateException("Converted object " + appObject + " is not of type " +  expectedReturnType +
                    ". So can't be assigned as property " + property.getName() + " of " + object.getClass());
        }
    }

    @Override
    public Class<? extends BasicDBObject> getConverterObjectType() {
        return BasicDBObject.class;
    }

    @Override
    public Class<S> getExpectedReturnType() {
        return expectedNoSQLObjectType;
    }
}
