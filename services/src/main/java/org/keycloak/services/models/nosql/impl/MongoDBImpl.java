package org.keycloak.services.models.nosql.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.api.query.NoSQLQueryBuilder;
import org.keycloak.services.models.nosql.api.types.Converter;
import org.keycloak.services.models.nosql.api.types.TypeConverter;
import org.keycloak.services.models.nosql.impl.types.BasicDBListToStringArrayConverter;
import org.keycloak.services.models.nosql.impl.types.NoSQLObjectConverter;
import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBImpl implements NoSQL {

    private final DB database;
    private static final Logger logger = Logger.getLogger(MongoDBImpl.class);

    private final TypeConverter typeConverter;
    private ConcurrentMap<Class<? extends NoSQLObject>, ObjectInfo> objectInfoCache =
            new ConcurrentHashMap<Class<? extends NoSQLObject>, ObjectInfo>();

    public MongoDBImpl(DB database, boolean removeAllObjectsAtStartup, Class<? extends NoSQLObject>[] managedDataTypes) {
        this.database = database;

        typeConverter = new TypeConverter();
        typeConverter.addConverter(new BasicDBListToStringArrayConverter());
        for (Class<? extends NoSQLObject> type : managedDataTypes) {
            typeConverter.addConverter(new NoSQLObjectConverter(this, typeConverter, type));
            getObjectInfo(type);
        }

        if (removeAllObjectsAtStartup) {
            for (Class<? extends NoSQLObject> type : managedDataTypes) {
                ObjectInfo objectInfo = getObjectInfo(type);
                String collectionName = objectInfo.getDbCollectionName();
                if (collectionName != null) {
                    logger.debug("Removing all objects of type " + type);

                    DBCollection dbCollection = this.database.getCollection(collectionName);
                    dbCollection.remove(new BasicDBObject());
                }  else {
                    logger.debug("Skip removing objects of type " + type + " as it doesn't have it's own collection");
                }
            }
            logger.info("All objects successfully removed from MongoDB");
        }
    }

    @Override
    public void saveObject(NoSQLObject object) {
        Class<? extends NoSQLObject> clazz = object.getClass();

        // Find annotations for ID, for all the properties and for the name of the collection.
        ObjectInfo objectInfo = getObjectInfo(clazz);

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = typeConverter.convertApplicationObjectToDBObject(object, BasicDBObject.class);

        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        // Decide if we should insert or update (based on presence of oid property in original object)
        Property<String> oidProperty = objectInfo.getOidProperty();
        String currentId = oidProperty == null ? null : oidProperty.getValue(object);
        if (currentId == null) {
            dbCollection.insert(dbObject);

            // Add oid to value of given object
            if (oidProperty != null) {
                oidProperty.setValue(object, dbObject.getString("_id"));
            }
        } else {
            BasicDBObject setCommand = new BasicDBObject("$set", dbObject);
            BasicDBObject query = new BasicDBObject("_id", new ObjectId(currentId));
            dbCollection.update(query, setCommand);
        }
    }

    @Override
    public <T extends NoSQLObject> T loadObject(Class<T> type, String oid) {
        DBCollection dbCollection = getDBCollectionForType(type);

        BasicDBObject idQuery = new BasicDBObject("_id", new ObjectId(oid));
        DBObject dbObject = dbCollection.findOne(idQuery);

        return typeConverter.convertDBObjectToApplicationObject(dbObject, type);
    }

    @Override
    public <T extends NoSQLObject> T loadSingleObject(Class<T> type, NoSQLQuery query) {
        List<T> result = loadObjects(type, query);
        if (result.size() > 1) {
            throw new IllegalStateException("There are " + result.size() + " results for type=" + type + ", query=" + query + ". We expect just one");
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            // 0 results
            return null;
        }
    }

    @Override
    public <T extends NoSQLObject> List<T> loadObjects(Class<T> type, NoSQLQuery query) {
        DBCollection dbCollection = getDBCollectionForType(type);
        BasicDBObject dbQuery = getDBQueryFromQuery(query);

        DBCursor cursor = dbCollection.find(dbQuery);

        return convertCursor(type, cursor);
    }

    @Override
    public void removeObject(NoSQLObject object) {
        Class<? extends NoSQLObject> type = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(type);

        Property<String> idProperty = objectInfo.getOidProperty();
        String oid = idProperty.getValue(object);

        removeObject(type, oid);
    }

    @Override
    public void removeObject(Class<? extends NoSQLObject> type, String oid) {
        NoSQLObject found = loadObject(type, oid);
        if (found == null) {
            logger.warn("Object of type: " + type + ", oid: " + oid + " doesn't exist in MongoDB. Skip removal");
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            BasicDBObject dbQuery = new BasicDBObject("_id", new ObjectId(oid));
            dbCollection.remove(dbQuery);
            logger.info("Object of type: " + type + ", oid: " + oid + " removed from MongoDB.");

            found.afterRemove(this);
        }
    }

    @Override
    public void removeObjects(Class<? extends NoSQLObject> type, NoSQLQuery query) {
        List<? extends NoSQLObject> foundObjects = loadObjects(type, query);
        if (foundObjects.size() == 0) {
            logger.info("Not found any objects of type: " + type + ", query: " + query);
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            BasicDBObject dbQuery = getDBQueryFromQuery(query);
            dbCollection.remove(dbQuery);
            logger.info("Removed " + foundObjects.size() + " objects of type: " + type + ", query: " + query);

            for (NoSQLObject found : foundObjects) {
                found.afterRemove(this);
            }
        }
    }

    @Override
    public NoSQLQueryBuilder createQueryBuilder() {
        return new MongoDBQueryBuilder();
    }

    // Possibility to add user-defined converters
    public void addConverter(Converter<?, ?> converter) {
        typeConverter.addConverter(converter);
    }

    public ObjectInfo getObjectInfo(Class<? extends NoSQLObject> objectClass) {
        ObjectInfo objectInfo = objectInfoCache.get(objectClass);
        if (objectInfo == null) {
            Property<String> idProperty = PropertyQueries.<String>createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(NoSQLId.class)).getFirstResult();

            List<Property<Object>> properties = PropertyQueries.createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(NoSQLField.class)).getResultList();

            NoSQLCollection classAnnotation = objectClass.getAnnotation(NoSQLCollection.class);

            String dbCollectionName = classAnnotation==null ? null : classAnnotation.collectionName();
            objectInfo = new ObjectInfo(objectClass, dbCollectionName, idProperty, properties);

            ObjectInfo existing = objectInfoCache.putIfAbsent(objectClass, objectInfo);
            if (existing != null) {
                objectInfo = existing;
            }
        }

        return objectInfo;
    }

    private <T extends NoSQLObject> List<T> convertCursor(Class<T> type, DBCursor cursor) {
        List<T> result = new ArrayList<T>();

        try {
            for (DBObject dbObject : cursor) {
                T converted = typeConverter.convertDBObjectToApplicationObject(dbObject, type);
                result.add(converted);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    private DBCollection getDBCollectionForType(Class<? extends NoSQLObject> type) {
        ObjectInfo objectInfo = getObjectInfo(type);
        return database.getCollection(objectInfo.getDbCollectionName());
    }

    private BasicDBObject getDBQueryFromQuery(NoSQLQuery query) {
        Map<String, Object> queryAttributes = query.getQueryAttributes();
        BasicDBObject dbQuery = new BasicDBObject();
        for (Map.Entry<String, Object> queryAttr : queryAttributes.entrySet()) {
            dbQuery.append(queryAttr.getKey(), queryAttr.getValue());
        }
        return dbQuery;
    }
}
