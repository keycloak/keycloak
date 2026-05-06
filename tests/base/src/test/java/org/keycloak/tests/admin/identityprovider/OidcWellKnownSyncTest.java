package org.keycloak.tests.admin.identityprovider;

import java.time.Duration;
import java.util.HashMap;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.representations.idm.IdentityProviderReloadWellKnownRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OidcWellKnownSyncTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private String wellKnownUrl() {
        return realm.getBaseUrl() + "/.well-known/openid-configuration";
    }

    private IdentityProviderRepresentation buildOidcIdp(String alias) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        idp.setConfig(new HashMap<>());
        idp.getConfig().put("clientId", "test");
        idp.getConfig().put("clientSecret", "test");
        return idp;
    }

    @Test
    public void testInitialSyncOnCreate() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-create-idp");
        idp.getConfig().put("metadataDescriptorUrl", wellKnownUrl());
        idp.getConfig().put("reloadEnabled", "true");
        idp.getConfig().put("includedWellKnownFields",
            "authorizationUrl##tokenUrl##logoutUrl##userInfoUrl##tokenIntrospectionUrl##issuer##jwksUrl");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-create-idp").remove());

        // Fetch back without any manual reloadWellKnown call - fields should be populated by create-time sync
        IdentityProviderRepresentation created = realm.admin().identityProviders().get("sync-create-idp").toRepresentation();

        assertNotNull(created.getConfig().get("authorizationUrl"));
        assertNotNull(created.getConfig().get("wellKnownLastSyncAttempt"));
        assertNull(created.getConfig().get("wellKnownLastSyncError"));
    }

    @Test
    public void testReloadWellKnownHappyPath() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-test-idp");
        idp.getConfig().put("metadataDescriptorUrl", wellKnownUrl());
        idp.getConfig().put("reloadEnabled", "true");
        idp.getConfig().put("includedWellKnownFields",
            "authorizationUrl##tokenUrl##logoutUrl##userInfoUrl##tokenIntrospectionUrl##issuer##jwksUrl");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-test-idp").remove());

        IdentityProviderRepresentation updated = realm.admin().identityProviders()
                .get("sync-test-idp").reloadWellKnown(new IdentityProviderReloadWellKnownRepresentation());

        assertNotNull(updated.getConfig().get("wellKnownLastSyncAttempt"));
        assertNotNull(updated.getConfig().get("wellKnownLastSyncAttemptDuration"));
        assertNull(updated.getConfig().get("wellKnownLastSyncError"));
        assertNotNull(updated.getConfig().get("authorizationUrl"));
        // we expect a jwks_uri, so expect those properties
        assertEquals("true", updated.getConfig().get("validateSignature"));
        assertEquals("true", updated.getConfig().get("useJwksUrl"));
    }

    @Test
    public void testReloadWellKnownNoChangePath() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-nochange-idp");
        idp.getConfig().put("metadataDescriptorUrl", wellKnownUrl());
        idp.getConfig().put("reloadEnabled", "true");
        idp.getConfig().put("includedWellKnownFields",
            "authorizationUrl##tokenUrl##logoutUrl##userInfoUrl##tokenIntrospectionUrl##issuer##jwksUrl");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-nochange-idp").remove());

        // Fetch initial timestamp
        String firstSync = realm.admin().identityProviders()
                .get("sync-nochange-idp").toRepresentation().getConfig().get("wellKnownLastSyncAttempt");

        try (var ignored = timeOffSet.withOffset(Duration.ofSeconds(1))) {
            // Sync again - no config should change, but timestamp should update
            String secondSync = realm.admin().identityProviders()
                    .get("sync-nochange-idp").reloadWellKnown(new IdentityProviderReloadWellKnownRepresentation())
                    .getConfig().get("wellKnownLastSyncAttempt");

            assertNotNull(firstSync);
            assertNotNull(secondSync);
            // Timestamp should have advanced by at least a second
            assertTrue(Long.parseLong(secondSync) - Long.parseLong(firstSync) > 1000,
                    "Second sync should be at least one second later than previous sync");
        }
    }

    @Test
    public void testReloadWellKnownFailurePath() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-fail-idp");
        idp.getConfig().put("metadataDescriptorUrl", wellKnownUrl() + "/invalid");
        idp.getConfig().put("reloadEnabled", "true");
        idp.getConfig().put("includedWellKnownFields", "authorizationUrl##tokenUrl");
        // Pre-set authorizationUrl so we can verify it was NOT overwritten
        idp.getConfig().put("authorizationUrl", "http://localhost/original-url/auth");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-fail-idp").remove());

        IdentityProviderRepresentation updated = realm.admin().identityProviders()
                .get("sync-fail-idp").reloadWellKnown(new IdentityProviderReloadWellKnownRepresentation());

        // Config field must NOT have been overwritten
        assertEquals("http://localhost/original-url/auth", updated.getConfig().get("authorizationUrl"));
        // Error must have been recorded
        assertNotNull(updated.getConfig().get("wellKnownLastSyncError"));
        // Timestamps must have been written
        assertNotNull(updated.getConfig().get("wellKnownLastSyncAttempt"));
        assertNotNull(updated.getConfig().get("wellKnownLastSyncAttemptDuration"));
    }

    @Test
    public void testReloadWellKnownInclusionList() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-include-idp");
        idp.getConfig().put("metadataDescriptorUrl", wellKnownUrl());
        idp.getConfig().put("reloadEnabled", "true");
        // Only include authorizationUrl and tokenUrl - NOT logoutUrl
        idp.getConfig().put("includedWellKnownFields", "authorizationUrl##tokenUrl");
        idp.getConfig().put("logoutUrl", "http://localhost/my-custom-logout/");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-include-idp").remove());

        IdentityProviderRepresentation updated = realm.admin().identityProviders()
                .get("sync-include-idp").reloadWellKnown(new IdentityProviderReloadWellKnownRepresentation());

        // Included field must have been synced
        assertNotNull(updated.getConfig().get("authorizationUrl"));
        // Excluded field must NOT have been overwritten
        assertEquals("http://localhost/my-custom-logout/", updated.getConfig().get("logoutUrl"));
    }

    @Test
    public void testReloadWellKnownInclusionListOverriddenByRequest() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-override-idp");
        idp.getConfig().put("metadataDescriptorUrl", wellKnownUrl());
        idp.getConfig().put("reloadEnabled", "true");
        // Server config says only sync authorizationUrl
        idp.getConfig().put("includedWellKnownFields", "authorizationUrl");
        idp.getConfig().put("logoutUrl", "http://localhost/my-custom-logout/");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-override-idp").remove());

        // Request body overrides the inclusion list to also sync logoutUrl
        IdentityProviderReloadWellKnownRepresentation req = new IdentityProviderReloadWellKnownRepresentation();
        req.setIncludedWellKnownFields("authorizationUrl##logoutUrl");
        IdentityProviderRepresentation updated = realm.admin().identityProviders()
                .get("sync-override-idp").reloadWellKnown(req);

        // logoutUrl must have been overwritten by the well-known value (not the original placeholder)
        assertNotEquals("http://localhost/my-custom-logout/", updated.getConfig().get("logoutUrl"));
        // The override must also be persisted on the IDP
        assertEquals("authorizationUrl##logoutUrl", updated.getConfig().get("includedWellKnownFields"));
    }

    @Test
    public void testReloadWellKnownRejectedForNonOidc() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("sync-saml-idp");
        idp.setProviderId("saml");
        idp.setEnabled(true);
        idp.setConfig(new HashMap<>());
        idp.getConfig().put("metadataDescriptorUrl", realm.getBaseUrl() + "/protocol/saml/descriptor");
        idp.getConfig().put("singleSignOnServiceUrl", realm.getBaseUrl() + "/protocol/saml");
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-saml-idp").remove());

        assertThrows(BadRequestException.class, () ->
            realm.admin().identityProviders().get("sync-saml-idp").reloadWellKnown(new IdentityProviderReloadWellKnownRepresentation()));
    }

    @Test
    public void testReloadWellKnownRejectedWhenNoMetadataUrl() {
        IdentityProviderRepresentation idp = buildOidcIdp("sync-nourl-idp");
        // No metadataDescriptorUrl set
        realm.admin().identityProviders().create(idp).close();
        realm.cleanup().add(r -> r.identityProviders().get("sync-nourl-idp").remove());

        assertThrows(BadRequestException.class, () ->
            realm.admin().identityProviders().get("sync-nourl-idp").reloadWellKnown(new IdentityProviderReloadWellKnownRepresentation()));
    }
}
