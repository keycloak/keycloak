/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml;

import org.keycloak.common.VerificationException;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * <p>PublicKeyLoader to retrieve keys from a SAML metadata entity endpoint.
 * It can be used to load IDP or SP keys.</p>
 *
 * @author rmartinc
 */
public class SamlMetadataPublicKeyLoader extends SamlAbstractMetadataPublicKeyLoader {

    private static final Logger logger = Logger.getLogger(SamlMetadataPublicKeyLoader.class);
    private final KeycloakSession session;
    private final String metadataUrl;

    public SamlMetadataPublicKeyLoader(KeycloakSession session, String metadataUrl) {
        this(session, metadataUrl, true);
    }

    public SamlMetadataPublicKeyLoader(KeycloakSession session, String metadataUrl, boolean forIdP) {
        super(forIdP);
        this.session = session;
        this.metadataUrl = metadataUrl;
    }

    /**
     * Creates a loader that only loads the keys if the metadata is signed with one of the certificates.
     *
     * @param session The keycloak session
     * @param metadataUrl The metadata endpoint
     * @param forIdP true to load the keys of an IDP, false to load the keys of a SP
     * @param signingCertificates The certificates that are trusted to sign the metadata
     */
    public SamlMetadataPublicKeyLoader(KeycloakSession session, String metadataUrl, boolean forIdP, String[] signingCertificates) {
        super(forIdP, signingCertificates);
        this.session = session;
        this.metadataUrl = metadataUrl;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        try {
            return super.loadKeys();
        } catch (VerificationException e) {
            logger.warnf("Rejected the SAML metadata loaded from %s: %s", metadataUrl, e.getMessage());
            throw e;
        }
    }

    @Override
    protected String getKeys() throws Exception {
        logger.debugf("loading keys from metadata endpoint %s", metadataUrl);
        return session.getProvider(HttpClientProvider.class).getString(metadataUrl);
    }
}
