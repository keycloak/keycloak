package org.keycloak.protocol.ssf.event.types.scim;

public class ProvisioningCreatedEventNotice extends ScimProvisioningEvent {

    /**
     * see: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.4.1
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:prov:create:notice";

    public ProvisioningCreatedEventNotice() {
        super(TYPE);
    }
}
