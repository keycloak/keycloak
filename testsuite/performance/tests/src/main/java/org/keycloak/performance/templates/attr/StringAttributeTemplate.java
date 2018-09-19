package org.keycloak.performance.templates.attr;

import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.attr.StringAttribute;
import org.keycloak.performance.dataset.attr.StringAttributeRepresentation;
import org.keycloak.performance.templates.EntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplate;

/**
 *
 * @author tkyjovsk
 * @param <PE> owner entity
 */
public abstract class StringAttributeTemplate<PE extends Entity>
        extends NestedEntityTemplate<PE, StringAttribute<PE>, StringAttributeRepresentation> {

    public StringAttributeTemplate(EntityTemplate parentEntityTemplate) {
        super(parentEntityTemplate);
    }

}
