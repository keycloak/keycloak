package org.keycloak.tests.authz;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author mhajas
 */
public abstract class AbstractAuthzTest extends AbstractKeycloakTest {

    private final List<String> importedRealmNames = new ArrayList<>();

    @InjectAdminClient
    protected Keycloak adminClient;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @BeforeEach
    public void beforeAuthzTest() {
        super.adminClient = adminClient;
        getTestingClient();
        runOnServerMaster = testingClient.server();
        runOnServer = testingClient.server("test");
        importedRealmNames.clear();
        testRealmReps = new ArrayList<>();
        addTestRealms(testRealmReps);
        ensureInjectedOAuthRedirectUris(testRealmReps);
        importTestRealms();
        testRealmReps.forEach(r -> importedRealmNames.add(r.getRealm()));
    }

    @AfterEach
    public void afterAuthzTest() {
        try {
            runManagedCleanupBeforeRealmRemoval();
        } finally {
            try {
                for (String realmName : importedRealmNames) {
                    removeRealm(realmName);
                }
            } finally {
                importedRealmNames.clear();
                closeTestingClient();
            }
        }
    }

    protected void runManagedCleanupBeforeRealmRemoval() {
        Set<ManagedRealm> processedManagedRealms = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Class<?> type = getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!ManagedRealm.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    ManagedRealm managedRealm = (ManagedRealm) field.get(this);
                    if (managedRealm != null && processedManagedRealms.add(managedRealm)) {
                        managedRealm.runCleanup();
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to run managed realm cleanup", e);
                }
            }
        }
    }

    @Override
    public Keycloak getAdminClient() {
        return adminClient;
    }

    @Override
    public KeycloakTestingClient getTestingClient() {
        if (testingClient == null) {
            String authServerRoot = oauth.getBaseUrl();
            int realmSegmentIndex = authServerRoot.indexOf("/realms/");
            if (realmSegmentIndex >= 0) {
                authServerRoot = authServerRoot.substring(0, realmSegmentIndex);
            }
            testingClient = KeycloakTestingClient.getInstance(authServerRoot);
        }
        return testingClient;
    }

    protected AccessToken toAccessToken(String rpt) {
        AccessToken accessToken;

        try {
            accessToken = new JWSInput(rpt).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }
        return accessToken;
    }

    protected PolicyRepresentation createAlwaysGrantPolicy(AuthorizationResource authorization) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.setType("always-grant");
        authorization.policies().create(policy).close();
        return policy;
    }

    protected PolicyRepresentation createAlwaysDenyPolicy(AuthorizationResource authorization) {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(KeycloakModelUtils.generateId());
        policy.setType("always-deny");
        authorization.policies().create(policy).close();
        return policy;
    }

    protected PolicyRepresentation createOnlyOwnerPolicy(AuthorizationResource authorization) {
        PolicyRepresentation onlyOwnerPolicy = new PolicyRepresentation();

        onlyOwnerPolicy.setName(KeycloakModelUtils.generateId());
        onlyOwnerPolicy.setType("allow-resource-owner");

        authorization.policies().create(onlyOwnerPolicy).close();

        return onlyOwnerPolicy;
    }

    protected InputStream authzConfigurationStream(InputStream input) {
        try {
            String authServerRoot = oauth.getBaseUrl();
            int realmSegmentIndex = authServerRoot.indexOf("/realms/");
            if (realmSegmentIndex >= 0) {
                authServerRoot = authServerRoot.substring(0, realmSegmentIndex);
            }
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("http://localhost:8180/auth", authServerRoot)
                    .replace("https://localhost:8543/auth", authServerRoot);
            return new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read authz configuration", e);
        }
    }

    private void ensureInjectedOAuthRedirectUris(List<RealmRepresentation> realms) {
        String redirectUri = oauth.getRedirectUri();
        if (redirectUri == null || redirectUri.isBlank()) {
            return;
        }

        for (RealmRepresentation realm : realms) {
            if (realm.getClients() == null) {
                continue;
            }

            for (ClientRepresentation client : realm.getClients()) {
                if (!Boolean.TRUE.equals(client.isPublicClient())) {
                    continue;
                }

                if (client.getRedirectUris() == null) {
                    client.setRedirectUris(new ArrayList<>());
                }
                if (!client.getRedirectUris().contains(redirectUri)) {
                    client.getRedirectUris().add(redirectUri);
                }
            }
        }
    }

    protected Events createEvents(String realmName) {
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRealm(realmName);
        Events events = new Events(new ManagedRealm(oauth.getBaseUrl() + "/realms/" + realmName, realmRepresentation, adminClient.realm(realmName)));
        events.skipAll();
        return events;
    }

    private void closeTestingClient() {
        if (testingClient != null) {
            testingClient.close();
            testingClient = null;
        }
    }
}
