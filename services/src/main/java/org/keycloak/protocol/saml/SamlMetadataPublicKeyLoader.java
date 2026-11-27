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

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;

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
    private final String entityId;

    public SamlMetadataPublicKeyLoader(KeycloakSession session, String metadataUrl) {
        this(session, metadataUrl, true);
    }

    public SamlMetadataPublicKeyLoader(KeycloakSession session, String metadataUrl, boolean forIdP) {
        this(session, metadataUrl, forIdP, null);
    }

    /**
     * @param session The keycloak session
     * @param metadataUrl The URL of the metadata endpoint
     * @param forIdP true to load the IDP keys, false to load the SP keys
     * @param entityId The entity ID of the entity to load the keys from. If the metadata does not
     * contain that entity no keys are loaded. If null the entity ID is not checked and the keys are
     * loaded from the first entity in the metadata.
     */
    public SamlMetadataPublicKeyLoader(KeycloakSession session, String metadataUrl, boolean forIdP, String entityId) {
        super(forIdP);
        this.session = session;
        this.metadataUrl = metadataUrl;
        this.entityId = entityId;
    }

    @Override
    protected String getKeys() throws Exception {
        logger.debugf("loading keys from metadata endpoint %s", metadataUrl);
        return session.getProvider(HttpClientProvider.class).getString(metadataUrl);
    }

    @Override
    protected EntityDescriptorType getEntityDescriptor(String descriptor) throws ParsingException {
        if (entityId == null) {
            return super.getEntityDescriptor(descriptor);
        }

        EntityDescriptorType entityType = SAMLMetadataUtil.parseEntityDescriptorType(descriptor, entityId);
        if (entityType == null) {
            logger.warnf("The metadata endpoint %s does not contain the entity ID '%s'. No keys are loaded from it.", metadataUrl, entityId);
        }
        return entityType;
    }
}
