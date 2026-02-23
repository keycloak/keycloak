package org.keycloak.scim.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstantMessagingAddress extends MultiValuedAttribute {

    public InstantMessagingAddress() {
    }

    public InstantMessagingAddress(String value, String type, Boolean primary) {
        setValue(value);
        setType(type);
        setPrimary(primary);
    }
}
