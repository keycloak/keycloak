package org.keycloak.performance.templates;

import java.util.AbstractList;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.NestedEntity;

/**
 * A wrapper list for NestedEntityTemplate which delegates to the
 template if requested element is absent in cache.
 *
 * @author tkyjovsk
 * @param <PE> parent entity type
 * @param <NIE> child entity type
 */
public class NestedEntityTemplateWrapperList<PE extends Entity, NIE extends NestedEntity<PE, R>, R> extends AbstractList<NIE> {

    PE parentEntity;
    NestedEntityTemplate<PE, NIE, R> nestedEntityTemplate;

    public NestedEntityTemplateWrapperList(PE parentEntity, NestedEntityTemplate<PE, NIE, R> nestedEntityTemplate) {
        this.parentEntity = parentEntity;
        this.nestedEntityTemplate = nestedEntityTemplate;
    }

    @Override
    public int size() {
        return nestedEntityTemplate.getEntityCountPerParent();
    }

    @Override
    public NIE get(int index) {
        return nestedEntityTemplate.produce(parentEntity, index);
    }

}
