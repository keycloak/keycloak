/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;

public abstract class AbstractCredentialSigner<T> implements CredentialSigner<T> {

    protected final KeycloakSession keycloakSession;

    protected AbstractCredentialSigner(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    /**
     * Reconstruct a signer matching a credential build configuration.
     */
    protected SignatureSignerContext getSigner(CredentialBuildConfig credentialBuildConfig) {
        if (credentialBuildConfig.getSigningAlgorithm() == null) {
            throw new CredentialSignerException(String.format(
                    "A signing algorithm must be configured for credential %s",
                    credentialBuildConfig.getCredentialId()
            ));
        }

        // 1. Will return the active key if `signingKeyId` is null.
        // 2. `signingKeyId` as header can be confusing if there is any key rotation,
        //    as key ids have to be immutable. It can lead to different keys being exposed
        //    under the same id. We give the ability to override with a custom kid if configured.
        KeyWrapper signingKey = getKeyWithKidSubstitute(
                credentialBuildConfig.getSigningKeyId(),
                credentialBuildConfig.getSigningAlgorithm(),
                credentialBuildConfig.getOverrideKeyId()
        );

        SignatureProvider signatureProvider = keycloakSession
                .getProvider(SignatureProvider.class, credentialBuildConfig.getSigningAlgorithm());

        return signatureProvider.signer(signingKey);
    }

    /**
     * Returns the key stored under keyId, or the active key for the given jws algorithm.
     * Additionally, the function clones the key retrieved from Keycloak, replacing the original
     * key ID by the substitute one if provided. This makes it possible to have a custom kid header
     * when producing JSON web signatures.
     */
    protected KeyWrapper getKeyWithKidSubstitute(String keyId, String algorithm, String keyIdSubstitute) {
        KeyWrapper signingKey = getKey(keyId, algorithm);
        if (signingKey == null) {
            throw new CredentialSignerException(
                    String.format("No key for id %s and algorithm %s available.", keyId, algorithm));
        }

        if (keyIdSubstitute != null) {
            // We need to clone the key first, to not change the kid of the original key
            // so that the next request still can find it.
            signingKey = signingKey.cloneKey();
            signingKey.setKid(keyIdSubstitute);
        }

        return signingKey;
    }

    /**
     * Returns the key stored under keyId, or the active key for the given jws algorithm.
     */
    protected KeyWrapper getKey(String keyId, String algorithm) {
        RealmModel realm = keycloakSession.getContext().getRealm();
        KeyManager keys = keycloakSession.keys();

        if (keyId == null) {
            // Allow the signer to work with the active key if keyId is null
            // And we still have to figure out how to proceed with key rotation
            return keys.getActiveKey(realm, KeyUse.SIG, algorithm);
        }

        return keys.getKey(realm, keyId, KeyUse.SIG, algorithm);
    }
}
