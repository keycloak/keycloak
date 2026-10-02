package org.keycloak.tests.client;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class DcrSsfPrivilegeEscalationTest extends AbstractClientRegistrationTest {

    private String oidcRat;
    private String registeredClientId;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();
        // Register an ordinary OIDC client via IAT so we hold a real RAT
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0,1))
                .getToken();

        reg.auth(Auth.token(iat));
        OIDCClientRepresentation created =  reg.oidc().create(new OIDCClientRepresentation());
        registeredClientId = created.getClientId();
        oidcRat = created.getRegistrationAccessToken();
        managedRealm.cleanup().add(r -> {
               var found = r.clients().findByClientId(registeredClientId);
               if (!found.isEmpty()) {
                   r.clients().get(found.get(0).getId()).remove();
               }
        });
    }

    @Test
    public void ratFromOidcProviderIsRejectedByDefaultProvider() {
        // Switch the ClientRegistration client to POST against the default endpoint
        reg.auth(Auth.token(oidcRat));
        ClientRepresentation updatedClient = new ClientRepresentation();
        updatedClient.setClientId(registeredClientId);

       ClientRegistrationException ex = Assertions.assertThrows(ClientRegistrationException.class, () -> reg.update(updatedClient));
       assertEquals(401, ((HttpErrorException) ex.getCause()).getStatusLine().getStatusCode(),
               "RAT issued for openid-connect provider must be rejected by the default provider");
    }

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
    public void serviceAccountsEnabledCannotBeSetViaIat() {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1))
                .getToken();

        reg.auth(Auth.token(iat));
        ClientRepresentation toCreate = new ClientRepresentation();
        toCreate.setServiceAccountsEnabled(true);

        ClientRegistrationException ex = Assertions.assertThrows(ClientRegistrationException.class,
                () -> reg.create(toCreate));
        assertEquals(400, ((HttpErrorException) ex.getCause()).getStatusLine().getStatusCode(),
                "IAT callers must not be permitted to create a client with serviceAccountsEnabled=true");
    }

    @Test
    public void authorizationServicesEnabledCannotBeSetViaIat() {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1))
                .getToken();

        reg.auth(Auth.token(iat));
        ClientRepresentation toCreate = new ClientRepresentation();
        toCreate.setAuthorizationServicesEnabled(true);

        ClientRegistrationException ex = Assertions.assertThrows(ClientRegistrationException.class,
                () -> reg.create(toCreate));
        assertEquals(400, ((HttpErrorException) ex.getCause()).getStatusLine().getStatusCode(),
                "IAT callers must not be permitted to create a client with authorizationServicesEnabled=true");
    }

    @Test
    public void ratFromDefaultProviderIsRejectedByOidcProvider() throws ClientRegistrationException {
        // Obtain a RAT from the default provider — it is stamped registration_provider=default
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1))
                .getToken();

        reg.auth(Auth.token(iat));
        ClientRepresentation defaultClient = reg.create(new ClientRepresentation());
        String defaultRat = defaultClient.getRegistrationAccessToken();
        String defaultClientId = defaultClient.getClientId();

        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(defaultClientId);
            if (!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        // Attempt to use the default-provider RAT against the openid-connect endpoint
        reg.auth(Auth.token(defaultRat));
        ClientRegistrationException ex = Assertions.assertThrows(ClientRegistrationException.class,
                () -> reg.oidc().get(defaultClientId));
        assertEquals(401, ((HttpErrorException) ex.getCause()).getStatusLine().getStatusCode(),
                "RAT issued for default provider must be rejected by the openid-connect provider");
    }
}
