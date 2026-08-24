package org.keycloak.tests.authz;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.common.util.Throwables;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class ProtectionResourceTest extends AbstractResourceServerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    private ProtectionResource protection;

    @BeforeEach
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
        managedRealm.cleanup().add(r -> {
            settings.setAllowRemoteResourceManagement(true);
            authorization.update(settings);
        });

        String expectedMessage = "Remote management is disabled";
        assertResponse(() -> {
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName(KeycloakModelUtils.generateId());
            protection.resource().create(resource);
        }, Status.BAD_REQUEST, expectedMessage);
        assertResponse(() -> protection.resource().update(existing), Status.BAD_REQUEST, expectedMessage);
        assertResponse(() -> protection.resource().delete(existing.getId()), Status.BAD_REQUEST, expectedMessage);
        assertResponse(() -> protection.resource().findAll(), Status.BAD_REQUEST, expectedMessage);
    }

    @Test
    public void testOnlyOwnerAndServerCanManageResources() throws Exception {
        ResourceRepresentation resource = addResource(KeycloakModelUtils.generateId(), "marta", true);
        ResourceRepresentation existing = assertCanManage(protection, resource);

        ProtectionResource koloClient = getAuthzClient().protection("kolo", "password");
        assertResponse(() -> koloClient.resource().update(existing), Status.FORBIDDEN);
        assertResponse(() -> koloClient.resource().findById(existing.getId()), Status.FORBIDDEN);
        assertEquals(0, koloClient.resource().findAll().length);
        assertResponse(() -> koloClient.resource().delete(existing.getId()), Status.FORBIDDEN);

        ProtectionResource resourceServerClient = getAuthzClient().protection();
        assertCanManage(resourceServerClient, existing);
    }

    private ResourceRepresentation assertCanManage(ProtectionResource protection, ResourceRepresentation resource) throws Exception {
        protection.resource().update(resource);
        resource = protection.resource().findById(resource.getId());
        assertNotNull(resource);
        assertEquals(1, protection.resource().findAll().length);
        protection.resource().delete(resource.getId());
        assertEquals(0, protection.resource().findAll().length);
        return addResource(KeycloakModelUtils.generateId(), "marta", true);
    }

    private void assertResponse(Runnable runnable, Status status) {
        assertResponse(runnable, status, null);
    }

    private void assertResponse(Runnable runnable, Status status, String message) {
        try {
            runnable.run();
            fail("Server should fail the request");
        } catch (Exception e) {
            Throwable error = e.getCause();
            assertTrue(Throwables.isCausedBy(e, HttpResponseException.class));
            if (!(error instanceof HttpResponseException)) {
                error = error.getCause();
            }
            assertEquals(status.getStatusCode(), ((HttpResponseException) error).getStatusCode());
            if (message != null) {
                assertTrue(new String(((HttpResponseException) error).getBytes()).contains(message));
            }
        }
    }
}
