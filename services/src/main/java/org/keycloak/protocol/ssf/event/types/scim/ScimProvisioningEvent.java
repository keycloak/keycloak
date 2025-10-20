package org.keycloak.protocol.ssf.event.types.scim;

public abstract class ScimProvisioningEvent extends ScimEvent {

    public ScimProvisioningEvent(String type) {
        super(type);
    }
}
