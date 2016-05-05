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

import org.keycloak.hash.PasswordHashManager;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordPolicy implements Serializable {

    private List<PasswordPolicyProvider> policies;
    private Map<String, String> policyArgs;
    private String policyString;

    public PasswordPolicy(String policyString, KeycloakSession session) {
        this.policyString = policyString;
        this.policies = new LinkedList<>();
        this.policyArgs = new HashMap<>();

        if (policyString != null && !policyString.trim().isEmpty()) {
            for (String policy : policyString.split(" and ")) {
                policy = policy.trim();

                String name;
                String arg = null;

                int i = policy.indexOf('(');
                if (i == -1) {
                    name = policy.trim();
                } else {
                    name = policy.substring(0, i).trim();
                    arg = policy.substring(i + 1, policy.length() - 1);
                }

                PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, name);
                if (provider == null) {
                  throw new IllegalArgumentException("Unsupported policy [" + name + "]");
                }

                policies.add(provider);
                policyArgs.put(name, arg == null ? provider.defaultValue() : arg);
            }
        }
    }

    public PasswordPolicy(int iterations) {
       this(null, null);
       policyArgs.put(HashIterations.NAME, String.valueOf(iterations));
    }

    public String getHashAlgorithm() {
        return stringArg(HashAlgorithm.NAME, Constants.DEFAULT_HASH_ALGORITHM);
    }

    /**
     *
     * @return -1 if no hash iterations setting
     */
    public int getHashIterations() {
        return intArg(HashIterations.NAME, -1);
    }

    /**
     *
     * @return -1 if no expired passwords setting
     */
    public int getExpiredPasswords() {
        return intArg(PasswordHistory.NAME, -1);
    }

    /**
     *
     * @return -1 if no force expired password change setting
     */
    public int getDaysToExpirePassword() {
        return intArg(ForceExpiredPasswordChange.NAME, -1);
    }

    public Error validate(KeycloakSession session, UserModel user, String password) {
        for (PasswordPolicyProvider p : policies) {
            Error error = p.validate(session, user, password, this);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    public Error validate(KeycloakSession session, String user, String password) {
        for (PasswordPolicyProvider p : policies) {
            Error error = p.validate(session, user, password, this);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    public int intArg(String policy, int defaultValue) {
        String arg = policyArgs.get(policy);
        if (arg == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(arg);
        }
    }

    public String stringArg(String policy, String defaultValue) {
        String arg = policyArgs.get(policy);
        if (arg == null) {
            return defaultValue;
        } else {
            return arg;
        }
    }

    @Override
    public String toString() {
        return policyString;
    }
}
