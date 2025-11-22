package org.keycloak.protocol.ssf.event.types.scim;

public class ProvisioningPatchEventFull extends ScimProvisioningEvent {

    /**
     * See: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.4.2
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:prov:patch:full";

    public ProvisioningPatchEventFull() {
        super(TYPE);
    }
}
