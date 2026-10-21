package org.keycloak.tests.ssf.transmitter;

import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression test for <a href="https://github.com/keycloak/keycloak/issues/49236">keycloak#49236</a>:
 * enabling the SSF transmitter on an existing realm must create the
 * {@code ssf.read} and {@code ssf.manage} client scopes if they are missing.
 */
@KeycloakIntegrationTest(config = SsfTransmitterEnableCreatesClientScopesTests.SsfServerConfig.class)
public class SsfTransmitterEnableCreatesClientScopesTests {

    @InjectRealm(config = SsfDisabledRealm.class)
    ManagedRealm realm;

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

        // The scopes must be registered as optional realm client scopes so receivers have to request them explicitly
        List<String> optionalScopeNames = realm.admin().getDefaultOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .toList();
        Assertions.assertTrue(optionalScopeNames.contains(Ssf.SCOPE_SSF_READ), "ssf.read should be an optional client scope");
        Assertions.assertTrue(optionalScopeNames.contains(Ssf.SCOPE_SSF_MANAGE), "ssf.manage should be an optional client scope");
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
