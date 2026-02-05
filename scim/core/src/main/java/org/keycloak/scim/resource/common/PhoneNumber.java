package org.keycloak.scim.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhoneNumber extends MultiValuedAttribute {

    public PhoneNumber() {
    }

    public PhoneNumber(String value, String type, Boolean primary) {
        setValue(value);
        setType(type);
        setPrimary(primary);
    }
}
