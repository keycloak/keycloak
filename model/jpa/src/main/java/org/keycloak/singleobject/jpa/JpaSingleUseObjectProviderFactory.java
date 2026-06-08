/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.singleobject.jpa;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.config.MetricsOptions;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.expiration.jpa.ExpirationHelper;
import org.keycloak.expiration.jpa.ExpirationTask;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SingleUseObjectProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import org.jboss.logging.Logger;

/**
 * Factory for {@link JpaSingleUseObjectProvider}.
 * <p>
 * Enabled only when the {@link Profile.Feature#CACHELESS} feature is active. Registers a periodic
 * {@link ExpirationTask} to clean up expired single-use objects from the database.
 */
public class JpaSingleUseObjectProviderFactory implements SingleUseObjectProviderFactory<JpaSingleUseObjectProvider>, EnvironmentDependentProviderFactory, ServerInfoAwareProviderFactory {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROVIDER_ID = "jpa";

    // Config
    private static final String METRICS_KEY = "metricsEnabled";

    private int expirationTaskIntervalSeconds;
    private int expirationTaskTimeoutSeconds;
    private int expirationTaskMaxRemoval;
    private boolean metricsEnabled;

    @Override
    public JpaSingleUseObjectProvider create(KeycloakSession session) {
        return new JpaSingleUseObjectProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        metricsEnabled = config.getBoolean(METRICS_KEY, config.root().getBoolean(MetricsOptions.METRICS_ENABLED.getKey(), Boolean.FALSE));
        expirationTaskIntervalSeconds = ExpirationHelper.getExpirationTaskInterval(config, logger);
        expirationTaskTimeoutSeconds = ExpirationHelper.getExpirationTaskTimeout(config, logger);
        expirationTaskMaxRemoval = ExpirationHelper.getExpirationTaskMaxRemoval(config, logger);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        ExpirationTask.builder()
                .withEntityId("single-use-obj")
                .withAction(SingleUseObjectExpirationAction.INSTANCE)
                .withFactory(factory)
                .withExecutor(ExpirationHelper.expirationExecutor(factory))
                .withMaxRemoval(expirationTaskMaxRemoval)
                .withMetrics(metricsEnabled)
                .withRealmExpiration(false)
                .withTimeout(expirationTaskTimeoutSeconds, TimeUnit.SECONDS)
                .withInterval(expirationTaskIntervalSeconds, TimeUnit.SECONDS)
                .build()
                .schedule();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        var builder = ProviderConfigurationBuilder.create();
        ExpirationHelper.addConfiguration(builder, "single use object");
        builder.property()
                .name(METRICS_KEY)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Whether metrics are enabled for this provider (expiration metrics). If not set, uses '" + MetricsOptions.METRICS_ENABLED.getKey() + "' option value.")
                .add();
        return builder.build();
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        var deps = new HashSet<>(ExpirationHelper.dependsOn());
        deps.add(JpaConnectionProvider.class);
        return Set.copyOf(deps);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CACHELESS);
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        var map = new HashMap<String, String>();
        ExpirationHelper.addToOperationalInfo(expirationTaskIntervalSeconds, expirationTaskTimeoutSeconds, expirationTaskMaxRemoval, map);
        map.put(METRICS_KEY, Boolean.toString(metricsEnabled));
        return Map.copyOf(map);
    }
}
