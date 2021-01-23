/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.vault;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * A {@link VaultProvider} implementation that uses the Elytron keystore-based credential store implementation to retrieve secrets.
 * Elytron credential stores can be created and managed using either the elytron subsystem in WildFly/EAP or the elytron tool.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronCSKeyStoreProvider extends AbstractVaultProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final CredentialStore credentialStore;

    public ElytronCSKeyStoreProvider(final CredentialStore store, final String realmName, final List<VaultKeyResolver> resolvers) {
        super(realmName, resolvers);
        this.credentialStore = store;
    }

    @Override
    protected VaultRawSecret obtainSecretInternal(String vaultSecretId) {
        try {
            PasswordCredential credential = this.credentialStore.retrieve(vaultSecretId, PasswordCredential.class);
            if (credential == null) {
                // alias not found, password type doesn't match entry, or algorithm (clear) doesn't match entry.
                logger.debugf("Cannot find secret %s in credential store", vaultSecretId);
                return DefaultVaultRawSecret.forBuffer(Optional.empty());
            }
            char[] secret = credential.getPassword().castAndApply(ClearPassword.class, ClearPassword::getPassword);
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(secret));
            return DefaultVaultRawSecret.forBuffer(Optional.of(buffer));
        } catch (CredentialStoreException e) {
            // this might happen if there is an error when trying to retrieve the secret from the store.
            logger.debugf(e, "Unable to retrieve secret %s from credential store", vaultSecretId);
            return DefaultVaultRawSecret.forBuffer(Optional.empty());
        }
    }

    @Override
    public void close() {
    }
}
