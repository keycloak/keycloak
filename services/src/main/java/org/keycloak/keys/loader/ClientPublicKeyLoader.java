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

import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(ClientPublicKeyLoader.class);

    private final KeycloakSession session;
    private final ClientModel client;
    private final JWK.Use keyUse;

    public ClientPublicKeyLoader(KeycloakSession session, ClientModel client) {
        this.session = session;
        this.client = client;
        this.keyUse = JWK.Use.SIG;
    }

    public ClientPublicKeyLoader(KeycloakSession session, ClientModel client, JWK.Use keyUse) {
        this.session = session;
        this.client = client;
        this.keyUse = keyUse;
    }

    @Override
    public Map<String, KeyWrapper> loadKeys() throws Exception {
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (config.isUseJwksUrl()) {
            String jwksUrl = config.getJwksUrl();
            jwksUrl = ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), jwksUrl);
            JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, jwksUrl);
            return JWKSUtils.getKeyWrappersForUse(jwks, keyUse);
        } else if (config.isUseJwksString()) {
            JSONWebKeySet jwks = JsonSerialization.readValue(config.getJwksString(), JSONWebKeySet.class);
            return JWKSUtils.getKeyWrappersForUse(jwks, keyUse);
        } else if (keyUse == JWK.Use.SIG) {
            try {
                CertificateRepresentation certInfo = CertificateInfoHelper.getCertificateFromClient(client, JWTClientAuthenticator.ATTR_PREFIX);
                KeyWrapper publicKey = getSignatureValidationKey(certInfo);
                return Collections.singletonMap(publicKey.getKid(), publicKey);
            } catch (ModelException me) {
                logger.warnf(me, "Unable to retrieve publicKey for verify signature of client '%s' . Error details: %s", client.getClientId(), me.getMessage());
                return Collections.emptyMap();
            }
        } else {
            logger.warnf("Unable to retrieve publicKey of client '%s' for the specified purpose other than verifying signature", client.getClientId());
            return Collections.emptyMap();
        }
    }

    private static KeyWrapper getSignatureValidationKey(CertificateRepresentation certInfo) throws ModelException {
        KeyWrapper keyWrapper = new KeyWrapper();
        String encodedCertificate = certInfo.getCertificate();
        String encodedPublicKey = certInfo.getPublicKey();

        if (encodedCertificate == null && encodedPublicKey == null) {
            throw new ModelException("Client doesn't have certificate or publicKey configured");
        }

        if (encodedCertificate != null && encodedPublicKey != null) {
            throw new ModelException("Client has both publicKey and certificate configured");
        }

        keyWrapper.setUse(KeyUse.SIG);
        String kid = null;
        if (encodedCertificate != null) {
            X509Certificate clientCert = KeycloakModelUtils.getCertificate(encodedCertificate);
            // Check if we have kid in DB, generate otherwise
            kid = certInfo.getKid() != null ? certInfo.getKid() : KeyUtils.createKeyId(clientCert.getPublicKey());
            keyWrapper.setKid(kid);
            keyWrapper.setPublicKey(clientCert.getPublicKey());
            keyWrapper.setType(clientCert.getPublicKey().getAlgorithm());
            keyWrapper.setCertificate(clientCert);
        } else {
            PublicKey publicKey = KeycloakModelUtils.getPublicKey(encodedPublicKey);
            // Check if we have kid in DB, generate otherwise
            kid = certInfo.getKid() != null ? certInfo.getKid() : KeyUtils.createKeyId(publicKey);
            keyWrapper.setKid(kid);
            keyWrapper.setPublicKey(publicKey);
            keyWrapper.setType(publicKey.getAlgorithm());
        }
        return keyWrapper;
    }


}
