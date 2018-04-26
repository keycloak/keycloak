package org.keycloak.performance.dataset.attr;

import org.keycloak.performance.dataset.Entity;

/**
 *
 * @author tkyjovsk
 * @param <PE> owner entity
 */
public class StringListAttribute<PE extends Entity> extends Attribute<PE, StringListAttributeRepresentation> {

    public StringListAttribute(PE attributeOwner, int index) {
        super(attributeOwner, index);
    }

    @Override
    public StringListAttributeRepresentation newRepresentation() {
        return new StringListAttributeRepresentation();
    }

}
