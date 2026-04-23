package org.keycloak.tests.ssf.transmitter;


import java.util.UUID;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the {@code ssf.streamId} duplicate-detection
 * hook wired into
 * {@link org.keycloak.ssf.transmitter.DefaultSsfTransmitterProviderFactory#validateImportedStreamId}.
 *
 * <p>Exercises the validator against a real Keycloak session so the
 * {@code ClientUpdatedEvent} chain and
 * {@link ModelDuplicateException} propagation out of
 * {@code client.updateClient()} are covered end to end.
 */
@KeycloakIntegrationTest(config = SsfClientStreamIdCollisionTests.CollisionKeycloakServerConfig.class)
public class SsfClientStreamIdCollisionTests {

    @InjectRealm(config = CollisionRealm.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void streamIdMustBeUniqueAcrossClientsInSameRealm() {
        final String realmName = realm.getName();
        final String streamId = UUID.randomUUID().toString();
        final String firstClientId = "collision-first-" + UUID.randomUUID();
        final String secondClientId = "collision-second-" + UUID.randomUUID();

        try {
            // First client takes the streamId cleanly — no collision at this point.
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                Assertions.assertNotNull(r, "test realm must exist");
                ClientModel first = r.addClient(firstClientId);
                first.setAttribute(ClientStreamStore.SSF_STREAM_ID_KEY, streamId);
                // ClientUpdatedEvent — the validator queries the realm for
                // other clients holding the same ssf.streamId; it's alone
                // so no exception should be thrown.
                first.updateClient();
            });

            // Second client attempting to reuse the same streamId must be rejected.
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                ClientModel second = r.addClient(secondClientId);
                second.setAttribute(ClientStreamStore.SSF_STREAM_ID_KEY, streamId);
                ModelDuplicateException thrown = Assertions.assertThrows(
                        ModelDuplicateException.class,
                        second::updateClient,
                        "updateClient should reject a duplicate ssf.streamId in the same realm");
                Assertions.assertTrue(thrown.getMessage().contains(streamId),
                        "exception message must name the duplicated streamId — got: "
                                + thrown.getMessage());
                Assertions.assertTrue(thrown.getMessage().contains(realmName),
                        "exception message must name the realm — got: "
                                + thrown.getMessage());
            });
        } finally {
            // Cleanup both clients regardless of test outcome. The second
            // client may or may not have been persisted depending on
            // whether the ClientUpdatedEvent rolled the surrounding
            // transaction back; use best-effort removal either way.
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                removeIfPresent(r, firstClientId);
                removeIfPresent(r, secondClientId);
            });
        }
    }

    @Test
    public void deleteThenReimportOfSameStreamIdIsAllowed() {
        final String realmName = realm.getName();
        final String streamId = UUID.randomUUID().toString();
        final String firstClientId = "reimport-first-" + UUID.randomUUID();
        final String secondClientId = "reimport-second-" + UUID.randomUUID();

        try {
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                ClientModel first = r.addClient(firstClientId);
                first.setAttribute(ClientStreamStore.SSF_STREAM_ID_KEY, streamId);
                first.updateClient();
                // Delete the first client — simulates the
                // delete-before-reimport workflow where the same
                // streamId legitimately reappears.
                r.removeClient(first.getId());
            });

            // Same streamId, no collision because the previous holder is gone.
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                ClientModel second = r.addClient(secondClientId);
                second.setAttribute(ClientStreamStore.SSF_STREAM_ID_KEY, streamId);
                // Must not throw — the original is gone, so the validator
                // finds only the reimported client.
                Assertions.assertDoesNotThrow(second::updateClient,
                        "reimporting a streamId whose original owner was deleted must be allowed");
            });
        } finally {
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                removeIfPresent(r, firstClientId);
                removeIfPresent(r, secondClientId);
            });
        }
    }

    @Test
    public void clientWithoutStreamIdIsAlwaysAllowed() {
        // Ensures the validator short-circuits when ssf.streamId isn't
        // set — regression guard against a future refactor that would
        // mistakenly run the collision query for every client update.
        final String realmName = realm.getName();
        final String clientId = "nostream-" + UUID.randomUUID();

        try {
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                ClientModel c = r.addClient(clientId);
                c.setAttribute("some.unrelated.attribute", "value");
                Assertions.assertDoesNotThrow(c::updateClient,
                        "clients without ssf.streamId must pass validation");
            });
        } finally {
            runOnServer.run(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                removeIfPresent(r, clientId);
            });
        }
    }

    private static void removeIfPresent(RealmModel realm, String clientId) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client != null) {
            realm.removeClient(client.getId());
        }
    }

    public static class CollisionKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            return configured;
        }
    }

    public static class CollisionRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-streamid-collision");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            return realm;
        }
    }
}
