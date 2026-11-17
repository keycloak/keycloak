package org.keycloak.tests.ssf.transmitter;

import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the SSF client scope provisioning:
 * <ul>
 * <li><a href="https://github.com/keycloak/keycloak/issues/49236">keycloak#49236</a>:
 * enabling the SSF transmitter on an existing realm must create the
 * {@code ssf.read} and {@code ssf.manage} client scopes if they are missing.</li>
 * <li><a href="https://github.com/keycloak/keycloak/issues/51818">keycloak#51818</a>:
 * the scopes must carry the {@code openid-connect} protocol, otherwise the admin console's
 * "Add client scope" dialog filters them out and they cannot be assigned to a receiver client.
 * Scopes created without a protocol by earlier versions must be backfilled.</li>
 * <li>The scopes must not be realm default scopes. A realm-level optional scope with the
 * {@code openid-connect} protocol is assigned to every newly created OIDC client, which would let any client
 * request {@code ssf.manage}. Receivers get the scopes assigned explicitly instead.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SsfTransmitterEnableCreatesClientScopesTests.SsfServerConfig.class)
public class SsfTransmitterEnableCreatesClientScopesTests {

    @InjectRealm(config = SsfDisabledRealm.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testEnablingTransmitterCreatesMissingClientScopes() {

        // The realm was created while the SSF feature was already enabled, so the
        // scopes exist from realm creation. Delete them to simulate a realm that
        // was created before the SSF feature was turned on.
        deleteClientScopeIfPresent(Ssf.SCOPE_SSF_READ);
        deleteClientScopeIfPresent(Ssf.SCOPE_SSF_MANAGE);

        Assertions.assertNull(findClientScope(Ssf.SCOPE_SSF_READ), "precondition: ssf.read should be absent");
        Assertions.assertNull(findClientScope(Ssf.SCOPE_SSF_MANAGE), "precondition: ssf.manage should be absent");

        // Enable the SSF transmitter through a regular realm update, like the admin console does
        realm.updateWithCleanup(r -> r.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true"));

        ClientScopeRepresentation readScope = findClientScope(Ssf.SCOPE_SSF_READ);
        ClientScopeRepresentation manageScope = findClientScope(Ssf.SCOPE_SSF_MANAGE);

        Assertions.assertNotNull(readScope, "ssf.read client scope should be created when the transmitter is enabled");
        Assertions.assertNotNull(manageScope, "ssf.manage client scope should be created when the transmitter is enabled");

        // The admin console only offers scopes with a matching protocol in the "Add client scope" dialog
        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, readScope.getProtocol(), "ssf.read should use the openid-connect protocol");
        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, manageScope.getProtocol(), "ssf.manage should use the openid-connect protocol");

        // The scopes must not be realm default scopes, otherwise every new OIDC client inherits them
        assertNotRealmDefaultClientScopes();
        assertNewClientDoesNotInheritSsfScopes();
    }

    @Test
    public void testReenablingTransmitterRestoresDeletedClientScopes() {

        // Start with the transmitter enabled and the scopes present
        realm.updateWithCleanup(r -> r.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true"));

        // Admin disables the transmitter and removes the scopes
        realm.updateWithCleanup(r -> r.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "false"));
        deleteClientScopeIfPresent(Ssf.SCOPE_SSF_READ);
        deleteClientScopeIfPresent(Ssf.SCOPE_SSF_MANAGE);

        // Re-enabling must bring the scopes back
        realm.updateWithCleanup(r -> r.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true"));

        Assertions.assertNotNull(findClientScope(Ssf.SCOPE_SSF_READ), "ssf.read client scope should be re-created");
        Assertions.assertNotNull(findClientScope(Ssf.SCOPE_SSF_MANAGE), "ssf.manage client scope should be re-created");
    }

    @Test
    public void testEnablingTransmitterBackfillsMissingProtocolOnExistingClientScopes() {

        // Simulate scopes provisioned by a version that did not set the protocol (keycloak#51818).
        // The admin API rejects scopes without a protocol, so recreate that state on the server side.
        deleteClientScopeIfPresent(Ssf.SCOPE_SSF_READ);
        deleteClientScopeIfPresent(Ssf.SCOPE_SSF_MANAGE);

        String realmName = realm.getName();
        runOnServer.run(session -> {
            RealmModel r = session.realms().getRealmByName(realmName);
            Assertions.assertNotNull(r, "test realm must exist");
            for (String scopeName : List.of(Ssf.SCOPE_SSF_READ, Ssf.SCOPE_SSF_MANAGE)) {
                ClientScopeModel scope = r.addClientScope(scopeName);
                scope.setDescription("legacy scope without protocol");
                scope.setIncludeInTokenScope(true);
                r.addDefaultClientScope(scope, false);
            }
        });

        Assertions.assertNull(findClientScope(Ssf.SCOPE_SSF_READ).getProtocol(), "precondition: ssf.read should have no protocol");
        Assertions.assertNull(findClientScope(Ssf.SCOPE_SSF_MANAGE).getProtocol(), "precondition: ssf.manage should have no protocol");
        List<String> legacyOptionalScopeNames = realmDefaultClientScopeNames(false);
        Assertions.assertTrue(legacyOptionalScopeNames.contains(Ssf.SCOPE_SSF_READ), "precondition: ssf.read should be a realm default optional scope");
        Assertions.assertTrue(legacyOptionalScopeNames.contains(Ssf.SCOPE_SSF_MANAGE), "precondition: ssf.manage should be a realm default optional scope");

        // Enabling the transmitter re-runs the SSF bootstrap, which must repair the existing scopes
        realm.updateWithCleanup(r -> r.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true"));

        ClientScopeRepresentation readScope = findClientScope(Ssf.SCOPE_SSF_READ);
        ClientScopeRepresentation manageScope = findClientScope(Ssf.SCOPE_SSF_MANAGE);

        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, readScope.getProtocol(), "ssf.read protocol should be backfilled");
        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, manageScope.getProtocol(), "ssf.manage protocol should be backfilled");

        // Backfilling must not touch anything else on the existing scopes
        Assertions.assertEquals("legacy scope without protocol", readScope.getDescription(), "existing scope description should be preserved");
        Assertions.assertEquals("legacy scope without protocol", manageScope.getDescription(), "existing scope description should be preserved");

        // The legacy realm default registration was harmless without a protocol, but with the protocol set it
        // would hand both scopes to every new OIDC client. The backfill must undo it.
        assertNotRealmDefaultClientScopes();
        assertNewClientDoesNotInheritSsfScopes();
    }

