package org.keycloak.protocol.ssf.event.types.scim;

public class ProvisioningDeactivatedEvent extends ScimProvisioningEvent {

    /**
     * See: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.4.6
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:prov:deactivate";

    public ProvisioningDeactivatedEvent() {
        super(TYPE);
    }
}
