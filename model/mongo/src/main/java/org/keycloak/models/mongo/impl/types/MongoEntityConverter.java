package org.keycloak.models.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;
import org.keycloak.models.mongo.api.types.TypeConverter;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.impl.ObjectInfo;
import org.picketlink.common.properties.Property;

import java.util.Collection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoEntityConverter<T extends MongoEntity> implements Converter<T, BasicDBObject> {

    private final MongoStoreImpl mongoStoreImpl;
    private final TypeConverter typeConverter;
    private final Class<T> expectedNoSQLObjectType;

    public MongoEntityConverter(MongoStoreImpl mongoStoreImpl, TypeConverter typeConverter, Class<T> expectedNoSQLObjectType) {
        this.mongoStoreImpl = mongoStoreImpl;
        this.typeConverter = typeConverter;
        this.expectedNoSQLObjectType = expectedNoSQLObjectType;
    }

    @Override
    public BasicDBObject convertObject(ConverterContext<T> context) {
        T applicationObject = context.getObjectToConvert();

        ObjectInfo objectInfo = mongoStoreImpl.getObjectInfo(applicationObject.getClass());

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = new BasicDBObject();
        Collection<Property<Object>> props = objectInfo.getProperties();
        for (Property<Object> property : props) {
            String propName = property.getName();
            Object propValue = property.getValue(applicationObject);

            Object dbValue = propValue == null ? null : typeConverter.convertApplicationObjectToDBObject(propValue, Object.class);
            dbObject.put(propName, dbValue);
        }

        return dbObject;
    }

    @Override
    public Class<? extends T> getConverterObjectType() {
        return expectedNoSQLObjectType;
    }

    @Override
    public Class<BasicDBObject> getExpectedReturnType() {
        return BasicDBObject.class;
    }
}
