package org.keycloak.scim.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Photo extends MultiValuedAttribute {

    public Photo() {
    }

    public Photo(String value, String type, Boolean primary) {
        setValue(value);
        setType(type);
        setPrimary(primary);
    }
}