    @Test
    public void testEnablingTransmitterLeavesAdminConfiguredRealmDefaultScopesAlone() {

        // An admin may deliberately register a scope that already has a protocol as a realm default scope.
        // Such a scope is not a legacy artifact and the bootstrap must not touch its registration.
        ClientScopeRepresentation readScope = findClientScope(Ssf.SCOPE_SSF_READ);
        Assertions.assertNotNull(readScope, "precondition: ssf.read should exist");
        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, readScope.getProtocol(), "precondition: ssf.read should have a protocol");
        realm.admin().addDefaultOptionalClientScope(readScope.getId());
        try {
            realm.updateWithCleanup(r -> r.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true"));

            Assertions.assertTrue(realmDefaultClientScopeNames(false).contains(Ssf.SCOPE_SSF_READ),
                    "admin-configured realm default registration of ssf.read should be preserved");
        } finally {
            realm.admin().removeDefaultOptionalClientScope(readScope.getId());
        }
    }

    private void assertNotRealmDefaultClientScopes() {
        List<String> defaultScopeNames = realmDefaultClientScopeNames(true);
        List<String> optionalScopeNames = realmDefaultClientScopeNames(false);
        for (String scopeName : List.of(Ssf.SCOPE_SSF_READ, Ssf.SCOPE_SSF_MANAGE)) {
            Assertions.assertFalse(defaultScopeNames.contains(scopeName), scopeName + " must not be a realm default client scope");
            Assertions.assertFalse(optionalScopeNames.contains(scopeName), scopeName + " must not be a realm default optional client scope");
        }
    }

    private void assertNewClientDoesNotInheritSsfScopes() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("fresh-oidc-client-" + System.nanoTime());
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setEnabled(true);

        String clientUuid;
        try (Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "client creation should succeed");
            clientUuid = ApiUtil.getCreatedId(response);
        }
        try {
            List<String> assignedScopeNames = Stream.concat(
                            realm.admin().clients().get(clientUuid).getDefaultClientScopes().stream(),
                            realm.admin().clients().get(clientUuid).getOptionalClientScopes().stream())
                    .map(ClientScopeRepresentation::getName)
                    .toList();
            Assertions.assertFalse(assignedScopeNames.contains(Ssf.SCOPE_SSF_READ), "a new OIDC client must not get ssf.read assigned automatically");
            Assertions.assertFalse(assignedScopeNames.contains(Ssf.SCOPE_SSF_MANAGE), "a new OIDC client must not get ssf.manage assigned automatically");
        } finally {
            realm.admin().clients().get(clientUuid).remove();
        }
    }

    private List<String> realmDefaultClientScopeNames(boolean defaultScope) {
        List<ClientScopeRepresentation> scopes = defaultScope
                ? realm.admin().getDefaultDefaultClientScopes()
                : realm.admin().getDefaultOptionalClientScopes();
        return scopes.stream().map(ClientScopeRepresentation::getName).toList();
    }

    private ClientScopeRepresentation findClientScope(String name) {
        return realm.admin().clientScopes().findAll().stream()
                .filter(scope -> name.equals(scope.getName()))
                .findFirst()
                .orElse(null);
    }

    private void deleteClientScopeIfPresent(String name) {
        ClientScopeRepresentation scope = findClientScope(name);
        if (scope != null) {
            realm.admin().clientScopes().get(scope.getId()).remove();
        }
    }

    public static class SsfServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            return configured;
        }
    }

    public static class SsfDisabledRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // Intentionally no ssf.transmitterEnabled attribute — the realm starts
            // with the transmitter disabled, like a realm created before SSF was adopted
            return realm.name("ssf-enable-scopes");
        }
    }
}
