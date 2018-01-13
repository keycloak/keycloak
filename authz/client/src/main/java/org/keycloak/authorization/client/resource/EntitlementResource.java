package org.keycloak.authorization.client.resource;

import static org.keycloak.authorization.client.util.Throwables.handleAndWrapException;

import org.keycloak.authorization.client.representation.EntitlementRequest;
import org.keycloak.authorization.client.representation.EntitlementResponse;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class EntitlementResource {

    private final Http http;
    private final String eat;

    public EntitlementResource(Http http, String eat) {
        this.http = http;
        this.eat = eat;
    }

    public EntitlementResponse getAll(String resourceServerId) {
        try {
            return this.http.<EntitlementResponse>get("/authz/entitlement/" + resourceServerId)
                    .authorizationBearer(eat)
                    .response().json(EntitlementResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Failed to obtain entitlements", cause);
        }
    }

    public EntitlementResponse get(String resourceServerId, EntitlementRequest request) {
        try {
            return this.http.<EntitlementResponse>post("/authz/entitlement/" + resourceServerId)
                    .authorizationBearer(eat)
                    .json(JsonSerialization.writeValueAsBytes(request))
                    .response().json(EntitlementResponse.class).execute();
        } catch (Exception cause) {
            throw handleAndWrapException("Failed to obtain entitlements", cause);
        }
    }
}
