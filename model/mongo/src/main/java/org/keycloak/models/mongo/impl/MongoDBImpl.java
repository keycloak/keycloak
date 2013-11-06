package org.keycloak.models.mongo.impl;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.NoSQLCollection;
import org.keycloak.models.mongo.api.NoSQLField;
import org.keycloak.models.mongo.api.NoSQLId;
import org.keycloak.models.mongo.api.NoSQLObject;
import org.keycloak.models.mongo.api.query.NoSQLQuery;
import org.keycloak.models.mongo.api.query.NoSQLQueryBuilder;
import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.TypeConverter;
import org.keycloak.models.mongo.impl.types.BasicDBListConverter;
import org.keycloak.models.mongo.impl.types.BasicDBObjectConverter;
import org.keycloak.models.mongo.impl.types.EnumToStringConverter;
import org.keycloak.models.mongo.impl.types.ListConverter;
import org.keycloak.models.mongo.impl.types.NoSQLObjectConverter;
import org.keycloak.models.mongo.impl.types.SimpleConverter;
import org.keycloak.models.mongo.impl.types.StringToEnumConverter;
import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBImpl implements NoSQL {

    private static final Class<?>[] SIMPLE_TYPES = { String.class, Integer.class, Boolean.class, Long.class, Double.class, Character.class, Date.class };

    private final DB database;
    private static final Logger logger = Logger.getLogger(MongoDBImpl.class);

    private final TypeConverter typeConverter;
    private ConcurrentMap<Class<? extends NoSQLObject>, ObjectInfo> objectInfoCache =
            new ConcurrentHashMap<Class<? extends NoSQLObject>, ObjectInfo>();


    public MongoDBImpl(DB database, boolean dropDatabaseOnStartup, Class<? extends NoSQLObject>[] managedDataTypes) {
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

        // Enum converters
        typeConverter.addAppObjectConverter(new EnumToStringConverter());
        typeConverter.addDBObjectConverter(new StringToEnumConverter());

        for (Class<? extends NoSQLObject> type : managedDataTypes) {
            getObjectInfo(type);
            typeConverter.addAppObjectConverter(new NoSQLObjectConverter(this, typeConverter, type));
            typeConverter.addDBObjectConverter(new BasicDBObjectConverter(this, typeConverter, type));
        }

        if (dropDatabaseOnStartup) {
            this.database.dropDatabase();
            logger.info("Database " + this.database.getName() + " dropped in MongoDB");
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
            BasicDBObject query = new BasicDBObject("_id", new ObjectId(currentId));
            dbCollection.update(query, dbObject);
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


    @Override
    public <S> void pushItemToList(NoSQLObject object, String listPropertyName, S itemToPush) {
        Class<? extends NoSQLObject> type = object.getClass();
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
        list.add(itemToPush);

        // Push item to DB. We always convert whole list, so it's not so optimal...
        BasicDBList dbList = typeConverter.convertApplicationObjectToDBObject(list, BasicDBList.class);

        BasicDBObject query = new BasicDBObject("_id", new ObjectId(oidProperty.getValue(object)));
        BasicDBObject listObject = new BasicDBObject(listPropertyName, dbList);
        BasicDBObject setCommand = new BasicDBObject("$set", listObject);
        getDBCollectionForType(type).update(query, setCommand);
    }


    @Override
    public <S> void pullItemFromList(NoSQLObject object, String listPropertyName, S itemToPull) {
        Class<? extends NoSQLObject> type = object.getClass();
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
            BasicDBObject query = new BasicDBObject("_id", new ObjectId(oidProperty.getValue(object)));
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
