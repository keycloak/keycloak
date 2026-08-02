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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
import org.keycloak.component.ComponentModel;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.timer.ScheduledTask;

import org.jboss.logging.Logger;

/**
 * Scheduled task for automatic key rotation.
 * This task checks all key providers across all realms and rotates keys that are due for rotation.
 * 
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

    // Root config key for --metrics-enabled (duplicated here to avoid a config-api module dependency).
    private static final String METRICS_ENABLED_CONFIG_KEY = "metrics-enabled";

    // Store gauge handles (backing value + registered meter) per provider so stale meters can be removed
    private static final Map<String, GaugeHandle> rotationGauges = new ConcurrentHashMap<>();

    /** Registered "last rotated" gauge for a provider: the backing value and the meter to remove. */
    private record GaugeHandle(AtomicLong value, Meter meter) {}

    // Attribute keys from org.keycloak.keys.Attributes (duplicated here to avoid module dependency)
    private static final String AUTO_ROTATION_ENABLED_KEY = "autoRotationEnabled";
    private static final String ROTATION_PERIOD_KEY = "rotationPeriod";
    private static final String LAST_ROTATION_TIME_KEY = "lastRotationTime";
    private static final String ACTIVE_KEY = "active";
    private static final String ENABLED_KEY = "enabled";
    private static final String KEY_USE = "keyUse";
    private static final String ALGORITHM_KEY = "algorithm";
    private static final String KEY_SIZE_KEY = "keySize";
    private static final String SECRET_SIZE_KEY = "secretSize";
    private static final String EC_GENERATE_CERTIFICATE_KEY = "ecGenerateCertificate";
    private static final String ECDSA_ELLIPTIC_CURVE_KEY = "ecdsaEllipticCurveKey";
    private static final String ECDH_ELLIPTIC_CURVE_KEY = "ecdhEllipticCurveKey";
    private static final String ECDH_ALGORITHM_KEY = "ecdhAlgorithm";
    private static final String EDDSA_ELLIPTIC_CURVE_KEY = "eddsaEllipticCurveKey";
    private static final String PASSIVE_KEY_EXPIRATION_KEY = "passiveKeyExpiration";
    private static final String AUTO_DELETE_DISABLED_KEYS_KEY = "autoDeleteDisabledKeys";
    private static final String DELETION_GRACE_PERIOD_KEY = "deletionGracePeriod";
    private static final String DISABLED_TIME_KEY = "disabledTime";
    private static final String PRE_ACTIVATION_PERIOD_KEY = "preActivationPeriod";
    private static final String ACTIVATION_TIME_KEY = "activationTime";
    private static final String PRE_ACTIVATION_PREDECESSOR_KEY = "preActivationPredecessorId";
    private static final String PRE_ACTIVATION_PENDING_KEY = "preActivationPending";

    private static final long DEFAULT_ROTATION_PERIOD_SECONDS = 7776000L; // 90 days

    /**
     * Key-generation parameters copied onto the replacement provider so the new key is generated
     * with the same characteristics as the rotated one (RSA key size, AES/HMAC secret size, EC
     * curve, key use, algorithm, certificate generation). Generated key material (private keys,
     * public keys, certificates, secrets, kid) is deliberately excluded so a fresh key is produced.
     */
    private static final List<String> GENERATION_PARAM_KEYS = List.of(
            KEY_USE, ALGORITHM_KEY, KEY_SIZE_KEY, SECRET_SIZE_KEY, EC_GENERATE_CERTIFICATE_KEY,
            ECDSA_ELLIPTIC_CURVE_KEY, ECDH_ELLIPTIC_CURVE_KEY, ECDH_ALGORITHM_KEY, EDDSA_ELLIPTIC_CURVE_KEY);

    @Override
    public void run(KeycloakSession session) {
        long startTimeMillis = Time.currentTimeMillis();
        long now = startTimeMillis;
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();

        RotationCounters counters = new RotationCounters();

        // Collect realms up front so we are not holding a result-set cursor open while each
        // per-provider action opens its own short transaction.
        List<RealmModel> realms = session.realms().getRealmsStream().collect(Collectors.toList());
        for (RealmModel realm : realms) {
            processRealm(factory, realm, now, counters);
        }

        // Remove gauges/meters for providers or realms that no longer exist, to avoid unbounded
        // stale metric cardinality when keys are deleted administratively. Skipped when metrics are
        // disabled, since no gauges were ever registered.
        if (isMetricsEnabled()) {
            reconcileStaleGauges(session);
        }

        long durationMillis = Time.currentTimeMillis() - startTimeMillis;
        long intervalSeconds = Config.scope("scheduled").getLong("interval", 900L);

        // Only log if there are providers with auto-rotation enabled or if any actions were taken
        if (counters.autoRotationEnabled > 0 || counters.rotated > 0 || counters.promoted > 0
                || counters.expired > 0 || counters.deleted > 0) {
            logger.infof("Automatic key rotation task: %d providers with auto-rotation enabled, rotated=%d, promoted=%d, expired=%d, deleted=%d keys in %d ms, next run in %d seconds",
                    counters.autoRotationEnabled, counters.rotated, counters.promoted, counters.expired, counters.deleted, durationMillis, intervalSeconds);
        }
    }

    /**
     * Processes a single realm as a clean pipeline: snapshot the current provider state, publish
     * observability metrics, then decide and act. Each mutation runs in its own short transaction
     * with a re-check, so a failure on one provider neither poisons the others nor silently
     * overwrites concurrent changes from another node.
     */
    private void processRealm(KeycloakSessionFactory factory, RealmModel realm, long now, RotationCounters counters) {
        try {
            // Compute the session/token-derived retention floor once per realm (it iterates the
            // realm's clients) rather than once per provider, then reuse it for every snapshot.
            long minimumPassiveRetentionSeconds = computeMinimumPassiveKeyRetention(realm);

            // Snapshot: parse each provider's configuration exactly once into an immutable state object.
            List<ProviderRotationState> snapshots = realm
                    .getComponentsStream(realm.getId(), KeyProvider.class.getName())
                    .map(component -> ProviderRotationState.from(component, realm, minimumPassiveRetentionSeconds))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Initialize missing rotation timestamps outside the hot loop, each in its own transaction.
            snapshots.stream()
                    .filter(state -> state.autoRotationEnabled() && state.lastRotationTimeMillis() == null)
                    .forEach(state -> {
                        try {
                            initializeMissingRotationTimestamps(factory, state);
                        } catch (Exception e) {
                            logger.errorv(e, "Failed to initialize rotation timestamp for provider '{0}' in realm '{1}'",
                                    state.providerName(), realm.getName());
                        }
                    });

            // Initialize a missing disable timestamp for disabled providers with auto-delete enabled,
            // so the deletion grace period can actually start counting. Without this, a provider that
            // was disabled administratively (which never writes disabledTime) — or one that had
            // auto-delete enabled while already disabled — would be retained forever.
            snapshots.stream()
                    .filter(state -> state.autoDeleteDisabledKeys() && !state.enabled()
                            && state.disabledTimeMillis() == null)
                    .forEach(state -> {
                        try {
                            initializeMissingDisabledTimestamp(factory, state);
                        } catch (Exception e) {
                            logger.errorv(e, "Failed to initialize disabled timestamp for provider '{0}' in realm '{1}'",
                                    state.providerName(), realm.getName());
                        }
                    });

            // Publish observability from the snapshot. Pure with respect to persistent state.
            snapshots.forEach(this::publishLastRotationGauge);

            counters.autoRotationEnabled += (int) snapshots.stream()
                    .filter(ProviderRotationState::autoRotationEnabled)
                    .count();

            // Decide, then act. Each action re-reads and re-checks inside its own transaction.
            // A failure on one provider is logged and isolated so it does not skip the remaining
            // providers in the realm.
            for (ProviderRotationState state : snapshots) {
                try {
                    if (isDueForRotation(state, now) && rotateKeyTx(factory, state, now)) {
                        counters.rotated++;
                    }
                    if (shouldPromotePreActivationKey(state, now) && promotePreActivationKeyTx(factory, state, now)) {
                        counters.promoted++;
                    }
                    if (shouldExpirePassiveKey(state, now) && expirePassiveKeyTx(factory, state, now)) {
                        counters.expired++;
                    }
                    if (shouldDeleteDisabledKey(state, now) && deleteDisabledKeyTx(factory, state, now)) {
                        counters.deleted++;
                    }
                } catch (Exception e) {
                    logger.errorv(e, "Automatic key rotation failed for provider '{0}' in realm '{1}'",
                            state.providerName(), realm.getName());
                }
            }
        } catch (Exception e) {
            logger.errorv(e, "Automatic key rotation failed for realm '{0}'", realm.getName());
        }
    }

    /**
     * Mutable counters aggregated across all realms for the summary log line.
     */
    private static final class RotationCounters {
        private int autoRotationEnabled;
        private int rotated;
        private int promoted;
        private int expired;
        private int deleted;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    /**
     * Publishes the "last rotated" gauge for an active key from an immutable snapshot.
     * Passive or not-yet-rotated keys have their gauge removed. This method is pure with respect
     * to persistent state — it only touches the in-memory metric registry.
     */
    private void publishLastRotationGauge(ProviderRotationState state) {
        // Honor --metrics-enabled=false: do not register meters when metrics are disabled.
        if (!isMetricsEnabled()) {
            return;
        }
        try {
            if (!state.active() || state.lastRotationTimeMillis() == null) {
                removeGauge(gaugeKey(state));
                return;
            }

            long lastRotationTimeSeconds = state.lastRotationTimeMillis() / 1000;

            // Get or create the gauge handle for this provider
            GaugeHandle handle = rotationGauges.computeIfAbsent(gaugeKey(state), k -> {
                AtomicLong value = new AtomicLong(lastRotationTimeSeconds);

                // Register the gauge with Micrometer
                List<Tag> tags = new ArrayList<>();
                tags.add(Tag.of("realm", state.realmName()));
                tags.add(Tag.of("provider", state.providerType()));
                tags.add(Tag.of("name", state.providerName()));

                Gauge gauge = Gauge.builder(KEY_LAST_ROTATED_METER_NAME, value, AtomicLong::get)
                    .description("Unix timestamp (seconds) when key was last rotated")
                    .tags(tags)
                    .register(Metrics.globalRegistry);

                return new GaugeHandle(value, gauge);
            });

            // Update the gauge value
            handle.value().set(lastRotationTimeSeconds);

        } catch (Exception e) {
            logger.infov(e, "Failed to update last rotation metric for provider '{0}'", state.providerName());
        }
    }

    /**
     * Removes the "last rotated" gauge for a provider (by gauge key) from both the local map and
     * the meter registry.
     */
    private void removeGauge(String gaugeKey) {
        GaugeHandle removed = rotationGauges.remove(gaugeKey);
        if (removed != null) {
            Metrics.globalRegistry.remove(removed.meter());
        }
    }

    /**
     * Removes gauges whose backing provider or realm no longer exists. Passive providers are
     * already handled during the scan; this reconciliation catches providers or realms deleted
     * administratively that would otherwise never appear in a future snapshot, leaving their meter
     * registered forever.
     */
    private void reconcileStaleGauges(KeycloakSession session) {
        for (String key : new ArrayList<>(rotationGauges.keySet())) {
            int sep = key.indexOf(':');
            if (sep < 0) {
                removeGauge(key);
                continue;
            }
            String realmId = key.substring(0, sep);
            String componentId = key.substring(sep + 1);
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null || realm.getComponent(componentId) == null) {
                removeGauge(key);
            }
        }
    }

    private static String gaugeKey(ProviderRotationState state) {
        return state.realmId() + ":" + state.componentId();
    }

    /**
     * Whether Keycloak metrics are enabled ({@code --metrics-enabled=true}). When disabled, this
     * task must not create counters or gauges, matching the behavior of other metric producers such
     * as {@code ExpirationTaskFactory} and {@code PasswordCredentialProviderFactory}.
     */
    private static boolean isMetricsEnabled() {
        return Config.scope().root().getBoolean(METRICS_ENABLED_CONFIG_KEY, Boolean.FALSE);
    }

    // -------------------------------------------------------------------------
    // Immutable snapshot + pure decision helpers
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of a key provider's rotation-relevant configuration, parsed exactly once.
     * Decisions and metrics both read from this single source of truth so they cannot drift apart.
     */
    record ProviderRotationState(
            String realmId,
            String realmName,
            String componentId,
            String providerType,
            String providerName,
            boolean autoRotationEnabled,
            boolean active,
            boolean enabled,
            Long lastRotationTimeMillis,
            long rotationPeriodSeconds,
            long configuredPassiveExpirationSeconds,
            long minimumPassiveRetentionSeconds,
            boolean autoDeleteDisabledKeys,
            Long disabledTimeMillis,
            long deletionGracePeriodSeconds,
            long preActivationPeriodSeconds,
            Long activationTimeMillis,
            String preActivationPredecessorId,
            boolean preActivationPending) {

        /**
         * Parses a provider's configuration into an immutable snapshot. A provider whose
         * configuration cannot be parsed is logged and skipped (returns {@code null}) so a single
         * malformed provider does not poison the whole realm scan.
         */
        static ProviderRotationState from(ComponentModel component, RealmModel realm, long minimumPassiveRetentionSeconds) {
            try {
                boolean autoRotationEnabled = "true".equalsIgnoreCase(component.get(AUTO_ROTATION_ENABLED_KEY));

                String activeStr = component.get(ACTIVE_KEY);
                boolean active = activeStr == null || !"false".equalsIgnoreCase(activeStr);

                String enabledStr = component.get(ENABLED_KEY);
                boolean enabled = enabledStr == null || !"false".equalsIgnoreCase(enabledStr);

                Long lastRotationTimeMillis = parseLongOrNull(component.get(LAST_ROTATION_TIME_KEY));
                long rotationPeriodSeconds = parseLongOrDefault(component.get(ROTATION_PERIOD_KEY), DEFAULT_ROTATION_PERIOD_SECONDS);
                if (rotationPeriodSeconds <= 0) {
                    logger.warnf("Invalid rotationPeriod (%d s) for provider '%s' in realm '%s'; " +
                            "falling back to the default of %d s to avoid rotating on every run.",
                            rotationPeriodSeconds, component.getName(), realm.getName(), DEFAULT_ROTATION_PERIOD_SECONDS);
                    rotationPeriodSeconds = DEFAULT_ROTATION_PERIOD_SECONDS;
                }
                long configuredPassiveExpirationSeconds = parseLongOrDefault(component.get(PASSIVE_KEY_EXPIRATION_KEY), 0L);

                boolean autoDeleteDisabledKeys = "true".equalsIgnoreCase(component.get(AUTO_DELETE_DISABLED_KEYS_KEY));
                Long disabledTimeMillis = parseLongOrNull(component.get(DISABLED_TIME_KEY));
                long deletionGracePeriodSeconds = parseLongOrDefault(component.get(DELETION_GRACE_PERIOD_KEY), 3600L); // 1 hour

                long preActivationPeriodSeconds = parseLongOrDefault(component.get(PRE_ACTIVATION_PERIOD_KEY), 0L);
                if (preActivationPeriodSeconds < 0) {
                    preActivationPeriodSeconds = 0L; // A negative window is meaningless; treat as immediate activation.
                }
                Long activationTimeMillis = parseLongOrNull(component.get(ACTIVATION_TIME_KEY));
                String preActivationPredecessorId = component.get(PRE_ACTIVATION_PREDECESSOR_KEY);
                boolean preActivationPending = "true".equalsIgnoreCase(component.get(PRE_ACTIVATION_PENDING_KEY));

                return new ProviderRotationState(
                        realm.getId(), realm.getName(), component.getId(), component.getProviderId(), component.getName(),
                        autoRotationEnabled, active, enabled, lastRotationTimeMillis, rotationPeriodSeconds,
                        configuredPassiveExpirationSeconds, minimumPassiveRetentionSeconds, autoDeleteDisabledKeys,
                        disabledTimeMillis, deletionGracePeriodSeconds, preActivationPeriodSeconds, activationTimeMillis,
                        preActivationPredecessorId, preActivationPending);
            } catch (Exception e) {
                logger.warnv(e, "Skipping key provider '{0}' in realm '{1}' due to malformed rotation configuration",
                        component.getName(), realm.getName());
                return null;
            }
        }

        /**
         * Effective passive-key expiration: never shorter than the session-derived retention floor,
         * so tokens signed with a passive key can still be verified while long-lived sessions exist.
         */
        long effectivePassiveExpirationSeconds() {
            if (configuredPassiveExpirationSeconds > 0) {
                return Math.max(configuredPassiveExpirationSeconds, minimumPassiveRetentionSeconds);
            }
            return minimumPassiveRetentionSeconds;
        }

        private static Long parseLongOrNull(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Long.parseLong(value.trim());
        }

        private static long parseLongOrDefault(String value, long defaultValue) {
            Long parsed = parseLongOrNull(value);
            return parsed != null ? parsed : defaultValue;
        }
    }

    /**
     * Pure predicate: is an active, enabled, auto-rotation-enabled key past its rotation period?
     * A provider that an administrator has disabled (enabled=false) is never rotated, so rotation
     * cannot silently re-enable disabled key material. A key that is itself a not-yet-promoted
     * pre-activation key, or one that already has a pre-activation successor pending promotion, is
     * also skipped so a single rotation does not spawn multiple replacements.
     */
    static boolean isDueForRotation(ProviderRotationState state, long now) {
        if (!state.autoRotationEnabled() || !state.active() || !state.enabled()) {
            return false;
        }
        if (state.activationTimeMillis() != null || state.preActivationPending()) {
            return false;
        }
        if (state.lastRotationTimeMillis() == null) {
            return false;
        }
        long elapsedMillis = now - state.lastRotationTimeMillis();
        return elapsedMillis >= TimeUnit.SECONDS.toMillis(state.rotationPeriodSeconds());
    }

    /**
     * Pure predicate: is a passive pre-activation key past its activation time and ready to become
     * the active signing key? During the pre-activation window the new key is published in the JWKS
     * ({@code enabled=true}) but not yet active, giving clients that cache the JWKS time to pick it
     * up before it signs tokens.
     */
    static boolean shouldPromotePreActivationKey(ProviderRotationState state, long now) {
        if (state.activationTimeMillis() == null) {
            return false;
        }
        if (state.active() || !state.enabled()) {
            return false;
        }
        return now >= state.activationTimeMillis();
    }

    /**
     * Pure predicate: has a passive, still-enabled key exceeded its effective expiration period?
     * A pre-activation key waiting to be promoted (activationTime set) is never expired here.
     */
    static boolean shouldExpirePassiveKey(ProviderRotationState state, long now) {
        if (state.active() || !state.enabled()) {
            return false;
        }
        if (state.activationTimeMillis() != null) {
            return false;
        }
        if (state.lastRotationTimeMillis() == null) {
            return false;
        }
        long elapsedMillis = now - state.lastRotationTimeMillis();
        return elapsedMillis >= TimeUnit.SECONDS.toMillis(state.effectivePassiveExpirationSeconds());
    }

    /**
     * Pure predicate: is a disabled key past its deletion grace period, with auto-delete enabled?
     */
    static boolean shouldDeleteDisabledKey(ProviderRotationState state, long now) {
        if (!state.autoDeleteDisabledKeys() || state.enabled()) {
            return false;
        }
        if (state.disabledTimeMillis() == null) {
            return false;
        }
        if (state.deletionGracePeriodSeconds() < 0) {
            // A negative grace period is invalid and must never trigger deletion of key material.
            return false;
        }
        long elapsedMillis = now - state.disabledTimeMillis();
        return elapsedMillis >= TimeUnit.SECONDS.toMillis(state.deletionGracePeriodSeconds());
    }

    // -------------------------------------------------------------------------
    // Transactional actions — each re-reads and re-checks inside its own transaction
    // -------------------------------------------------------------------------

    /**
     * Initializes a missing {@code lastRotationTime} in its own transaction. The value is only
     * written after re-reading the component inside the transaction and confirming it is still
     * unset, which provides a compare-and-set guarantee against concurrent writers.
     */
    private void initializeMissingRotationTimestamps(KeycloakSessionFactory factory, ProviderRotationState state) {
        KeycloakModelUtils.runJobInTransaction(factory, txSession -> {
            RealmModel realm = txSession.realms().getRealm(state.realmId());
            if (realm == null) {
                return;
            }
            ComponentModel component = realm.getComponent(state.componentId());
            if (component == null) {
                return;
            }
            String existing = component.get(LAST_ROTATION_TIME_KEY);
            if (existing != null && !existing.trim().isEmpty()) {
                return; // Re-check inside the transaction: another writer already set it.
            }
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(component.getConfig());
            config.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
            component.setConfig(config);
            realm.updateComponent(component);
        });
    }

    /**
     * Initializes a missing {@code disabledTime} for a disabled provider that has auto-delete
     * enabled, in its own transaction. Only {@code expirePassiveKey} writes this timestamp during
     * automatic expiration; a provider disabled by an administrator (or one that had auto-delete
     * turned on while already disabled) has no timestamp and would otherwise never start its
     * deletion grace period. The value is written only after re-reading the component inside the
     * transaction and confirming it is still disabled, still auto-delete, and still unset, which
     * provides a compare-and-set guarantee against concurrent writers.
     */
    private void initializeMissingDisabledTimestamp(KeycloakSessionFactory factory, ProviderRotationState state) {
        KeycloakModelUtils.runJobInTransaction(factory, txSession -> {
            RealmModel realm = txSession.realms().getRealm(state.realmId());
            if (realm == null) {
                return;
            }
            ComponentModel component = realm.getComponent(state.componentId());
            if (component == null) {
                return;
            }
            boolean stillEnabled = !"false".equalsIgnoreCase(component.get(ENABLED_KEY));
            boolean autoDelete = "true".equalsIgnoreCase(component.get(AUTO_DELETE_DISABLED_KEYS_KEY));
            String existing = component.get(DISABLED_TIME_KEY);
            if (stillEnabled || !autoDelete || (existing != null && !existing.trim().isEmpty())) {
                return; // Re-check inside the transaction: state changed or another writer set it.
            }
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(component.getConfig());
            config.putSingle(DISABLED_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
            component.setConfig(config);
            realm.updateComponent(component);
        });
    }

    /**
     * Rotates a key in its own transaction, re-checking the predicate against a freshly re-read
     * snapshot so a concurrent rotation on another node is not duplicated.
     *
     * @return {@code true} if the key was rotated, {@code false} if it was skipped on re-check.
     */
    private boolean rotateKeyTx(KeycloakSessionFactory factory, ProviderRotationState state, long now) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, txSession -> {
            RealmModel realm = txSession.realms().getRealm(state.realmId());
            if (realm == null) {
                return false;
            }
            ComponentModel component = realm.getComponent(state.componentId());
            if (component == null) {
                return false;
            }
            ProviderRotationState fresh = ProviderRotationState.from(component, realm, computeMinimumPassiveKeyRetention(realm));
            if (fresh == null || !isDueForRotation(fresh, now)) {
                return false; // Re-check inside the transaction.
            }
            rotateKey(txSession, realm, component);
            return true;
        });
    }

    /**
     * Promotes a pre-activation key in its own transaction, re-checking the predicate against a
     * freshly re-read snapshot so a concurrent promotion on another node is not duplicated.
     *
     * @return {@code true} if the key was promoted, {@code false} if it was skipped on re-check.
     */
    private boolean promotePreActivationKeyTx(KeycloakSessionFactory factory, ProviderRotationState state, long now) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, txSession -> {
            RealmModel realm = txSession.realms().getRealm(state.realmId());
            if (realm == null) {
                return false;
            }
            ComponentModel component = realm.getComponent(state.componentId());
            if (component == null) {
                return false;
            }
            ProviderRotationState fresh = ProviderRotationState.from(component, realm, computeMinimumPassiveKeyRetention(realm));
            if (fresh == null || !shouldPromotePreActivationKey(fresh, now)) {
                return false; // Re-check inside the transaction.
            }
            promotePreActivationKey(txSession, realm, component, fresh);
            return true;
        });
    }

    /**
     * Expires a passive key in its own transaction, re-checking the predicate against a freshly
     * re-read snapshot.
     *
     * @return {@code true} if the key was disabled, {@code false} if it was skipped on re-check.
     */
    private boolean expirePassiveKeyTx(KeycloakSessionFactory factory, ProviderRotationState state, long now) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, txSession -> {
            RealmModel realm = txSession.realms().getRealm(state.realmId());
            if (realm == null) {
                return false;
            }
            ComponentModel component = realm.getComponent(state.componentId());
            if (component == null) {
                return false;
            }
            ProviderRotationState fresh = ProviderRotationState.from(component, realm, computeMinimumPassiveKeyRetention(realm));
            if (fresh == null || !shouldExpirePassiveKey(fresh, now)) {
                return false; // Re-check inside the transaction.
            }
            expirePassiveKey(txSession, realm, component, fresh);
            return true;
        });
    }

    /**
     * Deletes a disabled key in its own transaction, re-checking the predicate against a freshly
     * re-read snapshot.
     *
     * @return {@code true} if the key was deleted, {@code false} if it was skipped on re-check.
     */
    private boolean deleteDisabledKeyTx(KeycloakSessionFactory factory, ProviderRotationState state, long now) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, txSession -> {
            RealmModel realm = txSession.realms().getRealm(state.realmId());
            if (realm == null) {
                return false;
            }
            ComponentModel component = realm.getComponent(state.componentId());
            if (component == null) {
                return false;
            }
            ProviderRotationState fresh = ProviderRotationState.from(component, realm, computeMinimumPassiveKeyRetention(realm));
            if (fresh == null || !shouldDeleteDisabledKey(fresh, now)) {
                return false; // Re-check inside the transaction.
            }
            deleteDisabledKey(txSession, realm, component);
            return true;
        });
    }

    /**
     * Computes the minimum safe passive key retention period based on realm session timeout settings.
     * Keys must remain available (passive) at least as long as the longest-lived session type
     * to ensure tokens signed with that key can still be verified.
     *
     * @param realm The realm to compute the minimum retention for
     * @return Minimum retention period in seconds
     */
    static long computeMinimumPassiveKeyRetention(RealmModel realm) {
        long maxLifespanSeconds = 0;

        // SSO session lifespans (in seconds)
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getSsoSessionMaxLifespan());
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getSsoSessionIdleTimeout());

        // "Remember me" variants (0 means "use regular SSO value", so skip if 0)
        if (realm.getSsoSessionMaxLifespanRememberMe() > 0) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getSsoSessionMaxLifespanRememberMe());
        }
        if (realm.getSsoSessionIdleTimeoutRememberMe() > 0) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getSsoSessionIdleTimeoutRememberMe());
        }

        // Offline session idle timeout — typically the largest (default 30 days)
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getOfflineSessionIdleTimeout());

        // Offline session max lifespan (only applies when enabled)
        if (realm.isOfflineSessionMaxLifespanEnabled()) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getOfflineSessionMaxLifespan());
        }

        // Client-level overrides (0 means "inherit realm value", so skip if 0)
        if (realm.getClientOfflineSessionIdleTimeout() > 0) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getClientOfflineSessionIdleTimeout());
        }
        if (realm.getClientOfflineSessionMaxLifespan() > 0) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getClientOfflineSessionMaxLifespan());
        }
        if (realm.getClientSessionIdleTimeout() > 0) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getClientSessionIdleTimeout());
        }
        if (realm.getClientSessionMaxLifespan() > 0) {
            maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getClientSessionMaxLifespan());
        }

        // Signed tokens that are not bounded by session timeouts (access tokens, action tokens).
        // A verifying key must outlive these too, otherwise a still-valid token could fail
        // verification after its key is disabled.
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getAccessTokenLifespan());
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getAccessTokenLifespanForImplicitFlow());
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getActionTokenGeneratedByAdminLifespan());
        maxLifespanSeconds = Math.max(maxLifespanSeconds, realm.getActionTokenGeneratedByUserLifespan());
        for (Integer actionTokenLifespan : realm.getUserActionTokenLifespans().values()) {
            if (actionTokenLifespan != null) {
                maxLifespanSeconds = Math.max(maxLifespanSeconds, actionTokenLifespan);
            }
        }

        // Per-client overrides: an individual client may configure a token/session lifespan that
        // exceeds every realm-level default above (e.g. a longer access.token.lifespan or
        // client.offline.session.max.lifespan). A verifying key must outlive the longest such value
        // so a token or session issued by any client can still be validated after the key goes
        // passive. Computed once per realm (see processRealm) so this scan is not repeated per key.
        long maxClientLifespanSeconds = realm.getClientsStream()
                .mapToLong(AutomaticKeyRotationTask::maxClientLifespanSeconds)
                .max()
                .orElse(0L);
        maxLifespanSeconds = Math.max(maxLifespanSeconds, maxClientLifespanSeconds);

        // Safety margin: 10% of the max lifespan, at least 1 hour
        long safetyMarginSeconds = Math.max(3600L, maxLifespanSeconds / 10);

        return maxLifespanSeconds + safetyMarginSeconds;
    }

    /**
     * Largest per-client token/session lifespan override configured on a single client, in seconds,
     * or 0 if the client inherits all realm-level values. These attributes, when set, take
     * precedence over the realm defaults (see {@code SessionExpirationUtils}).
     */
    private static long maxClientLifespanSeconds(ClientModel client) {
        long max = 0L;
        max = Math.max(max, clientAttributeSeconds(client, OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN));
        max = Math.max(max, clientAttributeSeconds(client, OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT));
        max = Math.max(max, clientAttributeSeconds(client, OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN));
        max = Math.max(max, clientAttributeSeconds(client, OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT));
        max = Math.max(max, clientAttributeSeconds(client, OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN));
        return max;
    }

    /**
     * Reads a client attribute as a non-negative number of seconds, returning 0 when the attribute
     * is unset, blank, or not a valid number (in which case the realm default applies).
     */
    private static long clientAttributeSeconds(ClientModel client, String attr) {
        String value = client.getAttribute(attr);
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Rotates the key. When a pre-activation period is configured the replacement key is first
     * published passively (visible in the JWKS but not yet used for signing) so clients that cache
     * the JWKS have time to pick it up; a later run promotes it to active. Otherwise the replacement
     * becomes active immediately (the default behavior).
     */
    private void rotateKey(KeycloakSession session, RealmModel realm, ComponentModel currentProvider) {
        long preActivationPeriodSeconds = currentProvider.get(PRE_ACTIVATION_PERIOD_KEY, 0L);
        if (preActivationPeriodSeconds > 0) {
            startPreActivation(session, realm, currentProvider, preActivationPeriodSeconds);
        } else {
            rotateImmediately(session, realm, currentProvider);
        }
    }

    /**
     * Immediate rotation: demote the current key to passive and create a new, higher-priority
     * active key in the same step, so the new key starts signing right away.
     */
    private void rotateImmediately(KeycloakSession session, RealmModel realm, ComponentModel currentProvider) {
        logger.infof("ROTATING KEY for provider '%s' (id=%s) in realm '%s'",
                currentProvider.getName(), currentProvider.getId(), realm.getName());

        // Set the current provider's key to passive
        MultivaluedHashMap<String, String> currentConfig = new MultivaluedHashMap<>(currentProvider.getConfig());
        currentConfig.putSingle(ACTIVE_KEY, "false");
        currentConfig.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
        currentProvider.setConfig(currentConfig);
        realm.updateComponent(currentProvider);

        ComponentModel added = createReplacementProvider(realm, currentProvider);

        MultivaluedHashMap<String, String> rotationConfig = baseRotationConfig(added, currentProvider);
        rotationConfig.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(Time.currentTimeMillis()));
        rotationConfig.putSingle(ACTIVE_KEY, "true");
        added.setConfig(rotationConfig);
        realm.updateComponent(added);

        incrementRotationMeter(realm, currentProvider.getProviderId(), "rotate");

        logger.infof("Automatic key rotation activated: Created and configured new key provider '%s' (id=%s) for realm '%s'",
                added.getName(), added.getId(), realm.getName());

        // Update metric for the new key immediately after rotation. The retention floor is
        // irrelevant to the gauge (it only reads active state and lastRotationTime), so pass 0.
        publishLastRotationGauge(ProviderRotationState.from(added, realm, 0L));

        // Fire admin event
        fireAdminEvent(session, realm, added, OperationType.CREATE, "Automatic key rotation activated");
    }

    /**
     * Pre-activation rotation: create the replacement key as passive-but-published
     * ({@code active=false, enabled=true}) with an {@code activationTime} in the future, while the
     * current key keeps signing. The current key is marked {@code preActivationPending} so it is not
     * rotated again before the pending key is promoted. A later run promotes the new key (see
     * {@link #promotePreActivationKey}).
     */
    private void startPreActivation(KeycloakSession session, RealmModel realm, ComponentModel currentProvider,
                                    long preActivationPeriodSeconds) {
        long now = Time.currentTimeMillis();
        long activationTime = now + TimeUnit.SECONDS.toMillis(preActivationPeriodSeconds);

        logger.infof("PRE-ACTIVATION rotation for provider '%s' (id=%s) in realm '%s': replacement key will be " +
                "published passively for %d s and activated at %d",
                currentProvider.getName(), currentProvider.getId(), realm.getName(), preActivationPeriodSeconds, activationTime);

        ComponentModel added = createReplacementProvider(realm, currentProvider);

        MultivaluedHashMap<String, String> cfg = baseRotationConfig(added, currentProvider);
        cfg.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(now));
        cfg.putSingle(ACTIVE_KEY, "false"); // passive but published in the JWKS
        cfg.putSingle(ACTIVATION_TIME_KEY, String.valueOf(activationTime));
        cfg.putSingle(PRE_ACTIVATION_PREDECESSOR_KEY, currentProvider.getId());
        added.setConfig(cfg);
        realm.updateComponent(added);

        // Mark the still-active predecessor so it is not rotated again while promotion is pending.
        MultivaluedHashMap<String, String> currentConfig = new MultivaluedHashMap<>(currentProvider.getConfig());
        currentConfig.putSingle(PRE_ACTIVATION_PENDING_KEY, "true");
        currentProvider.setConfig(currentConfig);
        realm.updateComponent(currentProvider);

        incrementRotationMeter(realm, currentProvider.getProviderId(), "pre-activate");

        logger.infof("Automatic key rotation pre-activation: published passive key provider '%s' (id=%s) for realm '%s'",
                added.getName(), added.getId(), realm.getName());

        // The new key is passive, so no "last rotated" gauge is published until it is promoted.
        fireAdminEvent(session, realm, added, OperationType.CREATE, "Automatic key rotation: pre-activation key published");
    }

    /**
     * Promotes a pre-activation key to the active signing key once its activation time has passed:
     * the new key becomes active and its pre-activation markers are cleared, and its predecessor
     * (the previously active key) is demoted to passive so its expiration clock starts.
     */
    private void promotePreActivationKey(KeycloakSession session, RealmModel realm, ComponentModel newProvider,
                                         ProviderRotationState state) {
        long now = Time.currentTimeMillis();

        logger.infof("PROMOTING pre-activation key '%s' (id=%s) to active in realm '%s'",
                newProvider.getName(), newProvider.getId(), realm.getName());

        // Demote the predecessor (previously active) key to passive, if it still exists.
        String predecessorId = state.preActivationPredecessorId();
        if (predecessorId != null && !predecessorId.trim().isEmpty()) {
            ComponentModel predecessor = realm.getComponent(predecessorId);
            if (predecessor != null) {
                MultivaluedHashMap<String, String> pc = new MultivaluedHashMap<>(predecessor.getConfig());
                pc.putSingle(ACTIVE_KEY, "false");
                pc.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(now)); // start its passive-expiration clock now
                pc.remove(PRE_ACTIVATION_PENDING_KEY);
                predecessor.setConfig(pc);
                realm.updateComponent(predecessor);
                // The predecessor is now passive: drop its "last rotated" gauge.
                publishLastRotationGauge(ProviderRotationState.from(predecessor, realm, 0L));
            }
        }

        // Promote the new key to active and clear the pre-activation markers.
        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(newProvider.getConfig());
        cfg.putSingle(ACTIVE_KEY, "true");
        cfg.putSingle(LAST_ROTATION_TIME_KEY, String.valueOf(now)); // its rotation clock starts at activation
        cfg.remove(ACTIVATION_TIME_KEY);
        cfg.remove(PRE_ACTIVATION_PREDECESSOR_KEY);
        newProvider.setConfig(cfg);
        realm.updateComponent(newProvider);

        incrementRotationMeter(realm, newProvider.getProviderId(), "promote");

        // The new key is now active: publish its "last rotated" gauge.
        publishLastRotationGauge(ProviderRotationState.from(newProvider, realm, 0L));

        fireAdminEvent(session, realm, newProvider, OperationType.UPDATE,
                "Automatic key rotation: pre-activation key promoted to active");
    }

    /**
     * Creates the replacement key provider (higher priority, same key-generation parameters as the
     * rotated one) and returns the persisted component. Generated key material is intentionally not
     * copied so a fresh key is produced. Rotation-specific and activation state is applied by the
     * caller.
     */
    private ComponentModel createReplacementProvider(RealmModel realm, ComponentModel currentProvider) {
        ComponentModel newProvider = new ComponentModel();
        // Use providerId as base name to avoid accumulating timestamps that exceed DB column length
        String baseName = currentProvider.getProviderId();
        newProvider.setName(baseName + "-" + Time.currentTimeMillis());
        newProvider.setParentId(realm.getId());
        newProvider.setProviderId(currentProvider.getProviderId());
        newProvider.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

        // Higher priority so the freshly generated key can become the active one.
        long currentPriority = currentProvider.get("priority", 0L);
        config.putSingle("priority", String.valueOf(currentPriority + 1));

        // Copy every key-generation parameter (size, secret size, EC curve, key use, algorithm,
        // certificate generation) so the replacement key is generated with the same characteristics.
        // Generated key material is intentionally NOT copied so a fresh key is produced.
        for (String paramKey : GENERATION_PARAM_KEYS) {
            if (currentProvider.contains(paramKey)) {
                config.putSingle(paramKey, currentProvider.get(paramKey));
            }
        }

        newProvider.setConfig(config);

        // Add the component (this triggers validation and key generation)
        return realm.addComponentModel(newProvider);
    }

    /**
     * Builds the rotation-specific configuration shared by both the immediate and pre-activation
     * paths (auto-rotation, rotation period, passive expiration, deletion settings and the
     * pre-activation period), starting from the added component's generated config. The caller adds
     * the active/activation state.
     */
    private MultivaluedHashMap<String, String> baseRotationConfig(ComponentModel added, ComponentModel currentProvider) {
        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>(added.getConfig());
        cfg.putSingle(AUTO_ROTATION_ENABLED_KEY, "true");
        cfg.putSingle(ROTATION_PERIOD_KEY, currentProvider.get(ROTATION_PERIOD_KEY));
        cfg.putSingle(PASSIVE_KEY_EXPIRATION_KEY, currentProvider.get(PASSIVE_KEY_EXPIRATION_KEY));
        cfg.putSingle(ENABLED_KEY, "true");
        // Propagate deletion settings to the new provider
        if (currentProvider.get(AUTO_DELETE_DISABLED_KEYS_KEY) != null) {
            cfg.putSingle(AUTO_DELETE_DISABLED_KEYS_KEY, currentProvider.get(AUTO_DELETE_DISABLED_KEYS_KEY));
        }
        if (currentProvider.get(DELETION_GRACE_PERIOD_KEY) != null) {
            cfg.putSingle(DELETION_GRACE_PERIOD_KEY, currentProvider.get(DELETION_GRACE_PERIOD_KEY));
        }
        // Propagate the pre-activation period so the replacement key rotates the same way next time.
        if (currentProvider.get(PRE_ACTIVATION_PERIOD_KEY) != null) {
            cfg.putSingle(PRE_ACTIVATION_PERIOD_KEY, currentProvider.get(PRE_ACTIVATION_PERIOD_KEY));
        }
        return cfg;
    }

    /**
     * Increments the key-rotation operations counter for the given operation, honoring
     * {@code --metrics-enabled}.
     */
    private void incrementRotationMeter(RealmModel realm, String providerId, String operation) {
        if (!isMetricsEnabled()) {
            return;
        }
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("realm", realm.getName()));
        tags.add(Tag.of("provider", providerId));
        tags.add(Tag.of("operation", operation));
        rotationMeterProvider.withTags(tags).increment();
    }

    /**
     * Disables a passive key that has exceeded its effective expiration period. The decision has
     * already been made by {@link #shouldExpirePassiveKey} against a freshly re-read snapshot; this
     * method only performs the mutation, emits the metric, and fires the admin event.
     */
    private void expirePassiveKey(KeycloakSession session, RealmModel realm, ComponentModel provider, ProviderRotationState state) {
        if (state.configuredPassiveExpirationSeconds() > 0
                && state.configuredPassiveExpirationSeconds() < state.minimumPassiveRetentionSeconds()) {
            logger.warnf("Configured passive key expiration (%d s) for provider '%s' in realm '%s' " +
                    "is shorter than the minimum derived from session timeouts (%d s). " +
                    "Using session-derived minimum to prevent token verification failures.",
                    state.configuredPassiveExpirationSeconds(), provider.getName(), realm.getName(),
                    state.minimumPassiveRetentionSeconds());
        }

        long currentTime = Time.currentTimeMillis();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>(provider.getConfig());
        config.putSingle(ENABLED_KEY, "false");
        // Track when the key was disabled for the deletion grace period
        config.putSingle(DISABLED_TIME_KEY, String.valueOf(currentTime));
        provider.setConfig(config);
        realm.updateComponent(provider);

        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("realm", realm.getName()));
        tags.add(Tag.of("provider", provider.getProviderId()));
        tags.add(Tag.of("operation", "expire"));
        if (isMetricsEnabled()) {
            rotationMeterProvider.withTags(tags).increment();
        }

        logger.infof("Disabled expired passive key provider '%s' in realm '%s' at %d",
                provider.getName(), realm.getName(), currentTime);

        // Fire admin event
        fireAdminEvent(session, realm, provider, OperationType.UPDATE, "Automatic expiration of passive key");
    }

    /**
     * Removes a disabled key whose deletion grace period has elapsed. The decision has already been
     * made by {@link #shouldDeleteDisabledKey} against a freshly re-read snapshot; this method only
     * fires the admin event, removes the component, and emits the metric.
     */
    private void deleteDisabledKey(KeycloakSession session, RealmModel realm, ComponentModel provider) {
        logger.infof("Automatic key deletion: provider='%s', realm='%s', providerId='%s'",
                provider.getName(), realm.getName(), provider.getProviderId());

        // Fire admin event before deletion
        fireAdminEvent(session, realm, provider, OperationType.DELETE,
                "Automatic deletion of disabled key after grace period");

        // Remove the component
        realm.removeComponent(provider);

        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("realm", realm.getName()));
        tags.add(Tag.of("provider", provider.getProviderId()));
        tags.add(Tag.of("operation", "delete"));
        if (isMetricsEnabled()) {
            rotationMeterProvider.withTags(tags).increment();
        }
    }

    /**
     * Fires an admin event for key rotation operations. The event is persisted to the configured
     * event store (when admin events are enabled) and dispatched to the realm's configured and
     * global {@link EventListenerProvider}s, so audit/SIEM listeners observe automatic rotation
     * events. This mirrors the dispatch performed by the normal admin-event path and honors
     * {@code isAdminEventsDetailsEnabled}.
     */
    private void fireAdminEvent(KeycloakSession session, RealmModel realm, ComponentModel component,
                                OperationType operationType, String message) {
        try {
            boolean includeRepresentation = realm.isAdminEventsDetailsEnabled();

            AdminEvent adminEvent = new AdminEvent();
            adminEvent.setId(java.util.UUID.randomUUID().toString());
            adminEvent.setTime(Time.currentTimeMillis());
            adminEvent.setRealmId(realm.getId());
            adminEvent.setRealmName(realm.getName());
            adminEvent.setOperationType(operationType);
            adminEvent.setResourceType(ResourceType.COMPONENT);
            adminEvent.setResourcePath("components/" + component.getId());

            // No auth details available in scheduled task context - system-generated event.
            AuthDetails authDetails = new AuthDetails();
            authDetails.setRealmId(realm.getId());
            adminEvent.setAuthDetails(authDetails);

            if (includeRepresentation) {
                Map<String, String> details = new java.util.HashMap<>();
                details.put("providerId", component.getProviderId());
                details.put("providerName", component.getName());
                details.put("message", message);
                details.put("rotationPeriod", component.get(ROTATION_PERIOD_KEY));
                details.put("passiveExpiration", component.get(PASSIVE_KEY_EXPIRATION_KEY));
                try {
                    adminEvent.setRepresentation(org.keycloak.util.JsonSerialization.writeValueAsString(details));
                } catch (Exception e) {
                    logger.infov(e, "Failed to serialize event details");
                }
            }

            // Persist to the event store when admin events are enabled.
            if (realm.isAdminEventsEnabled()) {
                EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
                if (eventStore != null) {
                    eventStore.onEvent(adminEvent, includeRepresentation);
                }
            }

            // Dispatch to the realm's configured and global event listeners.
            Set<String> realmListeners = realm.getEventsListenersStream().collect(Collectors.toSet());
            session.getKeycloakSessionFactory().getProviderFactoriesStream(EventListenerProvider.class)
                    .filter(pf -> realmListeners.contains(pf.getId())
                            || ((EventListenerProviderFactory) pf).isGlobal())
                    .forEach(pf -> {
                        try {
                            EventListenerProvider listener = ((EventListenerProviderFactory) pf).create(session);
                            listener.onEvent(adminEvent, includeRepresentation);
                        } catch (Throwable t) {
                            logger.warnv(t, "Failed to dispatch key rotation admin event to listener '{0}'", pf.getId());
                        }
                    });
        } catch (Exception e) {
            logger.warnv(e, "Failed to fire admin event for key rotation in realm '{0}'", realm.getName());
        }
    }
}
