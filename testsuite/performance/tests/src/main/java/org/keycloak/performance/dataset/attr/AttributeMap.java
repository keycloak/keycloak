package org.keycloak.performance.dataset.attr;

import java.util.HashMap;
import java.util.List;

/**
 *
 * @author tkyjovsk
 */
public class AttributeMap<AT> extends HashMap<String, AT> {

    public AttributeMap(List<Attribute<?, AttributeRepresentation<AT>>> attributes) {
        attributes.forEach(attribute -> {
            put(
                    attribute.getRepresentation().getName(),
                    attribute.getRepresentation().getValue()
            );
        });
    }

}
