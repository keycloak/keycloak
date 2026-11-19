package org.keycloak.protocol.oauth2.cimd.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientModelContext;

/**
 * The resource indicator (RFC 8707) that an access token of a client is bound to.
 *
 * <p>An executor that restricts the resource indicators of the client either rejects the resource by throwing a
 * {@code ClientPolicyException} or approves it. The resource is added to the audience of the access token only if it
 * is approved, so a resource is not issued when no such executor applies to the client.
 */
public class CimdResourceIndicatorContext implements ClientModelContext {

    private final ClientModel client;
    private final String resource;
    private boolean approved;

    public CimdResourceIndicatorContext(ClientModel client, String resource) {
        this.client = client;
        this.resource = resource;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.CIMD_RESOURCE_INDICATOR;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    public String getResource() {
        return resource;
    }

    public void approve() {
        approved = true;
    }

    public boolean isApproved() {
        return approved;
    }
}
