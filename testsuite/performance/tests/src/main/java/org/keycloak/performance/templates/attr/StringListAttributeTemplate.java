package org.keycloak.performance.templates.attr;

import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.attr.StringListAttribute;
import org.keycloak.performance.dataset.attr.StringListAttributeRepresentation;
import org.keycloak.performance.templates.EntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplate;

/**
 *
 * @author tkyjovsk
 * @param <PE> owner entity
 */
public abstract class StringListAttributeTemplate<PE extends Entity>
        extends NestedEntityTemplate<PE, StringListAttribute<PE>, StringListAttributeRepresentation> {

    public StringListAttributeTemplate(EntityTemplate parentEntityTemplate) {
        super(parentEntityTemplate);
    }

    @Override
    public void processMappings(StringListAttribute<PE> entity) {
    }

    @Override
    public StringListAttribute<PE> newEntity(PE parentEntity, int index) {
        return new StringListAttribute<>(parentEntity, index);
    }

}
