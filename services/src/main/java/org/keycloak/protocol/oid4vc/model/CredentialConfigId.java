package org.keycloak.protocol.oid4vc.model;

public class CredentialConfigId {
    private String value;
    public CredentialConfigId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
