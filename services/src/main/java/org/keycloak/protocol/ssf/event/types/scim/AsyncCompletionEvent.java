package org.keycloak.protocol.ssf.event.types.scim;

public class AsyncCompletionEvent extends ScimEvent {

    /**
     * see: https://www.ietf.org/archive/id/draft-ietf-scim-events-07.html#section-2.5.1.3
     */
    public static final String TYPE = "urn:ietf:params:SCIM:event:misc:asyncResp";

    public AsyncCompletionEvent() {
        super(TYPE);
    }
}
