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

package org.keycloak.models;

import org.keycloak.policy.PasswordPolicyConfigException;
import org.keycloak.policy.PasswordPolicyProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicy implements Serializable {

    public static final String HASH_ALGORITHM_ID = "hashAlgorithm";

    public static final String HASH_ALGORITHM_DEFAULT = "pbkdf2-sha256";

    public static final String HASH_ITERATIONS_ID = "hashIterations";

    public static final int HASH_ITERATIONS_DEFAULT = 27500;

    public static final String PASSWORD_HISTORY_ID = "passwordHistory";

    public static final String FORCE_EXPIRED_ID = "forceExpiredPasswordChange";

    private Map<String, Object> policyConfig;
    private Builder builder;

    public static PasswordPolicy empty() {
        return new PasswordPolicy(null, new HashMap<>());
    }

    public static Builder build() {
        return new Builder();
    }

    public static PasswordPolicy parse(KeycloakSession session, String policyString) {
        return new Builder(policyString).build(session);
    }

    private PasswordPolicy(Builder builder, Map<String, Object> policyConfig) {
        this.builder = builder;
        this.policyConfig = policyConfig;
    }

    public Set<String> getPolicies() {
        return policyConfig.keySet();
    }

    public <T> T getPolicyConfig(String key) {
        return (T) policyConfig.get(key);
    }

    public String getHashAlgorithm() {
        if (policyConfig.containsKey(HASH_ALGORITHM_ID)) {
            return getPolicyConfig(HASH_ALGORITHM_ID);
        } else {
            return HASH_ALGORITHM_DEFAULT;
        }
    }

    public int getHashIterations() {
        if (policyConfig.containsKey(HASH_ITERATIONS_ID)) {
            return getPolicyConfig(HASH_ITERATIONS_ID);
        } else {
            return -1;
        }
    }

    public int getExpiredPasswords() {
        if (policyConfig.containsKey(PASSWORD_HISTORY_ID)) {
            return getPolicyConfig(PASSWORD_HISTORY_ID);
        } else {
            return -1;
        }
    }

    public int getDaysToExpirePassword() {
        if (policyConfig.containsKey(FORCE_EXPIRED_ID)) {
            return getPolicyConfig(FORCE_EXPIRED_ID);
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return builder.asString();
    }

    public Builder toBuilder() {
        return builder.clone();
    }

    public static class Builder {

        private LinkedHashMap<String, String> map;

        private Builder() {
            this.map = new LinkedHashMap<>();
        }

        private Builder(LinkedHashMap<String, String> map) {
            this.map = map;
        }

        private Builder(String policyString) {
            map = new LinkedHashMap<>();

            if (policyString != null && !policyString.trim().isEmpty()) {
                for (String policy : policyString.split(" and ")) {
                    policy = policy.trim();

                    String key;
                    String config = null;

                    int i = policy.indexOf('(');
                    if (i == -1) {
                        key = policy.trim();
                    } else {
                        key = policy.substring(0, i).trim();
                        config = policy.substring(i + 1, policy.length() - 1);
                    }

                    map.put(key, config);
                }
            }
        }

        public boolean contains(String key) {
            return map.containsKey(key);
        }

        public String get(String key) {
            return map.get(key);
        }

        public Builder put(String key, String value) {
            map.put(key, value);
            return this;
        }

        public Builder remove(String key) {
            map.remove(key);
            return this;
        }

        public PasswordPolicy build(KeycloakSession session) {
            Map<String, Object> config = new HashMap<>();
            for (Map.Entry<String, String> e : map.entrySet()) {

                PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, e.getKey());
                if (provider == null) {
                    throw new PasswordPolicyConfigException("Password policy not found");
                }

                Object o;
                try {
                    o = provider.parseConfig(e.getValue());
                } catch (PasswordPolicyConfigException ex) {
                    throw new ModelException("Invalid config for " + e.getKey() + ": " + ex.getMessage());
                }

                config.put(e.getKey(), o);
            }
            return new PasswordPolicy(this, config);
        }

        public String asString() {
            if (map.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" and ");
                }

                sb.append(e.getKey());

                String c = e.getValue();
                if (c != null && !c.trim().isEmpty()) {
                    sb.append("(");
                    sb.append(c);
                    sb.append(")");
                }
            }
            return sb.toString();
        }

        public Builder clone() {
            return new Builder((LinkedHashMap<String, String>) map.clone());
        }

    }

}
