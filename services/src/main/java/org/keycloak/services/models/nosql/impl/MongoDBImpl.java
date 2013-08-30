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
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.nosql.api.AttributedNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBImpl implements NoSQL {

    private final DB database;
    // private static final Logger logger = Logger.getLogger(MongoDBImpl.class);

    public MongoDBImpl(DB database) {
        this.database = database;
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

            // Adding attributes
            if (object instanceof AttributedNoSQLObject) {
                AttributedNoSQLObject attributedObject = (AttributedNoSQLObject)object;
                Map<String, String> attributes = attributedObject.getAttributes();
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    dbObject.append(attribute.getKey(), attribute.getValue());
                }
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
        ObjectInfo<T> objectInfo = getObjectInfo(type);
        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        BasicDBObject idQuery = new BasicDBObject("_id", new ObjectId(oid));
        DBObject dbObject = dbCollection.findOne(idQuery);

        return convertObject(type, dbObject);
    }

    @Override
    public <T extends NoSQLObject> List<T> loadObjects(Class<T> type, Map<String, Object> queryAttributes) {
        ObjectInfo<T> objectInfo = getObjectInfo(type);
        DBCollection dbCollection = database.getCollection(objectInfo.getDbCollectionName());

        BasicDBObject query = new BasicDBObject();
        for (Map.Entry<String, Object> queryAttr : queryAttributes.entrySet()) {
            query.append(queryAttr.getKey(), queryAttr.getValue());
        }
        DBCursor cursor = dbCollection.find(query);

        return convertCursor(type, cursor);
    }

    @Override
    public void removeObject(NoSQLObject object) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeObject(Class<? extends NoSQLObject> type, String oid) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeObjects(Class<? extends NoSQLObject> type, Map<String, Object> queryAttributes) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private <T extends NoSQLObject> ObjectInfo<T> getObjectInfo(Class<?> objectClass) {
        ObjectInfo<T> objectInfo = (ObjectInfo<T>)objectInfoCache.get(objectClass);
        if (objectInfo == null) {
            Property<String> idProperty = PropertyQueries.<String>createQuery(objectClass).addCriteria(new AnnotatedPropertyCriteria(NoSQLId.class)).getFirstResult();
            if (idProperty == null) {
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
                property.setValue(object, value);

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
}
