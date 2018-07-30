package org.keycloak.performance.templates;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.NestedEntity;

/**
 *
 * @author tkyjovsk
 * @param <PE>
 * @param <NE>
 * @param <R>
 */
public abstract class NestedEntityTemplate<PE extends Entity, NE extends NestedEntity<PE, R>, R>
        extends EntityTemplate<NE, R> {

    private final EntityTemplate parentEntityTemplate;

    public static final int ENTITY_CACHE_SIZE = Integer.parseInt(System.getProperty("entity.cache.size", "100000"));

    private final Map<Integer, NE> cache = Collections.synchronizedMap(new LRUMap(ENTITY_CACHE_SIZE));

    public NestedEntityTemplate(EntityTemplate parentEntityTemplate) {
        super(parentEntityTemplate.getConfiguration());
        this.parentEntityTemplate = parentEntityTemplate;
    }

    public EntityTemplate getParentEntityTemplate() {
        return parentEntityTemplate;
    }

    public abstract int getEntityCountPerParent();

    public abstract NE newEntity(PE parentEntity, int index);

    public NE newEntity(PE parentEntity) {
        return newEntity(parentEntity, 0);
    }

    @Override
    public NE newEntity() {
        throw new UnsupportedOperationException("Nested entity must have a parent entity.");
    }

    public NE produce(PE parentEntity, int index) {
        int entityHashcode = configPrefix.hashCode() * index + parentEntity.hashCode();
        return cache.computeIfAbsent(entityHashcode, e -> processEntity(newEntity(parentEntity, index)));
    }

    public NE produce(PE parentEntity) {
        return produce(parentEntity, 0);
    }

    @Override
    public NE produce() {
        throw new UnsupportedOperationException("Nested entity must have a parent entity.");
    }

}
