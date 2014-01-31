package org.keycloak.models.mongo.impl.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import org.jboss.logging.Logger;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;
import org.keycloak.models.mongo.api.types.TypeConverter;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.impl.ObjectInfo;
import org.picketlink.common.properties.Property;
import org.picketlink.common.reflection.Types;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBObjectConverter<S extends MongoEntity> implements Converter<BasicDBObject, S> {

    private static final Logger logger = Logger.getLogger(BasicDBObjectConverter.class);

    private final MongoStoreImpl mongoStoreImpl;
    private final TypeConverter typeConverter;
    private final Class<S> expectedObjectType;

    public BasicDBObjectConverter(MongoStoreImpl mongoStoreImpl, TypeConverter typeConverter, Class<S> expectedObjectType) {
        this.mongoStoreImpl = mongoStoreImpl;
        this.typeConverter = typeConverter;
        this.expectedObjectType = expectedObjectType;
    }

    @Override
    public S convertObject(ConverterContext<BasicDBObject> context) {
        BasicDBObject dbObject = context.getObjectToConvert();
        if (dbObject == null) {
            return null;
        }

        ObjectInfo objectInfo = mongoStoreImpl.getObjectInfo(expectedObjectType);

        S object;
        try {
            object = expectedObjectType.newInstance();
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

            } else {
                // Show warning if it's unknown
                logger.warn("Property with key " + key + " not known for type " + expectedObjectType);
            }
        }

        return object;
    }

    private void setPropertyValue(MongoEntity object, Object valueFromDB, Property property) {
        if (valueFromDB == null) {
            property.setValue(object, null);
            return;
        }

        ConverterContext<Object> context;

        Type type = property.getBaseType();

        // This can be the case when we have parameterized type (like "List<String>")
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Type[] genericTypeArguments = parameterized.getActualTypeArguments();

            List<Class<?>> genericTypes = new ArrayList<Class<?>>();
            for (Type genericType : genericTypeArguments) {
                genericTypes.add((Class<?>)genericType);
            }

            Class<?> expectedReturnType = (Class<?>)parameterized.getRawType();
            context = new ConverterContext<Object>(valueFromDB, expectedReturnType, genericTypes);
        } else {
            Class<?> expectedReturnType = (Class<?>)type;
            // handle primitives
            expectedReturnType = Types.boxedClass(expectedReturnType);
            context = new ConverterContext<Object>(valueFromDB, expectedReturnType, null);
        }

        Object appObject = typeConverter.convertDBObjectToApplicationObject(context);

        if (Types.boxedClass(property.getJavaClass()).isAssignableFrom(appObject.getClass())) {
            property.setValue(object, appObject);
        } else {
            throw new IllegalStateException("Converted object " + appObject + " is not of type " +  context.getExpectedReturnType() +
                    ". So can't be assigned as property " + property.getName() + " of " + object.getClass());
        }
    }

    @Override
    public Class<? extends BasicDBObject> getConverterObjectType() {
        return BasicDBObject.class;
    }

    @Override
    public Class<S> getExpectedReturnType() {
        return expectedObjectType;
    }
}
