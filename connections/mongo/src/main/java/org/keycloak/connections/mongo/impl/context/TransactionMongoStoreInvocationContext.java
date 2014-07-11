package org.keycloak.connections.mongo.impl.context;

import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.connections.mongo.api.context.MongoTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Invocation context, which has some very basic support for transactions, and is able to cache loaded objects.
 * It always execute all pending update tasks before start searching for other objects
 *
 * It's per-request object (not thread safe)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TransactionMongoStoreInvocationContext implements MongoStoreInvocationContext {

    // Assumption is that all objects has unique ID (unique across all the types)
    private Map<String, MongoIdentifiableEntity> loadedObjects = new HashMap<String, MongoIdentifiableEntity>();

    private Map<MongoIdentifiableEntity, Set<MongoTask>> pendingUpdateTasks = new HashMap<MongoIdentifiableEntity, Set<MongoTask>>();

    private final MongoStore mongoStore;

    public TransactionMongoStoreInvocationContext(MongoStore mongoStore) {
        this.mongoStore = mongoStore;
    }

    @Override
    public void addCreatedEntity(MongoIdentifiableEntity entity) {
        // For now just add it to list of loaded objects
        addLoadedEntity(entity);
    }

    @Override
    public void addLoadedEntity(MongoIdentifiableEntity entity) {
        loadedObjects.put(entity.getId(), entity);
    }

    @Override
    public <T extends MongoIdentifiableEntity> T getLoadedEntity(Class<T> type, String id) {
        return (T)loadedObjects.get(id);
    }

    @Override
    public void addUpdateTask(MongoIdentifiableEntity entityToUpdate, MongoTask task) {
        if (!loadedObjects.containsValue(entityToUpdate)) {
            throw new IllegalStateException("Entity " + entityToUpdate + " not found in loaded objects");
        }

        Set<MongoTask> currentObjectTasks = pendingUpdateTasks.get(entityToUpdate);
        if (currentObjectTasks == null) {
            currentObjectTasks = new LinkedHashSet<MongoTask>();
            pendingUpdateTasks.put(entityToUpdate, currentObjectTasks);
        } else {
            // if task is full update, then remove all other tasks as we need to do full update of object anyway
            if (task.isFullUpdate()) {
                currentObjectTasks.clear();
            } else {
                // If it already contains task for fullUpdate, then we don't need to add ours as we need to do full update of object anyway
                for (MongoTask current : currentObjectTasks) {
                     if (current.isFullUpdate()) {
                         return;
                     }
                }
            }
        }

        currentObjectTasks.add(task);
    }

    @Override
    public void addRemovedEntity(MongoIdentifiableEntity entity) {
        // Remove all pending tasks and object from cache
        pendingUpdateTasks.remove(entity);
        loadedObjects.remove(entity.getId());

        entity.afterRemove(this);
    }

    @Override
    public void beforeDBSearch(Class<? extends MongoIdentifiableEntity> entityType) {
        // Now execute pending update tasks of type, which will be searched
        Set<MongoIdentifiableEntity> toRemove = new HashSet<MongoIdentifiableEntity>();

        for (MongoIdentifiableEntity currentEntity : pendingUpdateTasks.keySet()) {
            if (currentEntity.getClass().equals(entityType)) {
                Set<MongoTask> mongoTasks = pendingUpdateTasks.get(currentEntity);
                for (MongoTask currentTask : mongoTasks) {
                    currentTask.execute();
                }

                toRemove.add(currentEntity);
            }
        }

        // Now remove all done tasks
        for (MongoIdentifiableEntity entity : toRemove) {
            pendingUpdateTasks.remove(entity);
        }
    }

    @Override
    public void begin() {
        loadedObjects.clear();
        pendingUpdateTasks.clear();
    }

    @Override
    public void commit() {
        // Now execute all pending update tasks
        for (Set<MongoTask> mongoTasks : pendingUpdateTasks.values()) {
            for (MongoTask currentTask : mongoTasks) {
                currentTask.execute();
            }
        }

        // And clear it
        loadedObjects.clear();
        pendingUpdateTasks.clear();
    }

    @Override
    public void rollback() {
        // Just clear the map without executions of tasks TODO: Attempt to do complete rollback (removal of created objects, restoring of removed objects, rollback of updates)
        loadedObjects.clear();
        pendingUpdateTasks.clear();
    }

    @Override
    public MongoStore getMongoStore() {
        return mongoStore;
    }
}
