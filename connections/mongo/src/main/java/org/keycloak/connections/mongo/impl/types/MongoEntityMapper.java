package org.keycloak.connections.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;
import org.keycloak.connections.mongo.impl.EntityInfo;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.models.utils.reflection.Property;

import java.util.Collection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoEntityMapper<T> implements Mapper<T, BasicDBObject> {

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

        // Create instance of BasicDBObject and add all declared properties to it
        BasicDBObject dbObject = new BasicDBObject();
        Collection<Property<Object>> props = entityInfo.getProperties();
        for (Property<Object> property : props) {
            String propName = property.getName();

            // Ignore "id" property
            if (!"id".equals(propName) || !(applicationObject instanceof MongoIdentifiableEntity)) {
                Object propValue = property.getValue(applicationObject);
                if (propValue != null) {
                    Object dbValue = propValue == null ? null : mapperRegistry.convertApplicationObjectToDBObject(propValue, Object.class);
                    dbObject.put(propName, dbValue);
                }
            }
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
