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

import org.keycloak.policy.PasswordPolicyProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicy implements Serializable {

    public static final String HASH_ALGORITHM_ID = "hashAlgorithm";

    public static final String HASH_ALGORITHM_DEFAULT = "pbkdf2";

    public static final String HASH_ITERATIONS_ID = "hashIterations";

    public static final int HASH_ITERATIONS_DEFAULT = 20000;

    public static final String PASSWORD_HISTORY_ID = "passwordHistory";

    public static final String FORCE_EXPIRED_ID = "forceExpiredPasswordChange";

    private String policyString;
    private Map<String, Object> policyConfig;

    public static PasswordPolicy empty() {
        return new PasswordPolicy(null, new HashMap<>());
    }

    public static PasswordPolicy parse(KeycloakSession session, String policyString) {
        Map<String, Object> policyConfig = new HashMap<>();

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

                PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, key);
                if (provider == null) {
                    throw new IllegalArgumentException("Unsupported policy");
                }

                policyConfig.put(key, provider.parseConfig(config));
            }
        }

        return new PasswordPolicy(policyString, policyConfig);
    }

    private PasswordPolicy(String policyString, Map<String, Object> policyConfig) {
        this.policyString = policyString;
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
            return HASH_ITERATIONS_DEFAULT;
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
        return policyString;
    }

}
