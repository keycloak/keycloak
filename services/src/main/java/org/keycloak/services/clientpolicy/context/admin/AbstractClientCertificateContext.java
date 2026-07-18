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
package org.keycloak.services.clientpolicy.context.admin;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * Internal scaffolding for AdminAuth wiring shared by all client-certificate context classes.
 *
 * @see ClientCertificateContext
 * @see ClientCertificateUpdateContext
 */
abstract class AbstractClientCertificateContext implements ClientCertificateContext {

    protected final ClientModel targetClient;
    protected final String attributePrefix;
    protected final CertificateRepresentation proposed;
    protected final AdminAuth adminAuth;

    AbstractClientCertificateContext(ClientModel targetClient,
                                     String attributePrefix,
                                     CertificateRepresentation proposed,
                                     AdminAuth adminAuth) {
        this.targetClient = targetClient;
        this.attributePrefix = attributePrefix;
        this.proposed = sanitizeCertificate(proposed);
        this.adminAuth = adminAuth;
    }

    @Override
    public ClientModel getTargetClient() {
        return targetClient;
    }

    @Override
    public String getAttributePrefix() {
        return attributePrefix;
    }

    @Override
    public CertificateRepresentation getProposedCertificate() {
        return sanitizeCertificate(proposed);
    }

    @Override
    public ClientModel getAuthenticatedClient() {
        return adminAuth.getClient();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return adminAuth.getUser();
    }

    @Override
    public JsonWebToken getToken() {
        return adminAuth.getToken();
    }

    private static CertificateRepresentation sanitizeCertificate(CertificateRepresentation certificate) {
        if (certificate == null) {
            return null;
        }

        CertificateRepresentation sanitized = new CertificateRepresentation();
        sanitized.setCertificate(certificate.getCertificate());
        sanitized.setKid(certificate.getKid());
        sanitized.setPublicKey(certificate.getPublicKey());
        sanitized.setPrivateKey(null);
        return sanitized;
    }
}
