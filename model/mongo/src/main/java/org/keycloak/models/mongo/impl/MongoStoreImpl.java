package org.keycloak.models.mongo.impl;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoId;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;
import org.keycloak.models.mongo.api.types.TypeConverter;
import org.keycloak.models.mongo.impl.types.BasicDBListConverter;
import org.keycloak.models.mongo.impl.types.BasicDBObjectConverter;
import org.keycloak.models.mongo.impl.types.BasicDBObjectToMapConverter;
import org.keycloak.models.mongo.impl.types.EnumToStringConverter;
import org.keycloak.models.mongo.impl.types.ListConverter;
import org.keycloak.models.mongo.impl.types.MapConverter;
import org.keycloak.models.mongo.impl.types.MongoEntityConverter;
import org.keycloak.models.mongo.impl.types.SimpleConverter;
import org.keycloak.models.mongo.impl.types.StringToEnumConverter;
import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoStoreImpl implements MongoStore {

    private static final Class<?>[] SIMPLE_TYPES = { String.class, Integer.class, Boolean.class, Long.class, Double.class, Character.class, Date.class, byte[].class };

    private final DB database;
    private static final Logger logger = Logger.getLogger(MongoStoreImpl.class);

    private final TypeConverter typeConverter;
    private ConcurrentMap<Class<? extends MongoEntity>, ObjectInfo> objectInfoCache =
            new ConcurrentHashMap<Class<? extends MongoEntity>, ObjectInfo>();


    public MongoStoreImpl(DB database, boolean clearCollectionsOnStartup, Class<? extends MongoEntity>[] managedEntityTypes) {
        this.database = database;

        typeConverter = new TypeConverter();

        for (Class<?> simpleConverterClass : SIMPLE_TYPES) {
            SimpleConverter converter = new SimpleConverter(simpleConverterClass);
            typeConverter.addAppObjectConverter(converter);
            typeConverter.addDBObjectConverter(converter);
        }

        // Specific converter for ArrayList is added just for performance purposes to avoid recursive converter lookup (most of list impl will be ArrayList)
        typeConverter.addAppObjectConverter(new ListConverter(typeConverter, ArrayList.class));
        typeConverter.addAppObjectConverter(new ListConverter(typeConverter, List.class));
        typeConverter.addDBObjectConverter(new BasicDBListConverter(typeConverter));

        typeConverter.addAppObjectConverter(new MapConverter(HashMap.class));
        typeConverter.addAppObjectConverter(new MapConverter(Map.class));
        typeConverter.addDBObjectConverter(new BasicDBObjectToMapConverter());

        // Enum converters
        typeConverter.addAppObjectConverter(new EnumToStringConverter());
        typeConverter.addDBObjectConverter(new StringToEnumConverter());

        for (Class<? extends MongoEntity> type : managedEntityTypes) {
            getObjectInfo(type);
            typeConverter.addAppObjectConverter(new MongoEntityConverter(this, typeConverter, type));
            typeConverter.addDBObjectConverter(new BasicDBObjectConverter(this, typeConverter, type));
        }

        if (clearCollectionsOnStartup) {
            // dropDatabase();
            clearManagedCollections(managedEntityTypes);
        }
    }

    protected void dropDatabase() {
        this.database.dropDatabase();
        logger.info("Database " + this.database.getName() + " dropped in MongoDB");
    }

    // Don't drop database, but just clear all data in managed collections (useful for development)
    protected void clearManagedCollections(Class<? extends MongoEntity>[] managedEntityTypes) {
        for (Class<? extends MongoEntity> clazz : managedEntityTypes) {
            DBCollection dbCollection = getDBCollectionForType(clazz);
            if (dbCollection != null) {
                dbCollection.remove(new BasicDBObject());
                logger.debug("Collection " + dbCollection.getName() + " cleared from " + this.database.getName());
            }
        }
    }

    @Override
    public void insertObject(MongoEntity object) {
        Class<? extends MongoEntity> clazz = object.getClass();

        // Find annotations for ID, for all the properties and for the name of the collection.
        ObjectInfo objectInfo = getObjectInfo(clazz);

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = typeConverter.convertApplicationObjectToDBObject(object, BasicDBObject.class);

        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        Property<String> oidProperty = objectInfo.getOidProperty();
        String currentId = oidProperty == null ? null : oidProperty.getValue(object);

        // Inserting object, which already has oid property set. So we need to set "_id"
        if (currentId != null) {
            dbObject.put("_id", getObjectId(currentId));
        }

        dbCollection.insert(dbObject);

        // Add oid to value of given object
        if (currentId == null && oidProperty != null) {
            oidProperty.setValue(object, dbObject.getString("_id"));
        }
    }

    @Override
    public void updateObject(MongoEntity object) {
        Class<? extends MongoEntity> clazz = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(clazz);
        BasicDBObject dbObject = typeConverter.convertApplicationObjectToDBObject(object, BasicDBObject.class);
        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        Property<String> oidProperty = objectInfo.getOidProperty();
        String currentId = oidProperty == null ? null : oidProperty.getValue(object);

        if (currentId == null) {
            throw new IllegalStateException("Can't update object without id: " + object);
        } else {
            BasicDBObject query = new BasicDBObject("_id", getObjectId(currentId));
            dbCollection.update(query, dbObject);
        }
    }


    @Override
    public <T extends MongoEntity> T loadObject(Class<T> type, String oid) {
        DBCollection dbCollection = getDBCollectionForType(type);

        BasicDBObject idQuery = new BasicDBObject("_id", getObjectId(oid));
        DBObject dbObject = dbCollection.findOne(idQuery);

        if (dbObject == null) return null;

        ConverterContext<Object> converterContext = new ConverterContext<Object>(dbObject, type, null);
        return (T)typeConverter.convertDBObjectToApplicationObject(converterContext);
    }


    @Override
    public <T extends MongoEntity> T loadSingleObject(Class<T> type, DBObject query) {
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
    public <T extends MongoEntity> List<T> loadObjects(Class<T> type, DBObject query) {
        DBCollection dbCollection = getDBCollectionForType(type);

        DBCursor cursor = dbCollection.find(query);

        return convertCursor(type, cursor);
    }


    @Override
    public boolean removeObject(MongoEntity object) {
        Class<? extends MongoEntity> type = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(type);

        Property<String> idProperty = objectInfo.getOidProperty();
        String oid = idProperty.getValue(object);

        return removeObject(type, oid);
    }


    @Override
    public boolean removeObject(Class<? extends MongoEntity> type, String oid) {
        MongoEntity found = loadObject(type, oid);
        if (found == null) {
            return false;
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            BasicDBObject dbQuery = new BasicDBObject("_id", getObjectId(oid));
            dbCollection.remove(dbQuery);
            logger.info("Object of type: " + type + ", oid: " + oid + " removed from MongoDB.");

            found.afterRemove(this);
            return true;
        }
    }


    @Override
    public boolean removeObjects(Class<? extends MongoEntity> type, DBObject query) {
        List<? extends MongoEntity> foundObjects = loadObjects(type, query);
        if (foundObjects.size() == 0) {
            return false;
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            dbCollection.remove(query);
            logger.info("Removed " + foundObjects.size() + " objects of type: " + type + ", query: " + query);

            for (MongoEntity found : foundObjects) {
                found.afterRemove(this);
            }
            return true;
        }
    }

    @Override
    public <S> boolean pushItemToList(MongoEntity object, String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent) {
        Class<? extends MongoEntity> type = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(type);

        Property<String> oidProperty = getObjectInfo(type).getOidProperty();
        if (oidProperty == null) {
            throw new IllegalArgumentException("List pushes not supported for properties without oid");
        }

        // Add item to list directly in this object
        Property<Object> listProperty = objectInfo.getPropertyByName(listPropertyName);
        if (listProperty == null) {
            throw new IllegalArgumentException("Property " + listPropertyName + " doesn't exist on object " + object);
        }

        List<S> list = (List<S>)listProperty.getValue(object);
        if (list == null) {
            list = new ArrayList<S>();
            listProperty.setValue(object, list);
        }

        // Return if item is already in list
        if (skipIfAlreadyPresent && list.contains(itemToPush)) {
            return false;
        }

        list.add(itemToPush);

        // Push item to DB. We always convert whole list, so it's not so optimal...TODO: use $push if possible
        BasicDBList dbList = typeConverter.convertApplicationObjectToDBObject(list, BasicDBList.class);

        BasicDBObject query = new BasicDBObject("_id", getObjectId(oidProperty.getValue(object)));
        BasicDBObject listObject = new BasicDBObject(listPropertyName, dbList);
        BasicDBObject setCommand = new BasicDBObject("$set", listObject);
        getDBCollectionForType(type).update(query, setCommand);
        return true;
    }


    @Override
    public <S> void pullItemFromList(MongoEntity object, String listPropertyName, S itemToPull) {
        Class<? extends MongoEntity> type = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(type);

        Property<String> oidProperty = getObjectInfo(type).getOidProperty();
        if (oidProperty == null) {
            throw new IllegalArgumentException("List pulls not supported for properties without oid");
        }

        // Remove item from list directly in this object
        Property<Object> listProperty = objectInfo.getPropertyByName(listPropertyName);
        if (listProperty == null) {
            throw new IllegalArgumentException("Property " + listPropertyName + " doesn't exist on object " + object);
        }
        List<S> list = (List<S>)listProperty.getValue(object);

        // If list is null, we skip both object and DB update
        if (list != null) {
            list.remove(itemToPull);

            // Pull item from DB
            Object dbItemToPull = typeConverter.convertApplicationObjectToDBObject(itemToPull, Object.class);
            BasicDBObject query = new BasicDBObject("_id", getObjectId(oidProperty.getValue(object)));
            BasicDBObject pullObject = new BasicDBObject(listPropertyName, dbItemToPull);
            BasicDBObject pullCommand = new BasicDBObject("$pull", pullObject);
            getDBCollectionForType(type).update(query, pullCommand);
        }
    }

    // Possibility to add user-defined converters
    public void addAppObjectConverter(Converter<?, ?> converter) {
        typeConverter.addAppObjectConverter(converter);
    }

    public void addDBObjectConverter(Converter<?, ?> converter) {
        typeConverter.addDBObjectConverter(converter);
    }

    public ObjectInfo getObjectInfo(Class<? extends MongoEntity> objectClass) {
        ObjectInfo objectInfo = objectInfoCache.get(objectClass);
        if (objectInfo == null) {
            Property<String> idProperty = PropertyQueries.<String>createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(MongoId.class)).getFirstResult();

            List<Property<Object>> properties = PropertyQueries.createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(MongoField.class)).getResultList();

            MongoCollection classAnnotation = objectClass.getAnnotation(MongoCollection.class);

            String dbCollectionName = classAnnotation==null ? null : classAnnotation.collectionName();
            objectInfo = new ObjectInfo(objectClass, dbCollectionName, idProperty, properties);

            ObjectInfo existing = objectInfoCache.putIfAbsent(objectClass, objectInfo);
            if (existing != null) {
                objectInfo = existing;
            }
        }

        return objectInfo;
    }

    private <T extends MongoEntity> List<T> convertCursor(Class<T> type, DBCursor cursor) {
        List<T> result = new ArrayList<T>();

        try {
            for (DBObject dbObject : cursor) {
                ConverterContext<Object> converterContext = new ConverterContext<Object>(dbObject, type, null);
                T converted = (T)typeConverter.convertDBObjectToApplicationObject(converterContext);
                result.add(converted);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    private DBCollection getDBCollectionForType(Class<? extends MongoEntity> type) {
        ObjectInfo objectInfo = getObjectInfo(type);
        String dbCollectionName = objectInfo.getDbCollectionName();
        return dbCollectionName==null ? null : database.getCollection(objectInfo.getDbCollectionName());
    }

    // We allow ObjectId to be both "ObjectId" or "String".
    private Object getObjectId(String idAsString) {
        if (ObjectId.isValid(idAsString)) {
            return new ObjectId(idAsString);
        } else {
            return idAsString;
        }
    }
}
