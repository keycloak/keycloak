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

package org.keycloak.authentication.jpa;

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
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import org.jboss.logging.Logger;

public class JpaAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory<AuthenticationSessionProvider>, EnvironmentDependentProviderFactory, ServerInfoAwareProviderFactory {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String PROVIDER_ID = "jpa";

    private int authSessionsLimit;
    private int expirationTaskIntervalSeconds;
    private int expirationTaskTimeoutSeconds;
    private int expirationTaskMaxRemoval;
    private boolean metricsEnabled;

    // Config
    private static final String AUTH_SESSION_LIMIT_KEY = "authSessionsLimit";
    private static final int DEFAULT_AUTH_SESSION_LIMIT = 300;
    private static final String METRICS_KEY = "metricsEnabled";

    @Override
    public JpaAuthenticationSessionProvider create(KeycloakSession session) {
        return new JpaAuthenticationSessionProvider(session, authSessionsLimit);
    }

    @Override
    public void init(Config.Scope config) {
        authSessionsLimit = config.getInt(AUTH_SESSION_LIMIT_KEY, DEFAULT_AUTH_SESSION_LIMIT);
        metricsEnabled = config.getBoolean(METRICS_KEY, config.root().getBoolean(MetricsOptions.METRICS_ENABLED.getKey(), Boolean.FALSE));
        expirationTaskIntervalSeconds = ExpirationHelper.getExpirationTaskInterval(config, logger);
        expirationTaskTimeoutSeconds = ExpirationHelper.getExpirationTaskTimeout(config, logger);
        expirationTaskMaxRemoval = ExpirationHelper.getExpirationTaskMaxRemoval(config, logger);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        ExpirationTask.builder()
                .withFactory(factory)
                .withEntityId("authentication-sessions")
                .withInterval(expirationTaskIntervalSeconds, TimeUnit.SECONDS)
                .withTimeout(expirationTaskTimeoutSeconds, TimeUnit.SECONDS)
                .withAction(AuthenticationSessionExpirationAction.INSTANCE)
                .withExecutor(ExpirationHelper.expirationExecutor(factory))
                .withMaxRemoval(expirationTaskMaxRemoval)
                .withMetrics(metricsEnabled)
                .withRealmExpiration(true)
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
    public List<ProviderConfigProperty> getConfigMetadata() {
        var builder = ProviderConfigurationBuilder.create();
        builder.property()
                .name(AUTH_SESSION_LIMIT_KEY)
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .helpText("The maximum number of concurrent authentication sessions per RootAuthenticationSession.")
                .defaultValue(DEFAULT_AUTH_SESSION_LIMIT)
                .add();
        builder.property()
                .name(METRICS_KEY)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Whether metrics are enabled for this provider (expiration metrics). If not set, uses '" + MetricsOptions.METRICS_ENABLED.getKey() + "' option value.")
                .add();
        ExpirationHelper.addConfiguration(builder, "authentication sessions");
        return builder.build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        var map = new HashMap<String, String>();
        ExpirationHelper.addToOperationalInfo(expirationTaskIntervalSeconds, expirationTaskTimeoutSeconds, expirationTaskMaxRemoval, map);
        map.put(AUTH_SESSION_LIMIT_KEY, Integer.toString(authSessionsLimit));
        map.put(METRICS_KEY, Boolean.toString(metricsEnabled));
        return Map.copyOf(map);
    }
}
