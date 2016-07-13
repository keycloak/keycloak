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

package org.keycloak.hash;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.policy.HashAlgorithmPasswordPolicyProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordHashManager {

    private static final Logger log = Logger.getLogger(PasswordHashManager.class);

    public static UserCredentialValueModel encode(KeycloakSession session, RealmModel realm, String rawPassword) {
        return encode(session, realm.getPasswordPolicy(), rawPassword);
    }

    public static UserCredentialValueModel encode(KeycloakSession session, PasswordPolicy passwordPolicy, String rawPassword) {
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        if (provider == null) {
            log.warnv("Could not find hash provider {0} from password policy, using default provider {1}", passwordPolicy.getHashAlgorithm(), HashAlgorithmPasswordPolicyProviderFactory.DEFAULT_VALUE);
            provider = session.getProvider(PasswordHashProvider.class, HashAlgorithmPasswordPolicyProviderFactory.DEFAULT_VALUE);
        }
        return provider.encode(rawPassword, passwordPolicy.getHashIterations());
    }

    public static boolean verify(KeycloakSession session, RealmModel realm, String password, UserCredentialValueModel credential) {
        return verify(session, realm.getPasswordPolicy(), password, credential);
    }

    public static boolean verify(KeycloakSession session, PasswordPolicy passwordPolicy, String password, UserCredentialValueModel credential) {
        String algorithm = credential.getAlgorithm() != null ? credential.getAlgorithm() : passwordPolicy.getHashAlgorithm();
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, algorithm);
        if (provider == null) {
            log.warnv("Could not find hash provider {0} for password", algorithm);
            return false;
        }
        return provider.verify(password, credential);
    }

}
