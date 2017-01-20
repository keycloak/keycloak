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
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.KeyUtils;
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
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.util.JWKSUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(ClientPublicKeyLoader.class);

    private final KeycloakSession session;
    private final ClientModel client;

    public ClientPublicKeyLoader(KeycloakSession session, ClientModel client) {
        this.session = session;
        this.client = client;
    }


    @Override
    public Map<String, PublicKey> loadKeys() throws Exception {
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (config.isUseJwksUrl()) {
            String jwksUrl = config.getJwksUrl();
            jwksUrl = ResolveRelative.resolveRelativeUri(session.getContext().getUri().getRequestUri(), client.getRootUrl(), jwksUrl);
            JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, jwksUrl);
            return JWKSUtils.getKeysForUse(jwks, JWK.Use.SIG);
        } else {
            try {
                CertificateRepresentation certInfo = CertificateInfoHelper.getCertificateFromClient(client, JWTClientAuthenticator.ATTR_PREFIX);
                PublicKey publicKey = getSignatureValidationKey(certInfo);

                // Check if we have kid in DB, generate otherwise
                String kid = certInfo.getKid() != null ? certInfo.getKid() : KeyUtils.createKeyId(publicKey);
                return Collections.singletonMap(kid, publicKey);
            } catch (ModelException me) {
                logger.warnf(me, "Unable to retrieve publicKey for verify signature of client '%s' . Error details: %s", client.getClientId(), me.getMessage());
                return Collections.emptyMap();
            }

        }
    }

    private static PublicKey getSignatureValidationKey(CertificateRepresentation certInfo) throws ModelException {
        String encodedCertificate = certInfo.getCertificate();
        String encodedPublicKey = certInfo.getPublicKey();

        if (encodedCertificate == null && encodedPublicKey == null) {
            throw new ModelException("Client doesn't have certificate or publicKey configured");
        }

        if (encodedCertificate != null && encodedPublicKey != null) {
            throw new ModelException("Client has both publicKey and certificate configured");
        }

        if (encodedCertificate != null) {
            X509Certificate clientCert = KeycloakModelUtils.getCertificate(encodedCertificate);
            return clientCert.getPublicKey();
        } else {
            return KeycloakModelUtils.getPublicKey(encodedPublicKey);
        }
    }


}
