package org.keycloak.protocol.ssf.event.types.scim;

public class EventFeedAdded extends ScimEvent {

    /**
     * see: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.3.1
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:feed:add";

    public EventFeedAdded() {
        super(TYPE);
    }
}
