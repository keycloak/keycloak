package org.keycloak.tests.ssf.subject;

import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies the auto-notify-on-login behaviour of
 * {@code SsfTransmitterEventListener}: a LOGIN event for a user via a
 * receiver client with {@code ssf.autoNotifyOnLogin=true} and
 * {@code ssf.defaultSubjects=NONE} should set the user-level
 * {@code ssf.notify.<receiverClientId>} attribute — UNLESS the user
 * is already effectively subscribed (per-user attribute already set,
 * OR organization membership covers it). The org-membership leg
 * mirrors the dispatcher's
 * {@link org.keycloak.ssf.transmitter.subject.SubjectSubscriptionFilter}
 * so an admin's curated org-level subscription doesn't get layered
 * over with redundant per-user attribute writes.
 */
@KeycloakIntegrationTest(config = SsfAutoNotifyOnLoginTests.AutoNotifyServerConfig.class)
public class SsfAutoNotifyOnLoginTests {

    static final String RECEIVER = "ssf-receiver-auto";
    static final String RECEIVER_SECRET = "auto-receiver-secret";

    static final String TEST_USER = "auto-notify-user";
    static final String TEST_EMAIL = "auto-notify-user@local.test";
    static final String TEST_PASSWORD = "p@ssw0rd";

    static final String NOTIFY_ATTR = "ssf.notify." + RECEIVER;

    @InjectRealm(config = AutoNotifyRealm.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    public void cleanup() {
        // Clear any per-user notify attribute and remove org memberships
        // / test orgs so each test starts from a clean slate.
        runOnServer.run(session -> {
            RealmModel serverRealm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(serverRealm, TEST_USER);
            if (user != null) {
                user.removeAttribute(NOTIFY_ATTR);
            }
        });
        try {
            realm.admin().organizations().getAll().stream()
                    .filter(o -> o.getAlias() != null && o.getAlias().startsWith("auto-notify-org-"))
                    .forEach(o -> realm.admin().organizations().get(o.getId()).delete());
        } catch (Exception ignored) {
        }
    }

    @Test
    public void loginWritesPerUserAttributeWhenNoOrgInheritance() {
        // Baseline: when the user has no covering org subscription, the
        // listener should auto-tag the user on login.
        triggerLogin();

        runOnServer.run(session -> {
            RealmModel serverRealm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(serverRealm, TEST_USER);
            Assertions.assertEquals("true",
                    user.getFirstAttribute(NOTIFY_ATTR),
                    "user should be auto-tagged when no org inheritance applies");
        });
    }

    @Test
    public void loginSkipsPerUserAttributeWhenOrgAlreadyNotified() {
        // The dispatcher's subject filter already honors org-level
        // ssf.notify.<clientId>, so a user subscribed via an org
        // doesn't need the per-user attribute. The listener should
        // detect that and skip the write.
        String orgAlias = "auto-notify-org-" + System.nanoTime();
        OrganizationRepresentation rep = new OrganizationRepresentation();
        rep.setName(orgAlias);
        rep.setAlias(orgAlias);
        rep.addDomain(new OrganizationDomainRepresentation(orgAlias + ".local.test"));
        rep.singleAttribute(NOTIFY_ATTR, "true");
        try (jakarta.ws.rs.core.Response response = realm.admin().organizations().create(rep)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "test organization creation should succeed");
        }

        runOnServer.run(session -> {
            RealmModel serverRealm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(serverRealm, TEST_USER);
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getAllStream()
                    .filter(o -> orgAlias.equals(o.getAlias()))
                    .findFirst()
                    .orElseThrow();
            orgProvider.addMember(org, user);
        });

        triggerLogin();

        runOnServer.run(session -> {
            RealmModel serverRealm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(serverRealm, TEST_USER);
            List<String> attr = user.getAttributeStream(NOTIFY_ATTR)
                    .toList();
            Assertions.assertTrue(attr.isEmpty(),
                    "user must NOT be auto-tagged when an org membership already covers the receiver — "
                            + "got " + attr);
        });
    }

    /**
     * Fires a LOGIN event server-side via EventBuilder; the SSF event
     * listener picks it up synchronously from the same call and runs
     * {@code autoNotifyOnLogin}. Avoids the OAuth round-trip that an
     * actual login would need.
     */
    protected void triggerLogin() {
        runOnServer.run(session -> {
            RealmModel serverRealm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(serverRealm, TEST_USER);
            ClientModel client = serverRealm.getClientByClientId(RECEIVER);
            new EventBuilder(serverRealm, session)
                    .event(EventType.LOGIN)
                    .user(user)
                    .client(client)
                    .success();
        });
    }

    public static class AutoNotifyServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            // ORGANIZATION is required for the org-inheritance test;
            // SSF for the listener itself.
            config.features(Profile.Feature.SSF, Profile.Feature.ORGANIZATION);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            return configured;
        }
    }

    public static class AutoNotifyRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-auto-notify");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            realm.organizationsEnabled(true);

            realm.eventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_EMAIL)
                            .firstName("Auto")
                            .lastName("Notify")
                            .enabled(true)
                            .password(TEST_PASSWORD)
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER)
                            .secret(RECEIVER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                            .attribute(ClientStreamStore.SSF_AUTO_NOTIFY_ON_LOGIN_KEY, "true")
                            .build()
            );

            return realm;
        }
    }
}
