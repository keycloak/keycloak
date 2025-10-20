package org.keycloak.protocol.ssf.event.types.scim;

public class ProvisioningPatchEventNotice extends ScimProvisioningEvent {

    /**
     * see: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.4.2
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:prov:patch:notice";

    public ProvisioningPatchEventNotice() {
        super(TYPE);
    }
}
