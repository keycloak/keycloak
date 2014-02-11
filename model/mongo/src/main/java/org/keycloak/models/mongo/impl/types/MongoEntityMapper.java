package org.keycloak.models.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.types.Mapper;
import org.keycloak.models.mongo.api.types.MapperContext;
import org.keycloak.models.mongo.api.types.MapperRegistry;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.impl.EntityInfo;
import org.picketlink.common.properties.Property;

import java.util.Collection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoEntityMapper<T extends MongoEntity> implements Mapper<T, BasicDBObject> {

    private final MongoStoreImpl mongoStoreImpl;
    private final MapperRegistry mapperRegistry;
    private final Class<T> expectedMongoEntityType;

    public MongoEntityMapper(MongoStoreImpl mongoStoreImpl, MapperRegistry mapperRegistry, Class<T> expectedMongoEntityType) {
        this.mongoStoreImpl = mongoStoreImpl;
        this.mapperRegistry = mapperRegistry;
        this.expectedMongoEntityType = expectedMongoEntityType;
    }

    @Override
    public BasicDBObject convertObject(MapperContext<T, BasicDBObject> context) {
        T applicationObject = context.getObjectToConvert();

        EntityInfo entityInfo = mongoStoreImpl.getEntityInfo(applicationObject.getClass());

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = new BasicDBObject();
        Collection<Property<Object>> props = entityInfo.getProperties();
        for (Property<Object> property : props) {
            String propName = property.getName();
            Object propValue = property.getValue(applicationObject);

            Object dbValue = propValue == null ? null : mapperRegistry.convertApplicationObjectToDBObject(propValue, Object.class);
            dbObject.put(propName, dbValue);
        }

        return dbObject;
    }

    @Override
    public Class<? extends T> getTypeOfObjectToConvert() {
        return expectedMongoEntityType;
    }

    @Override
    public Class<BasicDBObject> getExpectedReturnType() {
        return BasicDBObject.class;
    }
}
