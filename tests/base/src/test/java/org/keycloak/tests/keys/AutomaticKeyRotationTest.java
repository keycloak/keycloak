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
import org.keycloak.keys.Attributes;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.services.scheduled.AutomaticKeyRotationTask;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.BasicRealmWithUserConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for automatic key rotation feature.
 * 
 * @author <a href="mailto:volck@redhat.com">Volck</a>
 */
@KeycloakIntegrationTest
public class AutomaticKeyRotationTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void cleanupOldTestProviders() {
        // Remove any leftover test providers from previous test runs
        // This prevents interference from old providers with auto-rotation enabled
        realm.admin().components().query(realm.getId(), KeyProvider.class.getName())
                .stream()
                .filter(c -> c.getName() != null && 
                        (c.getName().startsWith("test-auto-rotation-") || 
                         c.getName().startsWith("test-no-rotation-")))
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

        // Cleanup
        realm.admin().components().component(keyProviderId).remove();
    }

    @Test
    public void testPassiveKeyExpiration() {
        // Create a key provider and set it to passive
        ComponentRepresentation keyProvider = createKeyProviderWithAutoRotation();
        String keyProviderId = keyProvider.getId();

        // Set as passive and set last rotation time to 31 days ago (past the expiration period of 30 days)
        // Note: These are internal attributes, so we need to set them directly on the server
        long thirtyOneDaysAgo = Time.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000);
        String realmName = realm.getName();
        String finalKeyProviderId = keyProviderId;
        
        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realmModel = session.realms().getRealmByName(realmName);
            org.keycloak.component.ComponentModel provider = realmModel.getComponent(finalKeyProviderId);
            org.keycloak.common.util.MultivaluedHashMap<String, String> config = new org.keycloak.common.util.MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.LAST_ROTATION_TIME_KEY, String.valueOf(thirtyOneDaysAgo));
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
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            
            // Create a key provider with auto-deletion enabled
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-auto-deletion-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realm.getId());
            
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

            ComponentModel created = realm.addComponentModel(keyProvider);
            String providerId = created.getId();

            // Verify provider was created
            ComponentModel retrieved = realm.getComponent(providerId);
            assertEquals("false", retrieved.get(Attributes.ENABLED_KEY));
            assertEquals("true", retrieved.get(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY));

            // Run the rotation task
            AutomaticKeyRotationTask task = new AutomaticKeyRotationTask();
            task.run(session);

            // Verify the provider was deleted
            ComponentModel afterTask = realm.getComponent(providerId);
            if (afterTask != null) {
                throw new AssertionError("Provider should have been deleted but still exists: " + afterTask.getName());
            }
        });
    }

    /**
     * Test that disabled keys are NOT deleted when grace period hasn't elapsed.
     */
    @Test
    public void testKeyDeletionRespectGracePeriod() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            
            // Create a key provider with auto-deletion enabled but within grace period
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-grace-period-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realm.getId());
            
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle("priority", "200");
            config.putSingle(Attributes.ACTIVE_KEY, "false");
            config.putSingle(Attributes.ENABLED_KEY, "false");
            config.putSingle(Attributes.AUTO_DELETE_DISABLED_KEYS_KEY, "true");
            config.putSingle(Attributes.DELETION_GRACE_PERIOD_KEY, "3600"); // 1 hour grace period
            // Set disabledTime to just now (within grace period)
            config.putSingle(Attributes.DISABLED_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
            keyProvider.setConfig(config);

            ComponentModel created = realm.addComponentModel(keyProvider);
            String providerId = created.getId();

            // Run the rotation task
            AutomaticKeyRotationTask task = new AutomaticKeyRotationTask();
            task.run(session);

            // Verify the provider still exists (not deleted yet)
            ComponentModel stillExists = realm.getComponent(providerId);
            if (stillExists == null) {
                throw new AssertionError("Provider should not have been deleted yet");
            }
            assertEquals(providerId, stillExists.getId());
            assertEquals("false", stillExists.get(Attributes.ENABLED_KEY));

            // Clean up
            realm.removeComponent(stillExists);
        });
    }

    /**
     * Test that disabled keys are NOT deleted when auto-delete is disabled.
     */
    @Test
    public void testKeyDeletionDisabled() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            
            // Create a key provider without auto-deletion
            ComponentModel keyProvider = new ComponentModel();
            keyProvider.setName("test-no-deletion-" + System.currentTimeMillis());
            keyProvider.setProviderType(KeyProvider.class.getName());
            keyProvider.setProviderId("rsa-generated");
            keyProvider.setParentId(realm.getId());
            
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

            ComponentModel created = realm.addComponentModel(keyProvider);
            String providerId = created.getId();

            // Run the rotation task
            AutomaticKeyRotationTask task = new AutomaticKeyRotationTask();
            task.run(session);

            // Verify the provider still exists (not deleted because auto-delete is false)
            ComponentModel stillExists = realm.getComponent(providerId);
            if (stillExists == null) {
                throw new AssertionError("Provider should not have been deleted when auto-delete is disabled");
            }
            assertEquals(providerId, stillExists.getId());
            assertEquals("false", stillExists.get(Attributes.ENABLED_KEY));

            // Clean up
            realm.removeComponent(stillExists);
        });
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
}
