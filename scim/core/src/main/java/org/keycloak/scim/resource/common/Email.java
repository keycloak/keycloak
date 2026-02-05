package org.keycloak.scim.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Email extends MultiValuedAttribute {

    public Email() {
    }

    public Email(String email) {
        setValue(email);
        setPrimary(true);
        setType("other");
    }

    public Email(String value, String type, Boolean primary) {
        setValue(value);
        setType(type);
        setPrimary(primary);
    }
}
