package org.keycloak.authorization.client.resource;

import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.representation.AuthorizationRequestMetadata;
import org.keycloak.authorization.client.representation.EntitlementRequest;
import org.keycloak.authorization.client.representation.EntitlementResponse;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.authorization.client.util.HttpMethod;
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
            return getAll(resourceServerId, null);
        } catch (HttpResponseException e) {
            if (403 == e.getStatusCode()) {
                throw new AuthorizationDeniedException(e);
            }
            throw new RuntimeException("Failed to obtain entitlements.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain entitlements.", e);
        }
    }

    public EntitlementResponse getAll(String resourceServerId, AuthorizationRequestMetadata metadata) {
        try {
            HttpMethod<EntitlementResponse> method = this.http.<EntitlementResponse>get("/authz/entitlement/" + resourceServerId)
                    .authorizationBearer(this.eat);

            if (metadata != null) {
                method.param("include_resource_name", String.valueOf(metadata.isIncludeResourceName()));
            }

            return method.response().json(EntitlementResponse.class).execute();
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
