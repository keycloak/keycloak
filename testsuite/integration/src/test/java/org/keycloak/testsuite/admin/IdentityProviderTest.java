package org.keycloak.testsuite.admin;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.rule.WebRule;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @Test
    public void testFindAll() {
        realm.identityProviders().create(create("google", "google", "Google"));
        realm.identityProviders().create(create("facebook", "facebook", "Facebook"));

        assertNames(realm.identityProviders().findAll(), "google", "facebook");
    }

    @Test
    public void testCreate() {
        IdentityProviderRepresentation newIdentityProvider = create("new-identity-provider", "oidc", "New Identity Provider");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "clientSecret");

        realm.identityProviders().create(newIdentityProvider);
        IdentityProviderResource identityProviderResource = realm.identityProviders().get("new-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertNotNull(representation.getInternalId());
        assertEquals("new-identity-provider", representation.getAlias());
        assertEquals("oidc", representation.getProviderId());
        assertEquals("clientId", representation.getConfig().get("clientId"));
        assertEquals("clientSecret", representation.getConfig().get("clientSecret"));
        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertTrue(representation.isUpdateProfileFirstLogin());
    }

    @Test
    public void testUpdate() {
        IdentityProviderRepresentation newIdentityProvider = create("update-identity-provider", "oidc", "Update Identity Provider");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "clientSecret");

        realm.identityProviders().create(newIdentityProvider);
        IdentityProviderResource identityProviderResource = realm.identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertEquals("update-identity-provider", representation.getAlias());

        representation.setAlias("changed-alias");
        representation.setEnabled(false);
        representation.setStoreToken(true);
        representation.getConfig().put("clientId", "changedClientId");

        identityProviderResource.update(representation);

        identityProviderResource = realm.identityProviders().get(representation.getInternalId());

        assertNotNull(identityProviderResource);

        representation = identityProviderResource.toRepresentation();

        assertFalse(representation.isEnabled());
        assertTrue(representation.isStoreToken());
        assertEquals("changedClientId", representation.getConfig().get("clientId"));
    }

    @Test(expected = NotFoundException.class)
    public void testRemove() {
        IdentityProviderRepresentation newIdentityProvider = create("remove-identity-provider", "saml", "Remove Identity Provider");

        realm.identityProviders().create(newIdentityProvider);
        IdentityProviderResource identityProviderResource = realm.identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        identityProviderResource.remove();

        realm.identityProviders().get("update-identity-provider");
    }

    private IdentityProviderRepresentation create(String id, String providerId, String name) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(id);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }
}
