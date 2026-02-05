package org.keycloak.scim.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class X509Certificate extends MultiValuedAttribute {

    public X509Certificate() {
    }

    public X509Certificate(String value, String type, Boolean primary) {
        setValue(value);
        setType(type);
        setPrimary(primary);
    }
}
