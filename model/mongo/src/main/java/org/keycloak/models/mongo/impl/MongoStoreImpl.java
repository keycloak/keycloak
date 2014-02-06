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
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.api.context.MongoTask;
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
    public void insertObject(MongoIdentifiableEntity object, MongoStoreInvocationContext context) {
        Class<? extends MongoEntity> clazz = object.getClass();

        // Find annotations for ID, for all the properties and for the name of the collection.
        ObjectInfo objectInfo = getObjectInfo(clazz);

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = typeConverter.convertApplicationObjectToDBObject(object, BasicDBObject.class);

        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        String currentId = object.getId();

        // Inserting object, which already has oid property set. So we need to set "_id"
        if (currentId != null) {
            dbObject.put("_id", getObjectId(currentId));
        }

        dbCollection.insert(dbObject);

        // Add id to value of given object
        if (currentId == null) {
            object.setId(dbObject.getString("_id"));
        }

        // Treat object as if it is read (It is already submited to transaction)
        context.addLoadedObject(object);
    }

    @Override
    public void updateObject(final MongoIdentifiableEntity object, MongoStoreInvocationContext context) {
        MongoTask fullUpdateTask = new MongoTask() {

            @Override
            public void execute() {
                Class<? extends MongoEntity> clazz = object.getClass();
                ObjectInfo objectInfo = getObjectInfo(clazz);
                BasicDBObject dbObject = typeConverter.convertApplicationObjectToDBObject(object, BasicDBObject.class);
                DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

                String currentId = object.getId();

                if (currentId == null) {
                    throw new IllegalStateException("Can't update object without id: " + object);
                } else {
                    BasicDBObject query = new BasicDBObject("_id", getObjectId(currentId));
                    dbCollection.update(query, dbObject);
                }
            }

            @Override
            public boolean isFullUpdate() {
                return true;
            }
        };

        // update is just added to context and postponed
        context.addUpdateTask(object, fullUpdateTask);
    }


    @Override
    public <T extends MongoIdentifiableEntity> T loadObject(Class<T> type, String id, MongoStoreInvocationContext context) {
        // First look if we already read the object with this oid and type during this transaction. If yes, use it instead of DB lookup
        T cached = context.getLoadedObject(type, id);
        if (cached != null) return cached;

        DBCollection dbCollection = getDBCollectionForType(type);

        BasicDBObject idQuery = new BasicDBObject("_id", getObjectId(id));
        DBObject dbObject = dbCollection.findOne(idQuery);

        if (dbObject == null) return null;

        ConverterContext<Object> converterContext = new ConverterContext<Object>(dbObject, type, null);
        T converted = (T)typeConverter.convertDBObjectToApplicationObject(converterContext);

        // Now add it to loaded objects
        context.addLoadedObject(converted);

        return converted;
    }


    @Override
    public <T extends MongoIdentifiableEntity> T loadSingleObject(Class<T> type, DBObject query, MongoStoreInvocationContext context) {
        List<T> result = loadObjects(type, query, context);
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
    public <T extends MongoIdentifiableEntity> List<T> loadObjects(Class<T> type, DBObject query, MongoStoreInvocationContext context) {
        // First we should execute all pending tasks before searching DB
        context.beforeDBSearch(type);

        DBCollection dbCollection = getDBCollectionForType(type);
        DBCursor cursor = dbCollection.find(query);

        return convertCursor(type, cursor, context);
    }


    @Override
    public boolean removeObject(MongoIdentifiableEntity object, MongoStoreInvocationContext context) {
        return removeObject(object.getClass(), object.getId(), context);
    }


    @Override
    public boolean removeObject(Class<? extends MongoIdentifiableEntity> type, String id, MongoStoreInvocationContext context) {
        MongoIdentifiableEntity found = loadObject(type, id, context);
        if (found == null) {
            return false;
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            BasicDBObject dbQuery = new BasicDBObject("_id", getObjectId(id));
            dbCollection.remove(dbQuery);
            logger.info("Object of type: " + type + ", id: " + id + " removed from MongoDB.");

            context.addRemovedObject(found);
            return true;
        }
    }


    @Override
    public boolean removeObjects(Class<? extends MongoIdentifiableEntity> type, DBObject query, MongoStoreInvocationContext context) {
        List<? extends MongoIdentifiableEntity> foundObjects = loadObjects(type, query, context);
        if (foundObjects.size() == 0) {
            return false;
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            dbCollection.remove(query);
            logger.info("Removed " + foundObjects.size() + " objects of type: " + type + ", query: " + query);

            for (MongoIdentifiableEntity found : foundObjects) {
                context.addRemovedObject(found);;
            }
            return true;
        }
    }

    @Override
    public <S> boolean pushItemToList(final MongoIdentifiableEntity object, final String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent, MongoStoreInvocationContext context) {
        final Class<? extends MongoEntity> type = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(type);

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

        // Skip if item is already in list
        if (skipIfAlreadyPresent && list.contains(itemToPush)) {
            return false;
        }

        // Update java object
        list.add(itemToPush);

        // Add update of list to pending tasks
        final List<S> listt = list;
        context.addUpdateTask(object, new MongoTask() {

            @Override
            public void execute() {
                // Now DB update of new list with usage of $set
                BasicDBList dbList = typeConverter.convertApplicationObjectToDBObject(listt, BasicDBList.class);

                BasicDBObject query = new BasicDBObject("_id", getObjectId(object.getId()));
                BasicDBObject listObject = new BasicDBObject(listPropertyName, dbList);
                BasicDBObject setCommand = new BasicDBObject("$set", listObject);
                getDBCollectionForType(type).update(query, setCommand);
            }

            @Override
            public boolean isFullUpdate() {
                return false;
            }
        });

        return true;
    }


    @Override
    public <S> boolean pullItemFromList(final MongoIdentifiableEntity object, final String listPropertyName, final S itemToPull, MongoStoreInvocationContext context) {
        final Class<? extends MongoEntity> type = object.getClass();
        ObjectInfo objectInfo = getObjectInfo(type);

        // Remove item from list directly in this object
        Property<Object> listProperty = objectInfo.getPropertyByName(listPropertyName);
        if (listProperty == null) {
            throw new IllegalArgumentException("Property " + listPropertyName + " doesn't exist on object " + object);
        }
        List<S> list = (List<S>)listProperty.getValue(object);

        // If list is null, we skip both object and DB update
        if (list == null || !list.contains(itemToPull)) {
            return false;
        } else {

            // Update java object
            list.remove(itemToPull);

            // Add update of list to pending tasks
            context.addUpdateTask(object, new MongoTask() {

                @Override
                public void execute() {
                    // Pull item from DB
                    Object dbItemToPull = typeConverter.convertApplicationObjectToDBObject(itemToPull, Object.class);
                    BasicDBObject query = new BasicDBObject("_id", getObjectId(object.getId()));
                    BasicDBObject pullObject = new BasicDBObject(listPropertyName, dbItemToPull);
                    BasicDBObject pullCommand = new BasicDBObject("$pull", pullObject);
                    getDBCollectionForType(type).update(query, pullCommand);
                }

                @Override
                public boolean isFullUpdate() {
                    return false;
                }
            });

            return true;
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
            List<Property<Object>> properties = PropertyQueries.createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(MongoField.class)).getResultList();

            MongoCollection classAnnotation = objectClass.getAnnotation(MongoCollection.class);

            String dbCollectionName = classAnnotation==null ? null : classAnnotation.collectionName();
            objectInfo = new ObjectInfo(objectClass, dbCollectionName, properties);

            ObjectInfo existing = objectInfoCache.putIfAbsent(objectClass, objectInfo);
            if (existing != null) {
                objectInfo = existing;
            }
        }

        return objectInfo;
    }

    protected <T extends MongoIdentifiableEntity> List<T> convertCursor(Class<T> type, DBCursor cursor, MongoStoreInvocationContext context) {
        List<T> result = new ArrayList<T>();

        try {
            for (DBObject dbObject : cursor) {
                // First look if we already have loaded object cached. If yes, we will use cached instance
                String id = dbObject.get("_id").toString();
                T object = context.getLoadedObject(type, id);

                if (object == null) {
                    // So convert and use fresh instance from DB
                    ConverterContext<Object> converterContext = new ConverterContext<Object>(dbObject, type, null);
                    object = (T)typeConverter.convertDBObjectToApplicationObject(converterContext);
                    context.addLoadedObject(object);
                }

                result.add(object);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    protected DBCollection getDBCollectionForType(Class<? extends MongoEntity> type) {
        ObjectInfo objectInfo = getObjectInfo(type);
        String dbCollectionName = objectInfo.getDbCollectionName();
        return dbCollectionName==null ? null : database.getCollection(objectInfo.getDbCollectionName());
    }

    // We allow ObjectId to be both "ObjectId" or "String".
    protected Object getObjectId(String idAsString) {
        if (ObjectId.isValid(idAsString)) {
            return new ObjectId(idAsString);
        } else {
            return idAsString;
        }
    }
}
