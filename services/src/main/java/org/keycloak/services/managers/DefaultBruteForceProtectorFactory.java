/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.managers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultBruteForceProtectorFactory implements BruteForceProtectorFactory {
    DefaultBruteForceProtector protector;
    private final ConcurrentMap<String, DefaultLockingBruteForceProtector.UserLock> userLocks = new ConcurrentHashMap<>();

    private boolean allowConcurrentRequests;

    @Override
    public BruteForceProtector create(KeycloakSession session) {
        return protector != null ? protector : new DefaultLockingBruteForceProtector(session.getKeycloakSessionFactory(), session, userLocks);
    }

    @Override
    public void init(Config.Scope config) {
        // this can be a brute force setting?
        this.allowConcurrentRequests = config.getBoolean("allowConcurrentRequests", Boolean.FALSE);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (Profile.isFeatureEnabled(Profile.Feature.LOGIN_FAILURES_V1)) {
            protector = allowConcurrentRequests ? new DefaultBruteForceProtector(factory) : new DefaultBlockingBruteForceProtector(factory);
        }
    }

    @Override
    public void close() {
        Optional.ofNullable(protector).ifPresent(DefaultBruteForceProtector::shutdown);
    }

    @Override
    public String getId() {
        return "default-brute-force-detector";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("allowConcurrentRequests")
                .type("boolean")
                .helpText("If concurrent logins are allowed by the brute force protection. This is deprecated and only active for login-failures:v1. login-failure:v2 will execute concurrent logins serially on each Keycloak instance instead of returning an error.")
                .defaultValue(false)
                .add()
                .build();
    }
}
