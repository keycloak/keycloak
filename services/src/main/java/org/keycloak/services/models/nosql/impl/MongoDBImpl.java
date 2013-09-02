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
import org.keycloak.services.models.nosql.api.AttributedNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.api.types.Converter;
import org.keycloak.services.models.nosql.api.types.TypeConverter;
import org.keycloak.services.models.nosql.impl.types.BasicDBListToStringArrayConverter;
import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;
import org.picketlink.common.reflection.Types;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBImpl implements NoSQL {

    private final DB database;
    // private static final Logger logger = Logger.getLogger(MongoDBImpl.class);

    private final TypeConverter typeConverter;

    public MongoDBImpl(DB database) {
        this.database = database;

        typeConverter = new TypeConverter();
        typeConverter.addConverter(new BasicDBListToStringArrayConverter());
    }

    private ConcurrentMap<Class<? extends NoSQLObject>, ObjectInfo<? extends NoSQLObject>> objectInfoCache =
            new ConcurrentHashMap<Class<? extends NoSQLObject>, ObjectInfo<? extends NoSQLObject>>();



    @Override
    public void saveObject(NoSQLObject object) {
        Class<?> clazz = object.getClass();

        // Find annotations for ID, for all the properties and for the name of the collection.
        ObjectInfo objectInfo = getObjectInfo(clazz);

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = new BasicDBObject();
        List<Property<Object>> props = objectInfo.getProperties();
        for (Property<Object> property : props) {
            String propName = property.getName();
            Object propValue = property.getValue(object);


            dbObject.append(propName, propValue);
        }

        // Adding attributes
        if (object instanceof AttributedNoSQLObject) {
            AttributedNoSQLObject attributedObject = (AttributedNoSQLObject)object;
            Map<String, String> attributes = attributedObject.getAttributes();
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                dbObject.append(attribute.getKey(), attribute.getValue());
            }
        }

        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        // Decide if we should insert or update (based on presence of oid property in original object)
        Property<String> oidProperty = objectInfo.getOidProperty();
        String currentId = oidProperty.getValue(object);
        if (currentId == null) {
            dbCollection.insert(dbObject);

            // Add oid to value of given object
            oidProperty.setValue(object, dbObject.getString("_id"));
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

        return convertObject(type, dbObject);
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
        ObjectInfo<?> objectInfo = getObjectInfo(type);

        Property<String> idProperty = objectInfo.getOidProperty();
        String oid = idProperty.getValue(object);

        removeObject(type, oid);
    }

    @Override
    public void removeObject(Class<? extends NoSQLObject> type, String oid) {
        DBCollection dbCollection = getDBCollectionForType(type);

        BasicDBObject dbQuery = new BasicDBObject("_id", new ObjectId(oid));
        dbCollection.remove(dbQuery);
    }

    @Override
    public void removeObjects(Class<? extends NoSQLObject> type, NoSQLQuery query) {
        DBCollection dbCollection = getDBCollectionForType(type);
        BasicDBObject dbQuery = getDBQueryFromQuery(query);

        dbCollection.remove(dbQuery);
    }

    // Possibility to add user-defined converters
    public void addConverter(Converter<?, ?> converter) {
        typeConverter.addConverter(converter);
    }

    private <T extends NoSQLObject> ObjectInfo<T> getObjectInfo(Class<?> objectClass) {
        ObjectInfo<T> objectInfo = (ObjectInfo<T>)objectInfoCache.get(objectClass);
        if (objectInfo == null) {
            Property<String> idProperty = PropertyQueries.<String>createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(NoSQLId.class)).getFirstResult();
            if (idProperty == null) {
                // TODO: should be allowed to have NoSQLObject classes without declared NoSQLId annotation?
                throw new IllegalStateException("Class " + objectClass + " doesn't have property with declared annotation " + NoSQLId.class);
            }

            List<Property<Object>> properties = PropertyQueries.createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(NoSQLField.class)).getResultList();

            NoSQLCollection classAnnotation = objectClass.getAnnotation(NoSQLCollection.class);
            if (classAnnotation == null) {
                throw new IllegalStateException("Class " + objectClass + " doesn't have annotation " + NoSQLCollection.class);
            }

            String dbCollectionName = classAnnotation.collectionName();
            objectInfo = new ObjectInfo<T>((Class<T>)objectClass, dbCollectionName, idProperty, properties);

            ObjectInfo existing = objectInfoCache.putIfAbsent((Class<T>)objectClass, objectInfo);
            if (existing != null) {
                objectInfo = existing;
            }
        }

        return objectInfo;
    }


    private <T extends NoSQLObject> T convertObject(Class<T> type, DBObject dbObject) {
        if (dbObject == null) {
            return null;
        }

        ObjectInfo<T> objectInfo = getObjectInfo(type);

        T object;
        try {
            object = type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (String key : dbObject.keySet()) {
            Object value = dbObject.get(key);
            Property<Object> property;

            if ("_id".equals(key)) {
                // Current property is "id"
                Property<String> idProperty = objectInfo.getOidProperty();
                idProperty.setValue(object, value.toString());

            } else if ((property = objectInfo.getPropertyByName(key)) != null) {
                // It's declared property with @DBField annotation
                Class<?> expectedType = property.getJavaClass();
                Class actualType = value != null ? value.getClass() : expectedType;

                // handle primitives
                expectedType = Types.boxedClass(expectedType);
                actualType = Types.boxedClass(actualType);

                if (actualType.isAssignableFrom(expectedType)) {
                    property.setValue(object, value);
                } else {
                    // we need to convert
                    Object convertedValue = typeConverter.convertDBObjectToApplicationObject(value, expectedType, actualType);
                    property.setValue(object, convertedValue);
                }

            } else if (object instanceof AttributedNoSQLObject) {
                // It's attributed object and property is not declared, so we will call setAttribute
                ((AttributedNoSQLObject)object).setAttribute(key, value.toString());

            } else {
                // Show warning if it's unknown
                // TODO: logging
                // logger.warn("Property with key " + key + " not known for type " + type);
                System.err.println("Property with key " + key + " not known for type " + type);
            }
        }

        return object;
    }

    private <T extends NoSQLObject> List<T> convertCursor(Class<T> type, DBCursor cursor) {
        List<T> result = new ArrayList<T>();

        for (DBObject dbObject : cursor) {
            T converted = convertObject(type, dbObject);
            result.add(converted);
        }

        return result;
    }

    private DBCollection getDBCollectionForType(Class<? extends NoSQLObject> type) {
        ObjectInfo<?> objectInfo = getObjectInfo(type);
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
