package org.keycloak.tests.authz;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.SectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectSectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author mhajas
 */
public abstract class AbstractAuthzTest extends AuthzTestRealmSupport {

    private final List<String> importedRealmNames = new ArrayList<>();
    private final LinkedList<EventRepresentation> eventQueue = new LinkedList<>();
    private final Set<String> processedEventIds = new HashSet<>();

    @InjectAdminClient
    protected Keycloak injectedAdminClient;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectSectorIdentifierRedirectUrisProvider
    protected SectorIdentifierRedirectUrisProvider sectorIdentifierRedirectUrisProvider;

    @BeforeEach
    public void beforeAuthzTest() {
        adminClient = injectedAdminClient;
        // OAuthClient is CLASS-scoped; clear mutable state left by earlier test methods.
        oauth.scope(null);
        importedRealmNames.clear();
        eventQueue.clear();
        processedEventIds.clear();
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
            for (String realmName : importedRealmNames) {
                removeRealm(realmName);
            }
            importedRealmNames.clear();
            eventQueue.clear();
            processedEventIds.clear();
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
            String authServerRoot = authServerRoot();
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

    protected String authServerRoot() {
        String root = oauth.getBaseUrl();
        int realmSegmentIndex = root.indexOf("/realms/");
        if (realmSegmentIndex >= 0) {
            root = root.substring(0, realmSegmentIndex);
        }
        return root;
    }

    protected String pairwiseSectorIdentifierUri() {
        return sectorIdentifierRedirectUrisProvider.getUri();
    }

    protected void configureSectorIdentifierRedirectUris(String... redirectUris) {
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(Arrays.asList(redirectUris));
    }

    protected EventRepresentation pollTestEvent() {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            fetchEventsIntoQueue();
            EventRepresentation event = eventQueue.poll();
            if (event != null) {
                return event;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        fetchEventsIntoQueue();
        return eventQueue.poll();
    }

    protected void clearTestEvents() {
        eventQueue.clear();
        processedEventIds.clear();
        for (String realmName : importedRealmNames) {
            try {
                adminClient.realm(realmName).clearEvents();
            } catch (Exception ignore) {
            }
        }
    }

    private void fetchEventsIntoQueue() {
        for (String realmName : importedRealmNames) {
            List<EventRepresentation> events = adminClient.realm(realmName)
                    .getEvents(null, null, null, null, null, null, null, null, "asc");
            if (events == null) {
                continue;
            }
            for (EventRepresentation event : events) {
                if (event.getId() != null && processedEventIds.add(event.getId())) {
                    eventQueue.add(event);
                }
            }
        }
    }
}
