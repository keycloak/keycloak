package org.keycloak.tests.client;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class DcrSsfPrivilegeEscalationTest extends AbstractClientRegistrationTest {

    @Test
    public void serviceAccountsEnabledCannotBeSetViaRat() throws ClientRegistrationException {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0,1))
                .getToken();

        reg.auth(Auth.token(iat));
        ClientRepresentation defaultClient = reg.create(new ClientRepresentation());
        String dclientId = defaultClient.getClientId();
        String dclientRat = defaultClient.getRegistrationAccessToken();

        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(dclientId);
            if(!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        //Attempt to flip serviceAccountEnable via the defaul RAT
        reg.auth(Auth.token(dclientRat));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(dclientId);
        update.setServiceAccountsEnabled(true);

        ClientRegistrationException ex = Assertions.assertThrows(ClientRegistrationException.class, () -> reg.update(update));
        assertEquals(400, ((HttpErrorException) ex.getCause())
                .getStatusLine().getStatusCode(),"DCR callers must not be permitted to enable service accounts");

        // Server-side state must not have changed
        var found = managedRealm.admin().clients().findByClientId(dclientId);
        Assertions.assertFalse(found.get(0).isServiceAccountsEnabled(),
                "serviceAccountsEnabled must remain false after rejected update");
    }

    @Test
    public void serviceAccountsCannotBeReEnabledViaRatOnResourceServer() throws ClientRegistrationException {
        // A client that is a resource server, but whose service account an admin has disabled
        ClientRepresentation resourceServer = new ClientRepresentation();
        resourceServer.setClientId("authz-resource-server");
        resourceServer.setPublicClient(false);
        resourceServer.setServiceAccountsEnabled(true);
        resourceServer.setAuthorizationServicesEnabled(true);

        String uuid;
        try (var response = managedRealm.admin().clients().create(resourceServer)) {
            assertEquals(201, response.getStatus());
            uuid = ApiUtil.getCreatedId(response);
        }
        managedRealm.cleanup().add(realm -> realm.clients().get(uuid).remove());

        ClientRepresentation stored = managedRealm.admin().clients().get(uuid).toRepresentation();
        stored.setServiceAccountsEnabled(false);
        managedRealm.admin().clients().get(uuid).update(stored);

        stored = managedRealm.admin().clients().get(uuid).toRepresentation();
        Assertions.assertFalse(stored.isServiceAccountsEnabled(),
                "Precondition: the administrator disabled service accounts");
        Assertions.assertTrue(Boolean.TRUE.equals(stored.getAuthorizationServicesEnabled()),
                "Precondition: the client is still a resource server");

        // Update via a registration access token must not re-enable the flag
        String rat = managedRealm.admin().clients().get(uuid)
                .regenerateRegistrationAccessToken()
                .getRegistrationAccessToken();

        reg.auth(Auth.token(rat));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId("authz-resource-server");
        reg.update(update);

        Assertions.assertFalse(managedRealm.admin().clients().get(uuid).toRepresentation().isServiceAccountsEnabled(),
                "serviceAccountsEnabled must not be re-enabled by the authorization settings import");
    }

    @Test
    public void ssfAttributeIsStrippedOnDcrCreate() throws ClientRegistrationException {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0,1))
                .getToken();

        reg.auth(Auth.token(iat));

        ClientRepresentation toCreate = new ClientRepresentation();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("ssf.enabled", "true");
        attrs.put("ssf.customAttribute", "injected");
        attrs.put("safe.attribute", "allowed");
        toCreate.setAttributes(attrs);

        ClientRepresentation created = reg.create(toCreate);
        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(created.getClientId());
            if(!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        // Fetch from Admin API — ssf.* keys must be absent
        var storedAttributes = managedRealm.admin().clients().findByClientId(created.getClientId()).get(0).getAttributes();
        assertNull(storedAttributes.get("ssf.enabled"), "ssf.enabled must not be stored via DCR create");
        assertNull(storedAttributes.get("ssf.customAttribute"), "ssf.customAttribute must not be stored via DCR create");
        assertEquals("allowed", storedAttributes.get("safe.attribute"), "Non-ssf attributes must still be stored");
    }

    @Test
    public void ssfAttributeIsStrippedOnDcrUpdate() throws ClientRegistrationException {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0,1))
                .getToken();

        reg.auth(Auth.token(iat));
        ClientRepresentation defaultClient = reg.create(new ClientRepresentation());
        String dclientId = defaultClient.getClientId();
        String dclientRat = defaultClient.getRegistrationAccessToken();

        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(dclientId);
            if(!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        // Attempt to inject ssf.* via update
        reg.auth(Auth.token(dclientRat));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(dclientId);
        Map<String, String> attrs = new HashMap<>();
        attrs.put("ssf.enabled", "true");
        attrs.put("ssf.customAttribute", "injected");
        attrs.put("safe.update.attr", "allowed");
        update.setAttributes(attrs);

        reg.update(update);   // must succeed (200) but strip ssf.* silently

        var storedAttrs = managedRealm.admin().clients()
                .findByClientId(dclientId).get(0).getAttributes();
        assertNull(storedAttrs.get("ssf.enabled"), "ssf.enabled must not be stored via DCR update");
        assertNull(storedAttrs.get("ssf.customAttribute"), "ssf.customAttribute must not be stored via DCR update");
        assertEquals("allowed", storedAttrs.get("safe.update.attr"), "Non-ssf attributes must still be stored");
    }

    @Test
    public void introspectionAudienceBypassAttributeIsStrippedOnDcrCreate() throws ClientRegistrationException {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1))
                .getToken();

        reg.auth(Auth.token(iat));

        ClientRepresentation toCreate = new ClientRepresentation();
        Map<String, String> attrs = new HashMap<>();
        attrs.put(OIDCConfigAttributes.ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK, "true");
        attrs.put("safe.attribute", "allowed");
        toCreate.setAttributes(attrs);

        ClientRepresentation created = reg.create(toCreate);
        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(created.getClientId());
            if (!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        var storedAttributes = managedRealm.admin().clients().findByClientId(created.getClientId()).get(0).getAttributes();
        assertNull(storedAttributes.get(OIDCConfigAttributes.ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK),
                "allow.token.introspection.without.audience.check must not be stored via DCR create");
        assertEquals("allowed", storedAttributes.get("safe.attribute"), "Non-privileged attributes must still be stored");
    }

    @Test
    public void introspectionAudienceBypassAttributeIsStrippedOnDcrUpdate() throws ClientRegistrationException {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1))
                .getToken();

        reg.auth(Auth.token(iat));
        ClientRepresentation defaultClient = reg.create(new ClientRepresentation());
        String dclientId = defaultClient.getClientId();
        String dclientRat = defaultClient.getRegistrationAccessToken();

        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(dclientId);
            if (!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        reg.auth(Auth.token(dclientRat));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId(dclientId);
        Map<String, String> attrs = new HashMap<>();
        attrs.put(OIDCConfigAttributes.ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK, "true");
        attrs.put("safe.update.attr", "allowed");
        update.setAttributes(attrs);

        reg.update(update);

        var storedAttrs = managedRealm.admin().clients()
                .findByClientId(dclientId).get(0).getAttributes();
        assertNull(storedAttrs.get(OIDCConfigAttributes.ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK),
                "allow.token.introspection.without.audience.check must not be stored via DCR update");
        assertEquals("allowed", storedAttrs.get("safe.update.attr"), "Non-privileged attributes must still be stored");
    }
}
