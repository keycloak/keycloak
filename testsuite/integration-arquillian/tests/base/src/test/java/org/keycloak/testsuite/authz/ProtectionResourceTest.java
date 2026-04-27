package org.keycloak.testsuite.authz;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.common.util.Throwables;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ProtectionResourceTest extends AbstractResourceServerTest {

    private ProtectionResource protection;

    @Before
    public void onBefore() {
        protection = getAuthzClient().protection("marta", "password");
    }

    @Test
    public void testAllowRemoteManagement() throws Exception {
        AuthorizationResource authorization = getClient(getRealm()).authorization();
        ResourceRepresentation existing = addResource("test", "marta", true);
        ResourceServerRepresentation settings = authorization.getSettings();
        settings.setAllowRemoteResourceManagement(false);
        authorization.update(settings);
        getCleanup().addCleanup(() -> {
            settings.setAllowRemoteResourceManagement(true);
            authorization.update(settings);
        });

        assertBadRequest(() -> {
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName(KeycloakModelUtils.generateId());
            protection.resource().create(resource);
        });
        assertBadRequest(() -> protection.resource().update(existing));
        assertBadRequest(() -> protection.resource().delete(existing.getId()));
        assertBadRequest(() -> protection.resource().findAll());
    }

    private void assertBadRequest(Runnable runnable) {
        try {
            runnable.run();
            fail("Server should fail the request");
        } catch (Exception e) {
            Throwable error = e.getCause();
            assertTrue(Throwables.isCausedBy(e, HttpResponseException.class));
            if (!(error instanceof HttpResponseException)) {
                error = error.getCause();
            }
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ((HttpResponseException) error).getStatusCode());
            assertTrue(new String(((HttpResponseException) error).getBytes()).contains("Remote management is disabled"));
        }
    }
}
