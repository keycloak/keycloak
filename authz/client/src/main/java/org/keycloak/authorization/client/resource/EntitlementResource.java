package org.keycloak.authorization.client.resource;

import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.representation.EntitlementRequest;
import org.keycloak.authorization.client.representation.EntitlementResponse;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.HttpResponseException;
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
                    .authorizationBearer(this.eat)
                    .response()
                        .json(EntitlementResponse.class).execute();
        } catch (HttpResponseException e) {
            if (403 == e.getStatusCode()) {
                throw new AuthorizationDeniedException(e);
            }
            throw new RuntimeException("Failed to obtain entitlements.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain entitlements.", e);
        }
    }

    public EntitlementResponse get(String resourceServerId, EntitlementRequest request) {
        try {
            return this.http.<EntitlementResponse>post("/authz/entitlement/" + resourceServerId)
                    .authorizationBearer(this.eat)
                    .json(JsonSerialization.writeValueAsBytes(request))
                    .response().json(EntitlementResponse.class).execute();
        } catch (HttpResponseException e) {
            if (403 == e.getStatusCode()) {
                throw new AuthorizationDeniedException(e);
            }
            throw new RuntimeException("Failed to obtain entitlements.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain entitlements.", e);
        }
    }
}
