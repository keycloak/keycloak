package org.keycloak.models.mongo.impl;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.jboss.logging.Logger;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.api.context.MongoTask;
import org.keycloak.models.mongo.api.types.Mapper;
import org.keycloak.models.mongo.api.types.MapperContext;
import org.keycloak.models.mongo.api.types.MapperRegistry;
import org.keycloak.models.mongo.impl.types.BasicDBListMapper;
import org.keycloak.models.mongo.impl.types.BasicDBObjectMapper;
import org.keycloak.models.mongo.impl.types.BasicDBObjectToMapMapper;
import org.keycloak.models.mongo.impl.types.EnumToStringMapper;
import org.keycloak.models.mongo.impl.types.ListMapper;
import org.keycloak.models.mongo.impl.types.MapMapper;
import org.keycloak.models.mongo.impl.types.MongoEntityMapper;
import org.keycloak.models.mongo.impl.types.SimpleMapper;
import org.keycloak.models.mongo.impl.types.StringToEnumMapper;
import org.keycloak.models.utils.KeycloakModelUtils;
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

    private final MapperRegistry mapperRegistry;
    private ConcurrentMap<Class<? extends MongoEntity>, EntityInfo> entityInfoCache =
            new ConcurrentHashMap<Class<? extends MongoEntity>, EntityInfo>();


    public MongoStoreImpl(DB database, boolean clearCollectionsOnStartup, Class<? extends MongoEntity>[] managedEntityTypes) {
        this.database = database;

        mapperRegistry = new MapperRegistry();

        for (Class<?> simpleConverterClass : SIMPLE_TYPES) {
            SimpleMapper converter = new SimpleMapper(simpleConverterClass);
            mapperRegistry.addAppObjectMapper(converter);
            mapperRegistry.addDBObjectMapper(converter);
        }

        // Specific converter for ArrayList is added just for performance purposes to avoid recursive converter lookup (most of list idm will be ArrayList)
        mapperRegistry.addAppObjectMapper(new ListMapper(mapperRegistry, ArrayList.class));
        mapperRegistry.addAppObjectMapper(new ListMapper(mapperRegistry, List.class));
        mapperRegistry.addDBObjectMapper(new BasicDBListMapper(mapperRegistry));

        mapperRegistry.addAppObjectMapper(new MapMapper(HashMap.class));
        mapperRegistry.addAppObjectMapper(new MapMapper(Map.class));
        mapperRegistry.addDBObjectMapper(new BasicDBObjectToMapMapper());

        // Enum converters
        mapperRegistry.addAppObjectMapper(new EnumToStringMapper());
        mapperRegistry.addDBObjectMapper(new StringToEnumMapper());

        for (Class<? extends MongoEntity> type : managedEntityTypes) {
            getEntityInfo(type);
            mapperRegistry.addAppObjectMapper(new MongoEntityMapper(this, mapperRegistry, type));
            mapperRegistry.addDBObjectMapper(new BasicDBObjectMapper(this, mapperRegistry, type));
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
    public void insertEntity(MongoIdentifiableEntity entity, MongoStoreInvocationContext context) {
        Class<? extends MongoEntity> clazz = entity.getClass();

        // Find annotations for ID, for all the properties and for the name of the collection.
        EntityInfo entityInfo = getEntityInfo(clazz);

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = mapperRegistry.convertApplicationObjectToDBObject(entity, BasicDBObject.class);

        DBCollection dbCollection = database.getCollection(entityInfo.getDbCollectionName());

        String currentId = entity.getId();

        // Generate random ID if not set already
        if (currentId == null) {
            currentId = KeycloakModelUtils.generateId();
            entity.setId(currentId);
        }

        // Adding "_id"
        dbObject.put("_id", currentId);

        dbCollection.insert(dbObject);

        // Treat object as created in this transaction (It is already submited to transaction)
        context.addCreatedEntity(entity);
    }

    @Override
    public void updateEntity(final MongoIdentifiableEntity entity, MongoStoreInvocationContext context) {
        MongoTask fullUpdateTask = new MongoTask() {

            @Override
            public void execute() {
                Class<? extends MongoEntity> clazz = entity.getClass();
                EntityInfo entityInfo = getEntityInfo(clazz);
                BasicDBObject dbObject = mapperRegistry.convertApplicationObjectToDBObject(entity, BasicDBObject.class);
                DBCollection dbCollection = database.getCollection(entityInfo.getDbCollectionName());

                String currentId = entity.getId();

                if (currentId == null) {
                    throw new IllegalStateException("Can't update entity without id: " + entity);
                } else {
                    BasicDBObject query = new BasicDBObject("_id", currentId);
                    dbCollection.update(query, dbObject);
                }
            }

            @Override
            public boolean isFullUpdate() {
                return true;
            }
        };

        // update is just added to context and postponed
        context.addUpdateTask(entity, fullUpdateTask);
    }


    @Override
    public <T extends MongoIdentifiableEntity> T loadEntity(Class<T> type, String id, MongoStoreInvocationContext context) {
        // First look if we already read the object with this oid and type during this transaction. If yes, use it instead of DB lookup
        T cached = context.getLoadedEntity(type, id);
        if (cached != null) return cached;

        DBCollection dbCollection = getDBCollectionForType(type);

        BasicDBObject idQuery = new BasicDBObject("_id", id);
        DBObject dbObject = dbCollection.findOne(idQuery);

        if (dbObject == null) return null;

        MapperContext<Object, T> mapperContext = new MapperContext<Object, T>(dbObject, type, null);
        T converted = mapperRegistry.convertDBObjectToApplicationObject(mapperContext);

        // Now add it to loaded objects
        context.addLoadedEntity(converted);

        return converted;
    }


    @Override
    public <T extends MongoIdentifiableEntity> T loadSingleEntity(Class<T> type, DBObject query, MongoStoreInvocationContext context) {
        // First we should execute all pending tasks before searching DB
        context.beforeDBSearch(type);

        DBCollection dbCollection = getDBCollectionForType(type);
        DBObject dbObject = dbCollection.findOne(query);

        if (dbObject == null) {
            return null;
        } else {
            return convertDBObjectToEntity(type, dbObject, context);
        }
    }


    @Override
    public <T extends MongoIdentifiableEntity> List<T> loadEntities(Class<T> type, DBObject query, MongoStoreInvocationContext context) {
        // First we should execute all pending tasks before searching DB
        context.beforeDBSearch(type);

        DBCollection dbCollection = getDBCollectionForType(type);
        DBCursor cursor = dbCollection.find(query);

        return convertCursor(type, cursor, context);
    }


    @Override
    public boolean removeEntity(MongoIdentifiableEntity entity, MongoStoreInvocationContext context) {
        return removeEntity(entity.getClass(), entity.getId(), context);
    }


    @Override
    public boolean removeEntity(Class<? extends MongoIdentifiableEntity> type, String id, MongoStoreInvocationContext context) {
        MongoIdentifiableEntity found = loadEntity(type, id, context);
        if (found == null) {
            return false;
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            BasicDBObject dbQuery = new BasicDBObject("_id", id);
            dbCollection.remove(dbQuery);
            logger.info("Entity of type: " + type + ", id: " + id + " removed from MongoDB.");

            context.addRemovedEntity(found);
            return true;
        }
    }


    @Override
    public boolean removeEntities(Class<? extends MongoIdentifiableEntity> type, DBObject query, MongoStoreInvocationContext context) {
        List<? extends MongoIdentifiableEntity> foundObjects = loadEntities(type, query, context);
        if (foundObjects.size() == 0) {
            return false;
        } else {
            DBCollection dbCollection = getDBCollectionForType(type);
            dbCollection.remove(query);
            logger.info("Removed " + foundObjects.size() + " entities of type: " + type + ", query: " + query);

            for (MongoIdentifiableEntity found : foundObjects) {
                context.addRemovedEntity(found);;
            }
            return true;
        }
    }

    @Override
    public <S> boolean pushItemToList(final MongoIdentifiableEntity entity, final String listPropertyName, S itemToPush, boolean skipIfAlreadyPresent, MongoStoreInvocationContext context) {
        final Class<? extends MongoEntity> type = entity.getClass();
        EntityInfo entityInfo = getEntityInfo(type);

        // Add item to list directly in this object
        Property<Object> listProperty = entityInfo.getPropertyByName(listPropertyName);
        if (listProperty == null) {
            throw new IllegalArgumentException("Property " + listPropertyName + " doesn't exist on object " + entity);
        }

        List<S> list = (List<S>)listProperty.getValue(entity);
        if (list == null) {
            list = new ArrayList<S>();
            listProperty.setValue(entity, list);
        }

        // Skip if item is already in list
        if (skipIfAlreadyPresent && list.contains(itemToPush)) {
            return false;
        }

        // Update java object
        list.add(itemToPush);

        // Add update of list to pending tasks
        final List<S> listt = list;
        context.addUpdateTask(entity, new MongoTask() {

            @Override
            public void execute() {
                // Now DB update of new list with usage of $set
                BasicDBList dbList = mapperRegistry.convertApplicationObjectToDBObject(listt, BasicDBList.class);

                BasicDBObject query = new BasicDBObject("_id", entity.getId());
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
    public <S> boolean pullItemFromList(final MongoIdentifiableEntity entity, final String listPropertyName, final S itemToPull, MongoStoreInvocationContext context) {
        final Class<? extends MongoEntity> type = entity.getClass();
        EntityInfo entityInfo = getEntityInfo(type);

        // Remove item from list directly in this object
        Property<Object> listProperty = entityInfo.getPropertyByName(listPropertyName);
        if (listProperty == null) {
            throw new IllegalArgumentException("Property " + listPropertyName + " doesn't exist on object " + entity);
        }
        List<S> list = (List<S>)listProperty.getValue(entity);

        // If list is null, we skip both object and DB update
        if (list == null || !list.contains(itemToPull)) {
            return false;
        } else {

            // Update java object
            list.remove(itemToPull);

            // Add update of list to pending tasks
            context.addUpdateTask(entity, new MongoTask() {

                @Override
                public void execute() {
                    // Pull item from DB
                    Object dbItemToPull = mapperRegistry.convertApplicationObjectToDBObject(itemToPull, Object.class);
                    BasicDBObject query = new BasicDBObject("_id", entity.getId());
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

    // Possibility to add user-defined mappers
    public void addAppObjectConverter(Mapper<?, ?> mapper) {
        mapperRegistry.addAppObjectMapper(mapper);
    }

    public void addDBObjectConverter(Mapper<?, ?> mapper) {
        mapperRegistry.addDBObjectMapper(mapper);
    }

    public EntityInfo getEntityInfo(Class<? extends MongoEntity> entityClass) {
        EntityInfo entityInfo = entityInfoCache.get(entityClass);
        if (entityInfo == null) {
            List<Property<Object>> properties = PropertyQueries.createQuery(entityClass).addCriteria(new AnnotatedPropertyCriteria(MongoField.class)).getResultList();

            MongoCollection classAnnotation = entityClass.getAnnotation(MongoCollection.class);

            String dbCollectionName = classAnnotation==null ? null : classAnnotation.collectionName();
            entityInfo = new EntityInfo(entityClass, dbCollectionName, properties);

            EntityInfo existing = entityInfoCache.putIfAbsent(entityClass, entityInfo);
            if (existing != null) {
                entityInfo = existing;
            }
        }

        return entityInfo;
    }

    protected <T extends MongoIdentifiableEntity> List<T> convertCursor(Class<T> type, DBCursor cursor, MongoStoreInvocationContext context) {
        List<T> result = new ArrayList<T>();

        try {
            for (DBObject dbObject : cursor) {
                T entity = convertDBObjectToEntity(type, dbObject, context);
                result.add(entity);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    protected <T extends MongoIdentifiableEntity> T convertDBObjectToEntity(Class<T> type, DBObject dbObject, MongoStoreInvocationContext context) {
        // First look if we already have loaded object cached. If yes, we will use cached instance
        String id = dbObject.get("_id").toString();
        T object = context.getLoadedEntity(type, id);

        if (object == null) {
            // So convert and use fresh instance from DB
            MapperContext<Object, T> mapperContext = new MapperContext<Object, T>(dbObject, type, null);
            object = mapperRegistry.convertDBObjectToApplicationObject(mapperContext);
            context.addLoadedEntity(object);
        }
        return object;
    }

    protected DBCollection getDBCollectionForType(Class<? extends MongoEntity> type) {
        EntityInfo entityInfo = getEntityInfo(type);
        String dbCollectionName = entityInfo.getDbCollectionName();
        return dbCollectionName==null ? null : database.getCollection(dbCollectionName);
    }
}
