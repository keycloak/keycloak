package org.keycloak.performance.dataset.attr;

import org.keycloak.performance.dataset.Entity;

/**
 *
 * @author tkyjovsk
 * @param <PE>
 */
public class StringAttribute<PE extends Entity> extends Attribute<PE, StringAttributeRepresentation> {

    public StringAttribute(PE attributeOwner, int index) {
        super(attributeOwner, index);
    }

    @Override
    public StringAttributeRepresentation newRepresentation() {
        return new StringAttributeRepresentation();
    }
    
}
