/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.keys;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.scheduled.AutomaticKeyRotationTask;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.BasicRealmWithUserConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for automatic key rotation feature.
 * 
 * @author <a href="mailto:volck@redhat.com">Volck</a>
 */
@KeycloakIntegrationTest(config = AutomaticKeyRotationTest.ServerConfigWithMetrics.class)
public class AutomaticKeyRotationTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @BeforeEach
    public void cleanupOldTestProviders() {
        // Remove any leftover test providers from previous test runs
        // This prevents interference from old providers with auto-rotation enabled
        // Also clean up rotated providers (named "rsa-generated-<timestamp>" by rotateKey())
        realm.admin().components().query(realm.getId(), KeyProvider.class.getName())
                .stream()
                .filter(c -> c.getName() != null && 
                        (c.getName().startsWith("test-auto-rotation-") || 
                         c.getName().startsWith("test-no-rotation-") ||
                         c.getName().startsWith("test-hmac-rotation-") ||
                         c.getName().startsWith("test-ecdsa-rotation-") ||
                         c.getName().startsWith("test-ecdh-rotation-") ||
                         c.getName().startsWith("test-neg-grace-") ||
                         c.getName().startsWith("test-zero-period-") ||
                         c.getName().startsWith("test-disabled-rotation-") ||
                         c.getName().startsWith("test-nodistime-") ||
                         c.getName().startsWith("test-gauge-") ||
                         c.getName().startsWith("test-actiontoken-") ||
                         c.getName().startsWith("test-clienttoken-") ||
                         c.getName().startsWith("test-adminevent-") ||
                         c.getName().startsWith("test-preact-") ||
                         c.getName().matches("rsa-generated-\\d+") ||
                         c.getName().matches("hmac-generated-\\d+") ||
                         c.getName().matches("ecdsa-generated-\\d+") ||
                         c.getName().matches("ecdh-generated-\\d+")))
                .forEach(c -> {
                    try {
                        realm.admin().components().component(c.getId()).remove();
                        System.out.println("[DEBUG] Cleaned up old test provider: " + c.getName());
                    } catch (Exception e) {
                        System.err.println("[DEBUG] Failed to clean up provider " + c.getName() + ": " + e.getMessage());
                    }
                });
    }

    @Test
    public void testAutomaticKeyRotationConfiguration() {
        // Create a key provider with automatic rotation enabled
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Verify configuration was saved correctly
        ComponentRepresentation storedProvider = realm.admin().components().component(keyProviderId).toRepresentation();
        assertEquals("true", storedProvider.getConfig().getFirst(Attributes.AUTO_ROTATION_ENABLED_KEY));
        assertEquals("7776000", storedProvider.getConfig().getFirst(Attributes.ROTATION_PERIOD_KEY)); // 90 days in seconds
        assertEquals("2592000", storedProvider.getConfig().getFirst(Attributes.PASSIVE_KEY_EXPIRATION_KEY)); // 30 days in seconds

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    @Test
    public void testKeyRotationDueForRotation() {
        // Create a key provider with automatic rotation enabled and last rotation time in the past
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();
        System.out.println("[DEBUG] Created test provider: " + keyProvider.getName() + ", id=" + keyProviderId);
        
        // Verify it was actually created
        ComponentRepresentation created = realm.admin().components().component(keyProviderId).toRepresentation();
        System.out.println("[DEBUG] Retrieved created provider: " + created.getName() + ", id=" + created.getId() + 
                ", providerId=" + created.getProviderId());

        // Set last rotation time to 91 days ago (past the rotation period of 90 days)
        // Note: lastRotationTime is an internal-only attribute, so we set it directly on the server
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);
        String realmName = realm.getName();
        
        String finalKeyProviderId = keyProviderId;
        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realmModel = session.realms().getRealmByName(realmName);
            org.keycloak.component.ComponentModel provider = realmModel.getComponent(finalKeyProviderId);
            org.keycloak.common.util.MultivaluedHashMap<String, String> config = new org.keycloak.common.util.MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(config);
            realmModel.updateComponent(provider);
        });

        // Get the current active key ID
        String initialActiveKid = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
        
        // Debug: Check providers before rotation
        List<ComponentRepresentation> allComponents = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName());
        System.out.println("[DEBUG] Before rotation: " + allComponents.size() + " total key provider components");
        for (ComponentRepresentation p : allComponents) {
            System.out.println("[DEBUG]   " + p.getName() + ", id=" + p.getId() + ", providerId=" + p.getProviderId());
        }
        
        List<ComponentRepresentation> beforeProviders = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName(), "rsa-generated");
        System.out.println("[DEBUG] Before rotation: " + beforeProviders.size() + " rsa-generated providers");
        for (ComponentRepresentation p : beforeProviders) {
            System.out.println("[DEBUG]   " + p.getName() + ", id=" + p.getId());
        }

        // Manually trigger the rotation task
        runOnServer.run(session -> {
            new AutomaticKeyRotationTask().run(session);
        });
        
        // Debug: Check providers after rotation
        List<ComponentRepresentation> afterProviders = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName(), "rsa-generated");
        System.out.println("[DEBUG] After rotation: " + afterProviders.size() + " providers");
        for (ComponentRepresentation p : afterProviders) {
            System.out.println("[DEBUG]   " + p.getName() + ", id=" + p.getId());
        }

        // Verify a new key was created
        String newActiveKid = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
        assertNotEquals(initialActiveKid, newActiveKid, "Active key should have changed after rotation");

        // Verify the old provider is now passive
        ComponentRepresentation oldProvider = realm.admin().components().component(keyProviderId).toRepresentation();
        assertFalse(Boolean.parseBoolean(oldProvider.getConfig().getFirst(Attributes.ACTIVE_KEY)), 
                "Old key provider should be passive");

        // Verify there's a new active key provider
        List<ComponentRepresentation> allKeyProviders = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName(), "rsa-generated");
        System.out.println("[DEBUG] Found " + allKeyProviders.size() + " rsa-generated providers total:");
        for (ComponentRepresentation p : allKeyProviders) {
            System.out.println("[DEBUG]   Provider: " + p.getName() + ", id=" + p.getId() + 
                    ", active=" + p.getConfig().getFirst(Attributes.ACTIVE_KEY) + 
                    ", enabled=" + p.getConfig().getFirst(Attributes.ENABLED_KEY));
        }
        
        List<ComponentRepresentation> keyProviders = allKeyProviders.stream()
                .filter(p -> {
                    String active = p.getConfig().getFirst(Attributes.ACTIVE_KEY);
                    boolean isActive = active == null || Boolean.parseBoolean(active);
                    System.out.println("[DEBUG]   Filtering " + p.getName() + ": active=" + active + ", isActive=" + isActive);
                    return isActive;
                })
                .collect(Collectors.toList());
        
        assertEquals(1, keyProviders.size(), "Should have exactly one active key provider");
        assertNotEquals(keyProviderId, keyProviders.get(0).getId(), "Active key provider should be a new one");

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
        realm.admin().components().component(keyProviders.get(0).getId()).remove();
    }

    @Test
    public void testKeyRotationNotDueYet() {
        // Create a key provider with automatic rotation enabled and lastRotationTime=1 day ago
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Count the number of rsa-generated providers with auto-rotation enabled before rotation
        List<ComponentRepresentation> beforeProviders = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName())
                .stream()
                .filter(p -> "rsa-generated".equals(p.getProviderId()))
                .filter(p -> keyProvider.getName().equals(p.getName())) // Only check our test provider
                .toList();
        assertEquals(1, beforeProviders.size(), "Should have our test provider");
        
        System.out.println("=== BEFORE PROVIDERS ===");
        System.out.println("Found " + beforeProviders.size() + " providers:");
        for (ComponentRepresentation p : beforeProviders) {
            System.out.println("  Provider: " + p.getName() + " (id=" + p.getId() + ", enabled=" + p.getConfig().getFirst("enabled") + ", priority=" + p.getConfig().getFirst("priority") + ", lastRotationTime=" + p.getConfig().getFirst("lastRotationTime") + ")");
        }
        
        // Manually trigger the rotation task
        runOnServer.run(session -> {
            System.out.println("=== BEFORE ROTATION TASK ===");
            System.out.println("Running AutomaticKeyRotationTask...");
            new AutomaticKeyRotationTask().run(session);
            System.out.println("=== AFTER ROTATION TASK ===");
        });

        // Verify that NO new provider was created (rotation didn't happen)
        List<ComponentRepresentation> afterProviders = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName())
                .stream()
                .filter(p -> "rsa-generated".equals(p.getProviderId()))
                .filter(p -> p.getName().startsWith(keyProvider.getName())) // Check for our provider or rotated versions
                .toList();
        
        System.out.println("=== AFTER PROVIDERS ===");
        System.out.println("Found " + afterProviders.size() + " providers:");
        for (ComponentRepresentation p : afterProviders) {
            System.out.println("  Provider: " + p.getName() + " (id=" + p.getId() + ", enabled=" + p.getConfig().getFirst("enabled") + ", priority=" + p.getConfig().getFirst("priority") + ")");
        }
        
        // Should still be just 1 provider (no rotation happened)
        assertEquals(1, afterProviders.size(), "Should not have created a new provider since rotation is not due");
        assertEquals(keyProviderId, afterProviders.get(0).getId(), "Provider ID should not have changed");

        // Explicitly ensure no rotation replacement (named "rsa-generated-<timestamp>") was created;
        // the name-prefixed filter above would otherwise miss such a provider.
        boolean unexpectedReplacement = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName())
                .stream()
                .anyMatch(p -> p.getName() != null && p.getName().matches("rsa-generated-\\d+"));
        assertFalse(unexpectedReplacement, "No rotation replacement should have been created when rotation is not due");

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    @Test
    public void testPassiveKeyExpiration() {
        // Create a key provider and set it to passive
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Set as passive and set last rotation time to 40 days ago — well beyond the session-derived
        // minimum retention (~33 days for the default offline session idle timeout of 30 days + safety margin)
        // Note: These are internal attributes, so we need to set them directly on the server
        long fortyDaysAgo = Time.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000);
        String realmName = realm.getName();
        String finalKeyProviderId = keyProviderId;
        
        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realmModel = session.realms().getRealmByName(realmName);
            org.keycloak.component.ComponentModel provider = realmModel.getComponent(finalKeyProviderId);
            org.keycloak.common.util.MultivaluedHashMap<String, String> config = new org.keycloak.common.util.MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(fortyDaysAgo));
            provider.setConfig(config);
            realmModel.updateComponent(provider);
        });

        // Manually trigger the rotation task
        runOnServer.run(session -> {
            new AutomaticKeyRotationTask().run(session);
        });

        // Verify the passive key is now disabled
        ComponentRepresentation expiredProvider = realm.admin().components().component(keyProviderId).toRepresentation();
        assertFalse(Boolean.parseBoolean(expiredProvider.getConfig().getFirst(Attributes.ENABLED_KEY)), 
                "Expired passive key should be disabled");

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    @Test
    public void testRotationDisabledByDefault() {
        // Create a key provider without rotation configuration
        ComponentRepresentation keyProvider = createKeyProviderWithoutAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Verify rotation is disabled by default
        ComponentRepresentation storedProvider = realm.admin().components().component(keyProviderId).toRepresentation();
        String autoRotationEnabled = storedProvider.getConfig().getFirst(Attributes.AUTO_ROTATION_ENABLED_KEY);
        assertTrue(autoRotationEnabled == null || "false".equals(autoRotationEnabled), 
                "Auto rotation should be disabled by default");

        // Get the current active key ID
        String initialActiveKid = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);

        // Manually trigger the rotation task
        runOnServer.run(session -> {
            new AutomaticKeyRotationTask().run(session);
        });

        // Verify the key did NOT change (since rotation is disabled)
        String currentActiveKid = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
        assertEquals(initialActiveKid, currentActiveKid, 
                "Active key should not change when rotation is disabled");

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    @Test
    public void testKeyIdChangesAfterRotation() {
        // Create a key provider with automatic rotation enabled
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Set last rotation time to 91 days ago to trigger rotation
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);
        String realmName = realm.getName();
        String finalKeyProviderId = keyProviderId;
        
        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realmModel = session.realms().getRealmByName(realmName);
            org.keycloak.component.ComponentModel provider = realmModel.getComponent(finalKeyProviderId);
            org.keycloak.common.util.MultivaluedHashMap<String, String> config = new org.keycloak.common.util.MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(config);
            realmModel.updateComponent(provider);
        });

        // Get the active key ID (kid) before rotation
        String kidBeforeRotation = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
        System.out.println("=== KEY ID BEFORE ROTATION ===");
        System.out.println("Active RS256 kid: " + kidBeforeRotation);
        
        // Manually trigger the rotation task
        runOnServer.run(session -> {
            new AutomaticKeyRotationTask().run(session);
        });

        // Get the active key ID (kid) after rotation
        String kidAfterRotation = realm.admin().keys().getKeyMetadata().getActive().get(Algorithm.RS256);
        System.out.println("=== KEY ID AFTER ROTATION ===");
        System.out.println("Active RS256 kid: " + kidAfterRotation);
        
        // Verify the kid changed - rotation should create a new key with a different kid
        assertNotEquals(kidBeforeRotation, kidAfterRotation, 
                "Key ID (kid) should change after rotation - old and new keys should have different identifiers");
        
        // Verify both kids are not null or empty
        assertTrue(kidBeforeRotation != null && !kidBeforeRotation.isEmpty(), 
                "Key ID before rotation should not be null or empty");
        assertTrue(kidAfterRotation != null && !kidAfterRotation.isEmpty(), 
                "Key ID after rotation should not be null or empty");

        // Cleanup - remove all test providers
        List<ComponentRepresentation> testProviders = realm.admin().components()
                .query(realm.getId(), KeyProvider.class.getName())
                .stream()
                .filter(p -> p.getName().startsWith("test-auto-rotation-"))
                .collect(Collectors.toList());
        
        for (ComponentRepresentation provider : testProviders) {
            realm.admin().components().component(provider.getId()).remove();
        }
    }

    /**
     * Test that disabled keys are automatically deleted after grace period when auto-delete is enabled.
     */
    @Test
    public void testAutomaticKeyDeletion() {
        String realmName = realm.getName();

        // Create the provider in its own committed transaction. The scheduled task now runs each
        // action in a separate transaction, so the provider must be committed before it runs.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);

            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-auto-deletion-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "false"); // Passive key
            config.putSingle(Attributes.ENABLED_KEY, "false"); // Already disabled
            config.putSingle(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY, "true");
            config.putSingle(Attributes.DELETION_GRACE_PERIOD_KEY, "1"); // 1 second grace period
            // Set disabledTime to 2 seconds ago (past grace period)
            long twoSecondsAgo = Time.currentTimeMillis() - 2000;
            config.putSingle(Attributes.DISABLED_TIME_KEY, String.valueOf(twoSecondsAgo));
            keyProvider.setConfig(config);

            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Run the rotation task
        runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

        // Verify the provider was deleted
        boolean stillExists = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            return realmModel.getComponentsStream(realmModel.getId(), KeyProvider.class.getName())
                    .anyMatch(c -> c.getId().equals(providerId));
        }, Boolean.class);

        assertFalse(stillExists, "Provider should have been deleted but still exists: " + providerId);
    }

    /**
     * Test that disabled keys are NOT deleted when grace period hasn't elapsed.
     */
    @Test
    public void testKeyDeletionRespectGracePeriod() {
        String realmName = realm.getName();

        // Create the provider in its own committed transaction.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);

            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-grace-period-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.ENABLED_KEY, "false");
            config.putSingle(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY, "true");
            config.putSingle(Attributes.DELETION_GRACE_PERIOD_KEY, "3600"); // 1 hour grace period
            // Set disabledTime to just now (within grace period)
            config.putSingle(Attributes.DISABLED_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
            keyProvider.setConfig(config);

            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Run the rotation task
        runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

        // Verify the provider still exists (not deleted yet) and clean it up
        boolean stillExists = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            if (provider == null) {
                return false;
            }
            realmModel.removeComponent(provider);
            return true;
        }, Boolean.class);

        assertTrue(stillExists, "Provider should not have been deleted yet");
    }

    /**
     * Test that disabled keys are NOT deleted when auto-delete is disabled.
     */
    @Test
    public void testKeyDeletionDisabled() {
        String realmName = realm.getName();

        // Create the provider in its own committed transaction.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);

            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-no-deletion-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.ENABLED_KEY, "false");
            config.putSingle(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY, "false"); // Deletion disabled
            config.putSingle(Attributes.DELETION_GRACE_PERIOD_KEY, "1");
            // Set disabledTime to 2 seconds ago (past grace period)
            long twoSecondsAgo = Time.currentTimeMillis() - 2000;
            config.putSingle(Attributes.DISABLED_TIME_KEY, String.valueOf(twoSecondsAgo));
            keyProvider.setConfig(config);

            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Run the rotation task
        runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

        // Verify the provider still exists (not deleted because auto-delete is false) and clean it up
        boolean stillExists = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            if (provider == null) {
                return false;
            }
            realmModel.removeComponent(provider);
            return true;
        }, Boolean.class);

        assertTrue(stillExists, "Provider should not have been deleted when auto-delete is disabled");
    }

    /**
     * Test that passive key retention respects realm session timeout settings.
     * A passive key should NOT be expired if the configured expiration is shorter than
     * the maximum session lifespan in the realm (e.g. offlineSessionIdleTimeout = 30 days).
     *
     * Set passiveKeyExpiration=1 second, make the key passive 5 seconds ago, run the task.
     * The task should enforce a floor derived from realm session timeouts and keep the key enabled.
     */
    @Test
    public void testPassiveKeyRetentionRespectsSessionTimeouts() {
        String realmName = realm.getName();

        // Create a key provider with auto-rotation enabled
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Set the provider as passive with last rotation time 5 seconds ago
        // Configured passiveKeyExpiration = 1s, but realm session timeouts (e.g. offlineSessionIdleTimeout = 30 days)
        // should enforce a much longer minimum retention, preventing premature expiration
        long fiveSecondsAgo = Time.currentTimeMillis() - 5000;
        String finalKeyProviderId = keyProviderId;

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);

            // Set the provider to passive with a very short configured expiration
            ComponentModel provider = realmModel.getComponent(finalKeyProviderId);
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(fiveSecondsAgo));
            config.putSingle(Attributes.PASSIVE_KEY_EXPIRATION_KEY, "1"); // 1 second — deliberately too short
            provider.setConfig(config);
            realmModel.updateComponent(provider);
        });

        // Run the rotation task — the session-aware minimum should prevent expiration
        runOnServer.run(session -> {
            new AutomaticKeyRotationTask().run(session);
        });

        // Verify the key is still enabled — session-aware minimum should prevent premature expiration
        ComponentRepresentation provider = realm.admin().components().component(keyProviderId).toRepresentation();
        String enabledStr = provider.getConfig().getFirst(Attributes.ENABLED_KEY);
        boolean isStillEnabled = enabledStr == null || !"false".equalsIgnoreCase(enabledStr);
        assertTrue(isStillEnabled,
                "Passive key should NOT be expired because session-derived minimum retention exceeds the configured 1s expiration");

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    /**
     * Test that the rotation task correctly derives the passive key retention floor from
     * realm session timeout settings.
     *
     * Use the Admin API to read the realm's offlineSessionIdleTimeout (default 30 days = 2592000s),
     * then verify that a passive key with a configured expiration equal to that value minus 1 second
     * is NOT expired (because the derived floor should be at least offlineSessionIdleTimeout + safety margin).
     */
    @Test
    public void testPassiveRetentionDerivedFromSessionTimeouts() {
        String realmName = realm.getName();

        // Read the realm's offline session idle timeout via the admin API
        int offlineIdleTimeout = realm.admin().toRepresentation().getOfflineSessionIdleTimeout();
        System.out.println("[DEBUG] offlineSessionIdleTimeout = " + offlineIdleTimeout + " seconds");
        assertTrue(offlineIdleTimeout > 0, "offlineSessionIdleTimeout should be > 0");

        // Create a provider and mark it passive with expiration = offlineIdleTimeout - 1
        // (this is shorter than the floor which includes a safety margin)
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Set last rotation time far enough ago to exceed the configured expiration, but NOT the derived floor
        // The derived floor = max(all session timeouts) + safety margin >= offlineIdleTimeout + 1 hour
        // So set lastRotation to (offlineIdleTimeout) seconds ago — this exceeds (offlineIdleTimeout - 1)
        // but should NOT exceed the derived floor
        long lastRotationTime = Time.currentTimeMillis() - ((long) offlineIdleTimeout * 1000L);
        String shortExpiration = String.valueOf(offlineIdleTimeout - 1);
        String finalKeyProviderId = keyProviderId;

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(finalKeyProviderId);
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(lastRotationTime));
            config.putSingle(Attributes.PASSIVE_KEY_EXPIRATION_KEY, shortExpiration);
            provider.setConfig(config);
            realmModel.updateComponent(provider);
        });

        // Run the rotation task
        runOnServer.run(session -> {
            new AutomaticKeyRotationTask().run(session);
        });

        // The key should still be enabled because the derived floor (offlineIdleTimeout + safety margin)
        // is larger than both the configured expiration and the time since rotation
        ComponentRepresentation providerAfter = realm.admin().components().component(keyProviderId).toRepresentation();
        String enabledStr = providerAfter.getConfig().getFirst(Attributes.ENABLED_KEY);
        boolean isStillEnabled = enabledStr == null || !"false".equalsIgnoreCase(enabledStr);
        assertTrue(isStillEnabled,
                "Passive key should NOT be expired: the session-derived floor (offlineSessionIdleTimeout + safety margin) " +
                "should be > configured expiration (" + shortExpiration + "s) and > time since rotation (" + offlineIdleTimeout + "s)");

        System.out.println("[DEBUG] Test passed: key still enabled after " + offlineIdleTimeout +
                "s with configured expiration " + shortExpiration + "s — session-derived floor enforced");

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    /**
     * Rotation must preserve the HMAC secret size. The replacement key is generated by the provider
     * factory, so if the {@code secretSize} generation parameter is dropped the new key silently
     * reverts to the factory default (128 bytes). Regression test for lost non-RSA generation
     * parameters.
     */
    @Test
    public void testRotationPreservesHmacSecretSize() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String nonDefaultSecretSize = "64"; // factory default is 128
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        // Create server-side so internal attributes (e.g. lastRotationTime) are persisted.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-hmac-rotation-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("hmac-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(Attributes.SECRET_SIZE_KEY, nonDefaultSecretSize);
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Set lastRotationTime in a separate transaction so rotation is due (internal attribute).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            // Trigger rotation
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            // The rotated-in replacement is named "hmac-generated-<timestamp>"
            ComponentRepresentation rotated = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().matches("hmac-generated-\\d+"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(rotated, "Rotation should have created a replacement hmac provider");
            assertEquals(nonDefaultSecretSize, rotated.getConfig().getFirst(Attributes.SECRET_SIZE_KEY),
                    "Rotation must preserve the configured HMAC secret size");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().startsWith("test-hmac-rotation-") || c.getName().matches("hmac-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * Rotation must preserve the ECDSA elliptic curve. If the curve generation parameter is dropped
     * the replacement key silently reverts to the factory default (P-256), so a P-521 key would be
     * downgraded on its first rotation. Regression test for lost EC generation parameters.
     */
    @Test
    public void testRotationPreservesEcdsaCurve() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String ecdsaCurveKey = "ecdsaEllipticCurveKey";
        String nonDefaultCurve = "P-521"; // factory default is P-256
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        // Create server-side so internal attributes (e.g. lastRotationTime) are persisted.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-ecdsa-rotation-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("ecdsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(ecdsaCurveKey, nonDefaultCurve);
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Set lastRotationTime in a separate transaction so rotation is due (internal attribute).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            ComponentRepresentation rotated = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().matches("ecdsa-generated-\\d+"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(rotated, "Rotation should have created a replacement ecdsa provider");
            assertEquals(nonDefaultCurve, rotated.getConfig().getFirst(ecdsaCurveKey),
                    "Rotation must preserve the configured ECDSA elliptic curve");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().startsWith("test-ecdsa-rotation-") || c.getName().matches("ecdsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * A negative {@code deletionGracePeriod} must never trigger deletion. A configuration typo
     * (negative value) would otherwise make the deletion predicate immediately true and permanently
     * remove key material on the next task run. Regression test for accidental key-material loss.
     */
    @Test
    public void testNegativeDeletionGracePeriodDoesNotDelete() {
        String realmName = realm.getName();

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);

            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-neg-grace-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.ENABLED_KEY, "false");
            config.putSingle(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY, "true");
            config.putSingle(Attributes.DELETION_GRACE_PERIOD_KEY, "-1"); // invalid: must never delete
            config.putSingle(Attributes.DISABLED_TIME_KEY, String.valueOf(Time.currentTimeMillis() - 2000L));
            keyProvider.setConfig(config);

            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

        boolean stillExists = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            if (provider == null) {
                return false;
            }
            realmModel.removeComponent(provider);
            return true;
        }, Boolean.class);

        assertTrue(stillExists, "A negative deletionGracePeriod must never trigger deletion of key material");
    }

    /**
     * A zero (or otherwise non-positive) {@code rotationPeriod} must not cause the key to rotate on
     * every scheduled run. Regression test for the unvalidated-duration rotate-every-run behavior.
     */
    @Test
    public void testZeroRotationPeriodDoesNotRotateEveryRun() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        long oneDayAgo = Time.currentTimeMillis() - (24L * 60 * 60 * 1000);

        // Create server-side so internal attributes (e.g. lastRotationTime) are persisted.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-zero-period-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "0"); // invalid: must not rotate every run
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Set lastRotationTime one day ago in a separate transaction (internal attribute).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(oneDayAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            // No replacement provider (named "rsa-generated-<timestamp>") should have been created
            boolean rotated = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .anyMatch(c -> c.getName() != null && c.getName().matches("rsa-generated-\\d+"));

            assertFalse(rotated, "A zero/invalid rotationPeriod must not cause rotation on every run");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().startsWith("test-zero-period-") || c.getName().matches("rsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * A provider that is administratively disabled (enabled=false) must not be rotated, even if it
     * is still marked active. Rotating it would create an enabled replacement and silently
     * re-enable key material an administrator turned off. Regression test.
     */
    @Test
    public void testDisabledProviderIsNotRotated() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        // Create server-side so internal attributes (e.g. lastRotationTime) are persisted.
        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-disabled-rotation-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "false"); // administratively disabled
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Set lastRotationTime in a separate transaction so rotation would otherwise be due.
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            boolean rotated = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .anyMatch(c -> c.getName() != null && c.getName().matches("rsa-generated-\\d+"));

            assertFalse(rotated, "A disabled (enabled=false) provider must not be rotated");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().startsWith("test-disabled-rotation-") || c.getName().matches("rsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * The passive-key retention floor must also account for signed tokens not bounded by session
     * timeouts (here, the user action-token lifespan). A key must not be expired while such a token
     * could still need verification. Regression test for issue #5.
     */
    @Test
    public void testPassiveRetentionIncludesActionTokenLifespan() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-actiontoken-" + System.currentTimeMillis();
        int sixtyDays = 60 * 24 * 60 * 60;
        long fortyDaysAgo = Time.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000);

        int originalActionTokenLifespan = runOnServer.fetch(session ->
                session.realms().getRealmByName(realmName).getActionTokenGeneratedByUserLifespan(), Integer.class);

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            realmModel.setActionTokenGeneratedByUserLifespan(sixtyDays);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Make the key passive with a tiny configured expiration and a lastRotationTime 40 days ago,
        // all via updateComponent so the internal attributes persist (they are dropped on create).
        // 40 days is beyond the session-only floor (~33d) but within the action-token floor (~66d).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.ACTIVE_KEY, "false");
            cfg.putSingle(Attributes.ENABLED_KEY, "true");
            cfg.putSingle(Attributes.PASSIVE_KEY_EXPIRATION_KEY, "1"); // tiny; the derived floor should dominate
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(fortyDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            boolean stillEnabled = runOnServer.fetch(session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                ComponentModel provider = realmModel.getComponent(providerId);
                String enabled = provider.get(Attributes.ENABLED_KEY);
                return enabled == null || !"false".equalsIgnoreCase(enabled);
            }, Boolean.class);

            assertTrue(stillEnabled,
                    "Passive key must not be expired while the action-token lifespan floor still covers it");
        } finally {
            runOnServer.run(session -> session.realms().getRealmByName(realmName)
                    .setActionTokenGeneratedByUserLifespan(originalActionTokenLifespan));
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().equals(providerName))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * When an active auto-rotation provider is deleted administratively, its "last rotated" gauge
     * and meter must be removed on the next scan instead of leaking forever. Regression test for
     * issue #8.
     */
    @Test
    public void testStaleGaugeRemovedWhenProviderDeleted() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-gauge-" + System.currentTimeMillis();
        long nowMillis = Time.currentTimeMillis();

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Recent lastRotationTime so the provider stays active (a gauge is published, no rotation).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(nowMillis));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            // First run registers the gauge.
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));
            boolean gaugePresent = runOnServer.fetch(session ->
                    io.micrometer.core.instrument.Metrics.globalRegistry.find("keycloak.key.last_rotated_seconds")
                            .tag("name", providerName).gauge() != null, Boolean.class);
            assertTrue(gaugePresent, "Gauge should be registered for the active auto-rotation provider");

            // Delete the provider administratively, then run again so reconciliation removes the gauge.
            realm.admin().components().component(providerId).remove();
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            boolean gaugeStillPresent = runOnServer.fetch(session ->
                    io.micrometer.core.instrument.Metrics.globalRegistry.find("keycloak.key.last_rotated_seconds")
                            .tag("name", providerName).gauge() != null, Boolean.class);
            assertFalse(gaugeStillPresent, "Gauge/meter must be removed after the provider is deleted");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().equals(providerName))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * A rotation must fire an admin event through the normal dispatch path (persisted to the event
     * store and dispatched to the realm's configured/global event listeners by the same code).
     * Verifies fireAdminEvent end-to-end after the dispatch rewrite (issue #6).
     */
    @Test
    public void testRotationFiresAdminEvent() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-adminevent-" + System.currentTimeMillis();
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Configure rotation via updateComponent so the attributes persist (create-time config is
        // dropped for generated providers), with lastRotationTime 91 days ago so rotation is due.
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.ACTIVE_KEY, "true");
            cfg.putSingle(Attributes.ENABLED_KEY, "true");
            cfg.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            cfg.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            // The rotation creates a new key provider and fires a COMPONENT CREATE admin event.
            AdminEventAssertion.assertSuccess(adminEvents.poll())
                    .operationType(OperationType.CREATE)
                    .resourceType(ResourceType.COMPONENT);
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().equals(providerName) || c.getName().matches("rsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * Rotation must preserve the ECDH key-agreement algorithm. If {@code ecdhAlgorithm} is dropped
     * from the copied generation parameters, an ECDH provider configured for a non-default
     * algorithm silently reverts to the factory default (ECDH-ES) on its first rotation, changing
     * the content-encryption behavior. Regression test for review item C.
     */
    @Test
    public void testRotationPreservesEcdhAlgorithm() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String ecdhAlgorithmKey = "ecdhAlgorithm";
        String nonDefaultAlgorithm = "ECDH-ES+A256KW"; // factory default is ECDH-ES
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-ecdh-rotation-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("ecdh-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(ecdhAlgorithmKey, nonDefaultAlgorithm);
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Set lastRotationTime in a separate transaction so rotation is due (internal attribute).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            ComponentRepresentation rotated = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().matches("ecdh-generated-\\d+"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(rotated, "Rotation should have created a replacement ecdh provider");
            assertEquals(nonDefaultAlgorithm, rotated.getConfig().getFirst(ecdhAlgorithmKey),
                    "Rotation must preserve the configured ECDH algorithm");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().startsWith("test-ecdh-rotation-") || c.getName().matches("ecdh-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * A provider that is disabled with auto-delete enabled but has no {@code disabledTime} (the
     * normal state after an administrator disables it manually, or enables auto-delete on an
     * already-disabled provider) must have its disable timestamp initialized so the deletion grace
     * period can start. Without this the provider is retained forever. Regression test for review
     * item B.
     */
    @Test
    public void testDisabledProviderWithoutDisabledTimeGetsTimestampInitialized() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-nodistime-" + System.currentTimeMillis();

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Disable it with auto-delete enabled and a long grace period, but no disabledTime, via
        // updateComponent so the internal attributes persist (create-time config is dropped).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.ACTIVE_KEY, "false");
            cfg.putSingle(Attributes.ENABLED_KEY, "false");
            cfg.putSingle(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY, "true");
            cfg.putSingle(Attributes.DELETION_GRACE_PERIOD_KEY, "86400"); // 1 day: must NOT delete yet
            cfg.remove(Attributes.DISABLED_TIME_KEY); // ensure unset
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            String disabledTime = runOnServer.fetch(session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                ComponentModel provider = realmModel.getComponent(providerId);
                return provider == null ? null : provider.get(Attributes.DISABLED_TIME_KEY);
            }, String.class);

            assertNotNull(disabledTime,
                    "A disabled auto-delete provider without disabledTime must have one initialized");
            assertFalse(disabledTime.trim().isEmpty(),
                    "The initialized disabledTime must be a non-empty timestamp");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().equals(providerName))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * The passive-key retention floor must also account for per-client token/session lifespan
     * overrides that exceed the realm-level defaults. A client configuring a long
     * {@code access.token.lifespan} could otherwise have its still-valid tokens rejected after the
     * signing key is expired. Regression test for review item A.
     */
    @Test
    public void testPassiveRetentionIncludesClientAccessTokenLifespan() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-clienttoken-" + System.currentTimeMillis();
        String clientId = "test-clienttoken-client-" + System.currentTimeMillis();
        int sixtyDays = 60 * 24 * 60 * 60;
        long fortyDaysAgo = Time.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000);

        // A client whose access-token lifespan (60d) exceeds the realm session floor (~33d).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ClientModel client = realmModel.addClient(clientId);
            client.setAttribute("access.token.lifespan", String.valueOf(sixtyDays));
        });

        String providerId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Make the key passive with a tiny configured expiration and lastRotationTime 40 days ago.
        // 40d is beyond the session-only floor (~33d) but within the client access-token floor (~66d).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(providerId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.ACTIVE_KEY, "false");
            cfg.putSingle(Attributes.ENABLED_KEY, "true");
            cfg.putSingle(Attributes.PASSIVE_KEY_EXPIRATION_KEY, "1"); // tiny; the derived floor should dominate
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(fortyDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            boolean stillEnabled = runOnServer.fetch(session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                ComponentModel provider = realmModel.getComponent(providerId);
                String enabled = provider.get(Attributes.ENABLED_KEY);
                return enabled == null || !"false".equalsIgnoreCase(enabled);
            }, Boolean.class);

            assertTrue(stillEnabled,
                    "Passive key must not be expired while a per-client access-token lifespan still covers it");
        } finally {
            runOnServer.run(session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                ClientModel client = realmModel.getClientByClientId(clientId);
                if (client != null) {
                    realmModel.removeClient(client.getId());
                }
            });
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().equals(providerName))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * When a pre-activation period is configured, rotation must publish the replacement key as a
     * passive (but enabled) key without demoting the currently active key, so clients that cache
     * the JWKS can pick up the new public key before it is used for signing. The current key must
     * stay active and be marked as having a pending pre-activation so it is not rotated again.
     */
    @Test
    public void testPreActivationPublishesPassiveKeyWithoutDemotingCurrent() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-preact-" + System.currentTimeMillis();
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        String oldId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days
            config.putSingle(Attributes.PRE_ACTIVATION_PERIOD_KEY, "7776000"); // 90 days: stays pending
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        // Rotation due: set lastRotationTime in a separate transaction (internal attribute).
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(oldId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            ComponentRepresentation pending = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().matches("rsa-generated-\\d+"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(pending, "Pre-activation should have created a replacement provider");
            String pendingId = pending.getId();

            // The replacement key is passive (not signing) but enabled (published in the JWKS).
            String pendingActive = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(pendingId).get(Attributes.ACTIVE_KEY), String.class);
            String pendingEnabled = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(pendingId).get(Attributes.ENABLED_KEY), String.class);
            String activationTime = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(pendingId).get(Attributes.ACTIVATION_TIME_KEY), String.class);
            String predecessor = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(pendingId).get(Attributes.PRE_ACTIVATION_PREDECESSOR_KEY), String.class);

            assertEquals("false", pendingActive, "Pre-activation key must be passive (not signing) during the window");
            assertNotEquals("false", pendingEnabled, "Pre-activation key must be enabled/published in the JWKS");
            assertNotNull(activationTime, "Pre-activation key must carry a future activation time");
            assertEquals(oldId, predecessor, "Pre-activation key must reference its predecessor");

            // The current key keeps signing and is flagged so it is not rotated again while pending.
            String oldActive = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(oldId).get(Attributes.ACTIVE_KEY), String.class);
            String oldPending = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(oldId).get(Attributes.PRE_ACTIVATION_PENDING_KEY), String.class);

            assertNotEquals("false", oldActive, "The current key must stay active during the pre-activation window");
            assertEquals("true", oldPending, "The current key must be marked as having a pending pre-activation");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().equals(providerName) || c.getName().matches("rsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * Once the activation time of a pre-activation key has passed, a subsequent run must promote it
     * to the active signing key and demote its predecessor to passive.
     */
    @Test
    public void testPreActivationKeyPromotedAfterActivationTime() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-preact-" + System.currentTimeMillis();
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        String oldId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000");
            config.putSingle(Attributes.PRE_ACTIVATION_PERIOD_KEY, "7776000");
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(oldId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            // First run: publish the passive pre-activation key.
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            String pendingId = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().matches("rsa-generated-\\d+"))
                    .map(ComponentRepresentation::getId)
                    .findFirst()
                    .orElse(null);
            assertNotNull(pendingId, "Pre-activation should have created a replacement provider");

            // Force the activation time into the past so promotion is due on the next run.
            runOnServer.run(session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                ComponentModel provider = realmModel.getComponent(pendingId);
                MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
                cfg.putSingle(Attributes.ACTIVATION_TIME_KEY, String.valueOf(Time.currentTimeMillis() - 1000));
                provider.setConfig(cfg);
                realmModel.updateComponent(provider);
            });

            // Second run: promote the pending key and demote the predecessor.
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            String promotedActive = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(pendingId).get(Attributes.ACTIVE_KEY), String.class);
            String promotedActivationTime = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(pendingId).get(Attributes.ACTIVATION_TIME_KEY), String.class);
            String oldActiveAfter = runOnServer.fetch(session -> session.realms().getRealmByName(realmName)
                    .getComponent(oldId).get(Attributes.ACTIVE_KEY), String.class);

            assertNotEquals("false", promotedActive, "The promoted pre-activation key must be active for signing");
            assertTrue(promotedActivationTime == null || promotedActivationTime.trim().isEmpty(),
                    "The activation marker must be cleared once the key is promoted");
            assertEquals("false", oldActiveAfter, "The predecessor key must be demoted to passive after promotion");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().equals(providerName) || c.getName().matches("rsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    /**
     * While a pre-activation is pending, a further run must not rotate the still-active predecessor
     * again — otherwise every run would spawn another passive key. Exactly one replacement key must
     * exist during the pre-activation window.
     */
    @Test
    public void testPreActivationDoesNotCreateSecondKeyWhilePending() {
        String realmName = realm.getName();
        String realmId = realm.getId();
        String providerName = "test-preact-" + System.currentTimeMillis();
        long ninetyOneDaysAgo = Time.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000);

        String oldId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName(providerName);
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realmModel.getId());
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "true");
            config.putSingle(Attributes.ENABLED_KEY, "true");
            config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
            config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000");
            config.putSingle(Attributes.PRE_ACTIVATION_PERIOD_KEY, "7776000");
            keyProvider.setConfig(config);
            return realmModel.addComponentModel(keyProvider).getId();
        }, String.class);

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            ComponentModel provider = realmModel.getComponent(oldId);
            MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(provider.getConfig());
            cfg.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(ninetyOneDaysAgo));
            provider.setConfig(cfg);
            realmModel.updateComponent(provider);
        });

        try {
            // Run the task twice within the pre-activation window.
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));
            runOnServer.run(session -> new AutomaticKeyRotationTask().run(session));

            long replacementCount = realm.admin().components()
                    .query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null && c.getName().matches("rsa-generated-\\d+"))
                    .count();

            assertEquals(1L, replacementCount,
                    "Only one pre-activation replacement key must exist while a promotion is pending");
        } finally {
            realm.admin().components().query(realmId, KeyProvider.class.getName())
                    .stream()
                    .filter(c -> c.getName() != null &&
                            (c.getName().equals(providerName) || c.getName().matches("rsa-generated-\\d+")))
                    .forEach(c -> realm.admin().components().component(c.getId()).remove());
        }
    }

    private ComponentRepresentation createKeyProviderWithAutoRotation() {
        String realmId = realm.getId();
        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName("test-auto-rotation-" + System.currentTimeMillis());
        keyProvider.setProviderType(KeyProvider.class.getName());
        keyProvider.setProviderId("rsa-generated");
        keyProvider.setParentId(realmId);
        
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", "200");
        config.putSingle(Attributes.ACTIVE_KEY, "true");
        config.putSingle(Attributes.ENABLED_KEY, "true");
        config.putSingle(Attributes.AUTO_ROTATION_ENABLED_KEY, "true");
        config.putSingle(Attributes.ROTATION_PERIOD_KEY, "7776000"); // 90 days in seconds
        config.putSingle(Attributes.PASSIVE_KEY_EXPIRATION_KEY, "2592000"); // 30 days in seconds
        // Set lastRotationTime to 1 day ago (internal attribute must be set during creation)
        long oneDayAgo = Time.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000);
        config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(oneDayAgo));
        keyProvider.setConfig(config);

        Response response = realm.admin().components().add(keyProvider);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        keyProvider.setId(id);
        return keyProvider;
    }

    private ComponentRepresentation createKeyProviderWithoutAutoRotation() {
        String realmId = realm.getId();
        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName("test-no-rotation-" + System.currentTimeMillis());
        keyProvider.setProviderType(KeyProvider.class.getName());
        keyProvider.setProviderId("rsa-generated");
        keyProvider.setParentId(realmId);
        
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", "200");
        config.putSingle(Attributes.ACTIVE_KEY, "true");
        config.putSingle(Attributes.ENABLED_KEY, "true");
        keyProvider.setConfig(config);

        Response response = realm.admin().components().add(keyProvider);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        keyProvider.setId(id);
        return keyProvider;
    }

    public static class ServerConfigWithMetrics implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("metrics-enabled", "true");
        }
    }
}
