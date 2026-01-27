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

package org.keycloak.services.scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import org.keycloak.Config;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import io.micrometer.core.instrument.Tag;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;

import org.jboss.logging.Logger;

/**
 * Scheduled task for automatic key rotation.
 * This task checks all key providers across all realms and rotates keys that are due for rotation.
 * 
 * @author <a href="mailto:volck@redhat.com">Volck</a>
 */
public class AutomaticKeyRotationTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(AutomaticKeyRotationTask.class);

    public static final String TASK_NAME = "AutomaticKeyRotation";

    private static final String KEY_ROTATION_METER_NAME = "keycloak.key.rotation";
    private static final Meter.MeterProvider<Counter> rotationMeterProvider = Counter.builder(KEY_ROTATION_METER_NAME)
            .description("Key rotation operations")
            .baseUnit("operations")
            .withRegistry(Metrics.globalRegistry);

    private static final String KEY_LAST_ROTATED_METER_NAME = "keycloak.key.last_rotated_seconds";

    // Store gauge values for each provider so they can be tracked by Micrometer
    private static final Map<String, AtomicLong> rotationGaugeValues = new ConcurrentHashMap<>();

    // Attribute keys from org.keycloak.keys.Attributes (duplicated here to avoid module dependency)
    private static final String AUTO_ROTATION_ENABLED_KEY = "autoRotationEnabled";
    private static final String ROTATION_PERIOD_KEY = "rotationPeriod";
    private static final String LAST_ROTATION_TIME_KEY = "lastRotationTime";
    private static final String ACTIVE_KEY = "active";
    private static final String ENABLED_KEY = "enabled";
    private static final String KEY_USE = "keyUse";
    private static final String ALGORITHM_KEY = "algorithm";
    private static final String KEY_SIZE_KEY = "keySize";
    private static final String PASSIVE_KEY_EXPIRATION_KEY = "passiveKeyExpiration";
    private static final String AUTO_DELETE_DISABLED_KEYS_KEY = "autoDeleteDisabledKeys";
    private static final String DELETION_GRACE_PERIOD_KEY = "deletionGracePeriod";
    private static final String DISABLED_TIME_KEY = "disabledTime";

    @Override
    public void run(KeycloakSession session) {
        long startTimeMillis = Time.currentTimeMillis();

        int rotatedKeysCount = 0;
        int expiredKeysCount = 0;
        int deletedKeysCount = 0;
        int autoRotationEnabledCount = 0;

        // Process all realms
        List<RealmModel> realms = session.realms().getRealmsStream().collect(Collectors.toList());
        for (RealmModel realm : realms) {
            try {
                // Get all key providers for the realm
                List<ComponentModel> keyProviders = realm.getComponentsStream(realm.getId(), KeyProvider.class.getName())
                        .collect(Collectors.toList());

                for (ComponentModel provider : keyProviders) {
                    try {
                        // Check if automatic rotation is enabled for this provider
                        String autoRotStr = provider.get(AUTO_ROTATION_ENABLED_KEY);
                        boolean autoRotationEnabled = "true".equalsIgnoreCase(autoRotStr);
                        
                        if (autoRotationEnabled) {
                            autoRotationEnabledCount++;
                            // Update metric for last rotation timestamp
                            updateLastRotationMetric(provider, realm);
                            
                            // Check if it's time to rotate
                            if (shouldRotateKey(provider, realm)) {
                                rotateKey(session, realm, provider);
                                rotatedKeysCount++;
                            }
                        }

                        // Check for expired passive keys (regardless of auto-rotation setting)
                        int expired = expirePassiveKeys(session, realm, provider);
                        expiredKeysCount += expired;

                        // Check for disabled keys that should be deleted
                        int deleted = deleteDisabledKeys(session, realm, provider);
                        deletedKeysCount += deleted;

                    } catch (Exception e) {
                        logger.errorv(e, "Failed to process automatic key rotation for provider '{0}' in realm '{1}'", 
                                provider.getName(), realm.getName());
                    }
                }
            } catch (Exception e) {
                logger.errorv(e, "Failed to process automatic key rotation for realm '{0}'", realm.getName());
            }
        }

        long durationMillis = Time.currentTimeMillis() - startTimeMillis;
        long intervalSeconds = Config.scope("scheduled").getLong("interval", 900L);
        
        // Only log if there are providers with auto-rotation enabled or if any actions were taken
        if (autoRotationEnabledCount > 0 || rotatedKeysCount > 0 || expiredKeysCount > 0 || deletedKeysCount > 0) {
            logger.infof("Automatic key rotation task: %d providers with auto-rotation enabled, rotated=%d, expired=%d, deleted=%d keys in %d ms, next run in %d seconds",
                    autoRotationEnabledCount, rotatedKeysCount, expiredKeysCount, deletedKeysCount, durationMillis, intervalSeconds);
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    /**
     * Updates the gauge metric showing last rotation timestamp for active keys.
     * Also removes gauges for passive/inactive keys.
     */
    private void updateLastRotationMetric(ComponentModel provider, RealmModel realm) {
        try {
            String gaugeKey = realm.getId() + ":" + provider.getId();
            
            String activeStr = provider.get(ACTIVE_KEY);
            boolean isActive = activeStr == null || !"false".equalsIgnoreCase(activeStr);
            
            if (!isActive) {
                // Remove gauge for passive keys
                AtomicLong removed = rotationGaugeValues.remove(gaugeKey);
                if (removed != null) {
                    // Remove from Micrometer registry
                    Metrics.globalRegistry.remove(Metrics.globalRegistry.find(KEY_LAST_ROTATED_METER_NAME)
                        .tag("realm", realm.getName())
                        .tag("provider", provider.getProviderId())
                        .tag("name", provider.getName())
                        .meter());
                }
                return;
            }

            String lastRotationTimeStr = provider.get(LAST_ROTATION_TIME_KEY);
            if (lastRotationTimeStr == null || lastRotationTimeStr.trim().isEmpty()) {
                return; // No rotation time set yet
            }

            long lastRotationTimeMillis = Long.parseLong(lastRotationTimeStr);
            long lastRotationTimeSeconds = lastRotationTimeMillis / 1000;
            
            // Get or create the AtomicLong for this provider's gauge
            AtomicLong gaugeValue = rotationGaugeValues.computeIfAbsent(gaugeKey, k -> {
                AtomicLong value = new AtomicLong(lastRotationTimeSeconds);
                
                // Register the gauge with Micrometer
                List<Tag> tags = new ArrayList<>();
                tags.add(Tag.of("realm", realm.getName()));
                tags.add(Tag.of("provider", provider.getProviderId()));
                tags.add(Tag.of("name", provider.getName()));
                
                Gauge.builder(KEY_LAST_ROTATED_METER_NAME, value, AtomicLong::get)
                    .description("Unix timestamp (seconds) when key was last rotated")
                    .tags(tags)
                    .register(Metrics.globalRegistry);
                
                return value;
            });
            
            // Update the gauge value
            gaugeValue.set(lastRotationTimeSeconds);
            
        } catch (Exception e) {
            logger.infov(e, "Failed to update time until rotation metric for provider '{0}'", provider.getName());
        }
    }

    /**
     * Checks if a key should be rotated based on the last rotation time and rotation period.
     */
    private boolean shouldRotateKey(ComponentModel provider, RealmModel realm) {
        // First check if auto-rotation is enabled for this provider
        String autoRotationEnabledStr = provider.get(AUTO_ROTATION_ENABLED_KEY);
        boolean autoRotationEnabled = autoRotationEnabledStr != null && Boolean.parseBoolean(autoRotationEnabledStr);
        if (!autoRotationEnabled) {
            return false;
        }
        
        // Only rotate active keys (default to active if not explicitly set to false)
        String activeStr = provider.get(ACTIVE_KEY);
        boolean isActive = activeStr == null || !"false".equalsIgnoreCase(activeStr);
        if (!isActive) {
            return false;
        }

        String rotationPeriodStr = provider.get(ROTATION_PERIOD_KEY);
        long rotationPeriodSeconds = rotationPeriodStr != null ? Long.parseLong(rotationPeriodStr) : 7776000L; // Default: 90 days in seconds
        long rotationPeriodMillis = java.util.concurrent.TimeUnit.SECONDS.toMillis(rotationPeriodSeconds);

        String lastRotationTimeStr = provider.get(LAST_ROTATION_TIME_KEY);
        if (lastRotationTimeStr == null || lastRotationTimeStr.trim().isEmpty()) {
            // No rotation has happened yet - initialize the timestamp but don't rotate immediately
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(provider.getConfig());
            config.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
            provider.setConfig(config);
            realm.updateComponent(provider);
            return false;
        }

        long lastRotationTime = Long.parseLong(lastRotationTimeStr);
        long currentTime = Time.currentTimeMillis();
        long timeSinceRotation = currentTime - lastRotationTime;
        boolean shouldRotate = timeSinceRotation >= rotationPeriodMillis;
        
        return shouldRotate;
    }

    /**
     * Rotates the key by creating a new key provider with higher priority and 
     * setting the current active key to passive.
     */
    private void rotateKey(KeycloakSession session, RealmModel realm, ComponentModel currentProvider) {
        logger.infof("ROTATING KEY for provider '%s' (id=%s) in realm '%s'", 
                currentProvider.getName(), currentProvider.getId(), realm.getName());
        
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("realm", realm.getName()));
        tags.add(Tag.of("provider", currentProvider.getProviderId()));
        tags.add(Tag.of("operation", "rotate"));

        // Set the current provider's key to passive
        MultivaluedHashMap<String, String> currentConfig = new MultivaluedHashMap<>(currentProvider.getConfig());
        currentConfig.putSingle(ACTIVE_KEY, "false");
        currentConfig.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
        currentProvider.setConfig(currentConfig);
        realm.updateComponent(currentProvider);

        // Create a new key provider with higher priority
        ComponentModel newProvider = new ComponentModel();
        // Use providerId as base name to avoid accumulating timestamps that exceed DB column length
        String baseName = currentProvider.getProviderId();
        newProvider.setName(baseName + "-" + Time.currentTimeMillis());
        newProvider.setParentId(realm.getId());
        newProvider.setProviderId(currentProvider.getProviderId());
        newProvider.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        
        // Copy essential configuration
        long currentPriority = currentProvider.get("priority", 0L);
        config.putSingle("priority", String.valueOf(currentPriority + 1));
        
        // Copy key use if present
        if (currentProvider.contains(KEY_USE)) {
            config.putSingle(KEY_USE, currentProvider.get(KEY_USE));
        }
        
        // Copy algorithm if present
        if (currentProvider.contains(ALGORITHM_KEY)) {
            config.putSingle(ALGORITHM_KEY, currentProvider.get(ALGORITHM_KEY));
        }
        
        // Copy key size if present
        if (currentProvider.contains(KEY_SIZE_KEY)) {
            config.putSingle(KEY_SIZE_KEY, currentProvider.get(KEY_SIZE_KEY));
        }
        
        newProvider.setConfig(config);

        // Add the component (this triggers validation and key generation)
        ComponentModel added = realm.addComponentModel(newProvider);
        
        // Now update it with rotation-specific config after it's been created and validated
        MultivaluedHashMap<String, String> rotationConfig = new MultivaluedHashMap<>(added.getConfig());
        rotationConfig.putSingle(AUTO_ROTATION_ENABLED_KEY, "true");
            rotationConfig.putSingle(ROTATION_PERIOD_KEY, currentProvider.get(ROTATION_PERIOD_KEY));
            rotationConfig.putSingle(PASSIVE_KEY_EXPIRATION_KEY, currentProvider.get(PASSIVE_KEY_EXPIRATION_KEY));
        rotationConfig.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
        rotationConfig.putSingle(ACTIVE_KEY, "true");
        rotationConfig.putSingle(ENABLED_KEY, "true");
        added.setConfig(rotationConfig);
        realm.updateComponent(added);

        rotationMeterProvider.withTags(tags).increment();
        
        logger.infof("Automatic key rotation activated: Created and configured new key provider '%s' (id=%s) for realm '%s'", 
                added.getName(), added.getId(), realm.getName());
        
        // Update metric for the new key immediately after rotation
        updateLastRotationMetric(added, realm);
        
        // Fire admin event
        fireAdminEvent(session, realm, added, OperationType.CREATE, "Automatic key rotation activated");
    }

    /**
     * Expires passive keys that have exceeded their expiration period.
     * Returns the number of keys that were disabled.
     */
    private int expirePassiveKeys(KeycloakSession session, RealmModel realm, ComponentModel provider) {
        // Only check passive keys (if active is explicitly set to false)
        String activeStr = provider.get(ACTIVE_KEY);
        boolean isActive = activeStr == null || !"false".equalsIgnoreCase(activeStr);
        if (isActive) {
            return 0;
        }

        String expirationPeriodStr = provider.get(PASSIVE_KEY_EXPIRATION_KEY);
        long passiveExpirationSeconds = expirationPeriodStr != null ? Long.parseLong(expirationPeriodStr) : 2592000L; // Default: 30 days in seconds
        long passiveExpirationMillis = java.util.concurrent.TimeUnit.SECONDS.toMillis(passiveExpirationSeconds);

        String lastRotationTimeStr = provider.get(LAST_ROTATION_TIME_KEY);
        if (lastRotationTimeStr == null || lastRotationTimeStr.trim().isEmpty()) {
            return 0;
        }

        long lastRotationTime = Long.parseLong(lastRotationTimeStr);
        long currentTime = Time.currentTimeMillis();
        long timeSinceRotation = currentTime - lastRotationTime;
        
        if ((currentTime - lastRotationTime) >= passiveExpirationMillis) {
            // Disable the key
            String enabledStr = provider.get(ENABLED_KEY);
            boolean wasEnabled = enabledStr == null || !"false".equalsIgnoreCase(enabledStr);
            if (wasEnabled) {
                MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(provider.getConfig());
                config.putSingle(ENABLED_KEY, "false");
                // Track when the key was disabled for deletion grace period
                config.putSingle(DISABLED_TIME_KEY, String.valueOf(currentTime));
                provider.setConfig(config);
                realm.updateComponent(provider);

                List<Tag> tags = new ArrayList<>();
                tags.add(Tag.of("realm", realm.getName()));
                tags.add(Tag.of("provider", provider.getProviderId()));
                tags.add(Tag.of("operation", "expire"));
                rotationMeterProvider.withTags(tags).increment();

                logger.infof("Disabled expired passive key provider '%s' in realm '%s' at %d", 
                        provider.getName(), realm.getName(), currentTime);
                
                // Fire admin event
                fireAdminEvent(session, realm, provider, OperationType.UPDATE, 
                        "Automatic expiration of passive key");
                
                return 1;
            }
        }

        return 0;
    }

    /**
     * Delete disabled keys that have passed their deletion grace period.
     * Only deletes keys if AUTO_DELETE_DISABLED_KEYS_KEY is enabled for the provider.
     * 
     * @param session Keycloak session
     * @param realm The realm containing the provider
     * @param provider The key provider to check
     * @return 1 if the provider was deleted, 0 otherwise
     */
    private int deleteDisabledKeys(KeycloakSession session, RealmModel realm, ComponentModel provider) {
        // Check if auto-deletion is enabled for this provider
        String autoDeleteStr = provider.get(AUTO_DELETE_DISABLED_KEYS_KEY);
        boolean autoDeleteEnabled = "true".equalsIgnoreCase(autoDeleteStr);
        if (!autoDeleteEnabled) {
            return 0;
        }

        // Only delete disabled keys (must be explicitly disabled)
        String enabledStr = provider.get(ENABLED_KEY);
        boolean isDisabled = "false".equalsIgnoreCase(enabledStr);
        if (!isDisabled) {
            return 0;
        }

        // Check if the key has been disabled long enough
        String disabledTimeStr = provider.get(DISABLED_TIME_KEY);
        if (disabledTimeStr == null || disabledTimeStr.trim().isEmpty()) {
            return 0;
        }

        long disabledTime = Long.parseLong(disabledTimeStr);
        long currentTime = Time.currentTimeMillis();
        long timeSinceDisabled = currentTime - disabledTime;

        String gracePeriodStr = provider.get(DELETION_GRACE_PERIOD_KEY);
        long gracePeriodSeconds = gracePeriodStr != null ? Long.parseLong(gracePeriodStr) : 3600L; // Default: 1 hour
        long gracePeriodMillis = java.util.concurrent.TimeUnit.SECONDS.toMillis(gracePeriodSeconds);

        boolean shouldDelete = timeSinceDisabled >= gracePeriodMillis;

        if (shouldDelete) {
            logger.infof("Automatic key deletion: provider='%s', realm='%s', providerId='%s', gracePeriod=%d seconds",
                    provider.getName(), realm.getName(), provider.getProviderId(), gracePeriodSeconds);
            
            // Fire admin event before deletion
            fireAdminEvent(session, realm, provider, OperationType.DELETE, 
                    "Automatic deletion of disabled key after grace period");

            // Remove the component
            realm.removeComponent(provider);

            List<Tag> tags = new ArrayList<>();
            tags.add(Tag.of("realm", realm.getName()));
            tags.add(Tag.of("provider", provider.getProviderId()));
            tags.add(Tag.of("operation", "delete"));
            rotationMeterProvider.withTags(tags).increment();

            return 1;
        }

        return 0;
    }

    /**
     * Fires an admin event for key rotation operations.
     */
    private void fireAdminEvent(KeycloakSession session, RealmModel realm, ComponentModel component, 
                                OperationType operationType, String message) {
        try {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            if (eventStore != null && realm.isAdminEventsEnabled()) {
                AdminEvent adminEvent = new AdminEvent();
                adminEvent.setTime(Time.currentTimeMillis());
                adminEvent.setRealmId(realm.getId());
                adminEvent.setOperationType(operationType);
                adminEvent.setResourceType(ResourceType.COMPONENT);
                adminEvent.setResourcePath("components/" + component.getId());
                
                // No auth details available in scheduled task context - create empty AuthDetails for system-generated event
                AuthDetails authDetails = new AuthDetails();
                authDetails.setRealmId(realm.getId());
                adminEvent.setAuthDetails(authDetails);
                
                // Add details about the key rotation
                java.util.Map<String, String> details = new java.util.HashMap<>();
                details.put("providerId", component.getProviderId());
                details.put("providerName", component.getName());
                details.put("message", message);
                details.put("rotationPeriod", component.get(ROTATION_PERIOD_KEY));
                details.put("passiveExpiration", component.get(PASSIVE_KEY_EXPIRATION_KEY));
                
                // Serialize details as representation
                try {
                    adminEvent.setRepresentation(org.keycloak.util.JsonSerialization.writeValueAsString(details));
                } catch (Exception e) {
                    logger.infov(e, "Failed to serialize event details");
                }
                
                // Always save with full details for system-generated security events
                eventStore.onEvent(adminEvent, true);
            }
        } catch (Exception e) {
            logger.warnv(e, "Failed to fire admin event for key rotation in realm '%s'", realm.getName());
        }
    }
}
