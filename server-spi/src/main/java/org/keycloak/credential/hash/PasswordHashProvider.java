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

package org.keycloak.credential.hash;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 */
public interface PasswordHashProvider extends Provider {
    boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential);

    PasswordCredentialModel encodedCredential(String rawPassword, int iterations);

    default
    String encode(String rawPassword, int iterations) {
        return rawPassword;
    }

    boolean verify(String rawPassword, PasswordCredentialModel credential);

    /**
     * @deprecated Exists due the backwards compatibility. It is recommended to use {@link #policyCheck(PasswordPolicy, PasswordCredentialModel)}
     */
    @Deprecated
    default boolean policyCheck(PasswordPolicy policy, CredentialModel credential) {
        return policyCheck(policy, PasswordCredentialModel.createFromCredentialModel(credential));
    }

    /**
     * @deprecated Exists due the backwards compatibility. It is recommended to use {@link #encodedCredential(String, int)}}
     */
    @Deprecated
    default void encode(String rawPassword, int iterations, CredentialModel credential) {
        PasswordCredentialModel passwordCred = encodedCredential(rawPassword, iterations);

        credential.setCredentialData(passwordCred.getCredentialData());
        credential.setSecretData(passwordCred.getSecretData());
    }

    /**
     * @deprecated Exists due the backwards compatibility. It is recommended to use {@link #verify(String, PasswordCredentialModel)}
     */
    @Deprecated
    default boolean verify(String rawPassword, CredentialModel credential) {
        PasswordCredentialModel password = PasswordCredentialModel.createFromCredentialModel(credential);
        return verify(rawPassword, password);
    }
}
