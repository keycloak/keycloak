/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.oidc;

import java.util.Date;

import org.keycloak.broker.oidc.ClientCertificates.ClientCertificateKey;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Creates the {@link SimpleHttp} for requests that authenticate the client to an identity provider, which presents the
 * client certificate of a realm key provider when the client is authenticated using {@code tls_client_auth}.
 */
public final class MtlsAwareHttp {

    private static final Logger logger = Logger.getLogger(MtlsAwareHttp.class);

    private MtlsAwareHttp() {
    }

    public static SimpleHttp create(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        if (!config.isTlsClientAuthentication()) {
            return SimpleHttp.create(session);
        }
        String keyProviderId = config.getClientCertificateKeyProviderId();
        ClientCertificateKey certificate;
        try {
            certificate = ClientCertificates.findEnabled(session, session.getContext().getRealm(), keyProviderId);
        } catch (IllegalArgumentException e) {
            logger.errorf("Identity provider '%s': %s", config.getAlias(), e.getMessage());
            throw new IdentityBrokerException("Invalid client certificate configuration of the identity provider");
        }
        if (logger.isDebugEnabled() && certificate.getCertificate().getNotAfter().before(new Date())) {
            logger.debugf("The certificate of key provider '%s' of identity provider '%s' has expired",
                    keyProviderId, config.getAlias());
        }
        try {
            return SimpleHttp.create(session, certificate);
        } catch (RuntimeException e) {
            logger.errorf(e, "Unable to use the client certificate of identity provider '%s': %s", config.getAlias(), e.getMessage());
            throw new IdentityBrokerException("Invalid client certificate configuration of the identity provider", e);
        }
    }
}
