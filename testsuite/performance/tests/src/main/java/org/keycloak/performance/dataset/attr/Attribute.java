package org.keycloak.performance.dataset.attr;

import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.NestedEntity;

/**
 *
 * @author tkyjovsk
 * @param <PE> parent entity
 * @param <REP> representation
 */
public abstract class Attribute<PE extends Entity, REP extends AttributeRepresentation> extends NestedEntity<PE, REP> {

    public Attribute(PE attributeOwner, int index) {
        super(attributeOwner, index);
    }
}
