package org.keycloak.protocol.ssf.event.types.scim;

public class EventFeedRemoved extends ScimEvent {

    /**
     * see: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.3.2
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:feed:remove";

    public EventFeedRemoved() {
        super(TYPE);
    }
}
