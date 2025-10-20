package org.keycloak.protocol.ssf.event.types.scim;

public class ProvisioningPutEventNotice extends ScimProvisioningEvent {

    /**
     * see: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.4.3
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:prov:put:notice";

    public ProvisioningPutEventNotice() {
        super(TYPE);
    }
}
