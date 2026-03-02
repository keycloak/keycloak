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

package org.keycloak.policy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Whitelist (by email/domain) for {@code forceExpiredPasswordChange} required action.
 *
 * Config format: comma/whitespace/newline separated items. Supported:
 * - Full email, e.g. {@code user@example.com}
 * - Domain match, e.g. {@code @example.com} or {@code *@example.com}
 */
public class ForceExpiredPasswordChangeWhitelistPolicyProviderFactory implements PasswordPolicyProviderFactory, PasswordPolicyProvider {

    public static final String ID = "forceExpiredPasswordChangeWhitelist";

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        return null;
    }

    @Override
    public PolicyError validate(String user, String password) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Expire Password - Whitelist";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public Object parseConfig(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.<String>emptySet();
        }

        // Split by comma/semicolon and all whitespace.
        String[] tokens = value.split("[,;\\s]+");
        Set<String> out = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }

            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }

            // Support "*@example.com" as synonym for "@example.com"
            if (normalized.startsWith("*@")) {
                normalized = normalized.substring(1);
            }

            out.add(normalized);
        }

        return Collections.unmodifiableSet(out);
    }
}

