package org.keycloak.scim.resource.user;

import org.keycloak.scim.resource.common.MultiValuedAttribute;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Email extends MultiValuedAttribute {

    public Email() {
        setType("work");
        setPrimary(true);
    }

    public Email(String email) {
        setValue(email);
        setPrimary(true);
        setType("work");
    }

    public Email(String value, String type, Boolean primary) {
        setValue(value);
        setType(type);
        setPrimary(primary);
    }
}
