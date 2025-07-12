package org.keycloak.dom.saml.v2.metadata;

import org.keycloak.dom.saml.v2.assertion.AttributeType;

public class RequestedAttributeValueType extends AttributeType {
    protected Boolean isRequired;
    protected String value;

    public RequestedAttributeValueType(String name) {
        super(name);
        this.isRequired = Boolean.FALSE;
    }

    public Boolean isIsRequired() {
        return this.isRequired;
    }

    public void setIsRequired(Boolean value) {
        this.isRequired = value;
    }
}
