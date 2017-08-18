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

package org.keycloak.keys.loader;

import java.security.PublicKey;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.*;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PublicKeyStorageManager {

    private static final Logger logger = Logger.getLogger(PublicKeyStorageManager.class);

    public static PublicKey getClientPublicKey(KeycloakSession session, ClientModel client, JWSInput input) {
        String kid = input.getHeader().getKeyId();

        PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);

        String modelKey = PublicKeyStorageUtils.getClientModelCacheKey(client.getRealm().getId(), client.getId());
        ClientPublicKeyLoader loader = new ClientPublicKeyLoader(session, client);
        return keyStorage.getPublicKey(modelKey, kid, loader);
    }


    public static PublicKey getIdentityProviderPublicKey(KeycloakSession session, RealmModel realm, OIDCIdentityProviderConfig idpConfig, JWSInput input) {
        boolean keyIdSetInConfiguration = idpConfig.getPublicKeySignatureVerifierKeyId() != null
          && ! idpConfig.getPublicKeySignatureVerifierKeyId().trim().isEmpty();

        String kid = input.getHeader().getKeyId();

        PublicKeyStorageProvider keyStorage = session.getProvider(PublicKeyStorageProvider.class);

        String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(realm.getId(), idpConfig.getInternalId());
        PublicKeyLoader loader;
        if (idpConfig.isUseJwksUrl()) {
            loader = new OIDCIdentityProviderPublicKeyLoader(session, idpConfig);
        } else {
            String pem = idpConfig.getPublicKeySignatureVerifier();

            if (pem == null || pem.trim().isEmpty()) {
                logger.warnf("No public key saved on identityProvider %s", idpConfig.getAlias());
                return null;
            }

            loader = new HardcodedPublicKeyLoader(
              keyIdSetInConfiguration
                ? idpConfig.getPublicKeySignatureVerifierKeyId().trim()
                : kid, pem);
        }

        return keyStorage.getPublicKey(modelKey, kid, loader);
    }
}
